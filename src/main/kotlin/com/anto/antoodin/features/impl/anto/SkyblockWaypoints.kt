// Adapted from Odin's DungeonWaypoints (Copyright (c) 2025, odtheking, BSD 3-Clause)
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.DungeonWaypoints.DungeonWaypoint
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.TextPromptScreen
import com.odtheking.odin.features.impl.render.Etherwarp
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.getBlockBounds
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.drawBoxes
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.toFixed
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

object SkyblockWaypoints : Module(
    name = "Skyblock Waypoints",
    description = "Custom waypoints for every island and dungeon boss room. Manage packs with /ao wp.",
    category = Skit.ANTO
) {
    var allowEdits by BooleanSetting("Allow Edits", false, desc = "Allows you to edit waypoints.")
    private val allowTextEdit by BooleanSetting("Allow Text Edit", false, desc = "Allows you to set the text of a waypoint while sneaking.").withDependency { allowEdits }

    private val titleScale by NumberSetting("Title Scale", 1f, 0.1f, 4f, increment = 0.1f, desc = "The scale of the titles of waypoints.")
    private val disableDepth by BooleanSetting("Global Depth", false, desc = "Disables depth testing for all waypoints.")

    private val editorHud by HUD("Editor HUD", "Shows information about the waypoint you're placing or looking at.", false) {
        drawEditorHud(it)
    }

    private val settingsDropDown by DropdownSetting("Next Waypoint Settings")
    var color by ColorSetting("Color", Colors.MINECRAFT_GREEN, true, desc = "The color of the next waypoint you place.").withDependency { settingsDropDown }
    var filled by BooleanSetting("Filled", false, desc = "If the next waypoint you place should be 'filled'.").withDependency { settingsDropDown }
    var depthCheck by BooleanSetting("Depth check", false, desc = "Whether the next waypoint you place should have a depth check.").withDependency { settingsDropDown }
    var useBlockSize by BooleanSetting("Use block size", true, desc = "Use the size of the block you click for waypoint size.").withDependency { settingsDropDown }
    var sizeX by NumberSetting("Size X", 1.0, .1, 5.0, 0.01, desc = "The X size of the next waypoint you place.").withDependency { !useBlockSize && settingsDropDown }
    var sizeY by NumberSetting("Size Y", 1.0, .1, 5.0, 0.01, desc = "The Y size of the next waypoint you place.").withDependency { !useBlockSize && settingsDropDown }
    var sizeZ by NumberSetting("Size Z", 1.0, .1, 5.0, 0.01, desc = "The Z size of the next waypoint you place.").withDependency { !useBlockSize && settingsDropDown }

    private val editModeSettings by DropdownSetting("Edit Mode Settings")
    private val preset1 by ColorSetting("Preset 1", Colors.MINECRAFT_GREEN, true, "Color preset for the cycle keybind.").withDependency { editModeSettings }
    private val preset2 by ColorSetting("Preset 2", Colors.MINECRAFT_RED, true, "Color preset for the cycle keybind.").withDependency { editModeSettings }
    private val preset3 by ColorSetting("Preset 3", Colors.MINECRAFT_BLUE, true, "Color preset for the cycle keybind.").withDependency { editModeSettings }
    private val preset4 by ColorSetting("Preset 4", Colors.MINECRAFT_GOLD, true, "Color preset for the cycle keybind.").withDependency { editModeSettings }
    private var presetIndex = 0
    private val cycleColor by KeybindSetting("Cycle Color", InputConstants.UNKNOWN, "Keybind to cycle the next waypoint's color through the presets.").withDependency { editModeSettings }
        .onPress {
            if (!allowEdits) return@onPress
            presetIndex = (presetIndex + 1) % 4
            color = listOf(preset1, preset2, preset3, preset4)[presetIndex].copy()
            modMessage("§aWaypoint color changed to preset ${presetIndex + 1}.")
        }

    var selectedPackIds by ListSetting("Selected Waypoint Packs", mutableListOf<String>()).hide()
    var editPackId by StringSetting("Edit Waypoint Pack", "", length = 256, desc = "").hide()

    private val resetButton by ActionSetting("Reset Current Island", desc = "Removes the edit pack's waypoints on the current island.") {
        val key = currentKey() ?: return@ActionSetting modMessage("§cNot on a supported island!")
        if (SkyblockWaypointPacks.packs[editPackId]?.remove(key).isNullOrEmpty()) return@ActionSetting modMessage("§cCurrent island does not have any editable waypoints!")
        SkyblockWaypointPacks.save()
        modMessage("§aSuccessfully reset current island!")
    }

    init {
        on<LevelEvent.Load> {
            SkyblockWaypointPacks.reload()
        }

        on<RenderEvent.Extract> {
            val key = currentKey() ?: return@on
            val waypoints = SkyblockWaypointPacks.visible(key)
            drawBoxes(waypoints, disableDepth)
            waypoints.forEach { waypoint ->
                val title = waypoint.title ?: return@forEach
                drawText(title, waypoint.blockPos.center.add(0.0, 0.1 * titleScale, 0.0), titleScale, waypoint.depth && !disableDepth)
            }

            if (allowEdits) reachPosition?.let { pos ->
                drawStyledBox(aabbAt(pos).move(pos), color.withAlpha(0.3f), style = if (filled) 0 else 1, depthCheck)
            }
        }

        on<InputEvent> {
            if (!allowEdits || key.value != InputConstants.MOUSE_BUTTON_RIGHT || mc.screen != null) return@on
            val island = currentKey() ?: return@on
            val pos = reachPosition ?: return@on
            val editable = SkyblockWaypointPacks.editable(island)
            if (editable.none { it.blockPos == pos } && SkyblockWaypointPacks.visible(island).any { it.blockPos == pos })
                return@on modMessage("§eThat waypoint belongs to another active pack. Switch edit packs to change it.")

            if (allowTextEdit && mc.player?.isCrouching == true) {
                mc.setScreen(TextPromptScreen("Waypoint Name").setCallback { text ->
                    SkyblockWaypointPacks.editable(island).apply {
                        removeIf { it.blockPos == pos }
                        add(newWaypoint(pos, text.ifBlank { null }))
                    }
                    SkyblockWaypointPacks.save()
                    mc.setScreen(null)
                })
                return@on
            }

            if (!editable.removeIf { it.blockPos == pos }) editable.add(newWaypoint(pos))
            SkyblockWaypointPacks.save()
        }
    }

    /** Waypoint key for where the player is: the boss room in a dungeon boss, else the island. Null in dungeon clear (Odin's Dungeon Waypoints covers it). */
    fun currentKey(): String? = when {
        DungeonUtils.inBoss -> DungeonUtils.floor?.let { bossKey(it.floorNumber) }
        LocationUtils.currentArea == Island.Dungeon || LocationUtils.currentArea == Island.Unknown -> null
        else -> LocationUtils.currentArea.name
    }

    // Master mode bosses are the same rooms as their normal floors
    fun bossKey(floor: Int) = "F${floor}Boss"

    private val reachPosition: BlockPos?
        get() {
            val hitResult = mc.hitResult
            return when {
                hitResult?.type == HitResult.Type.MISS -> Etherwarp.getEtherPos(mc.player?.position(), 5.0, returnEnd = true).pos
                hitResult is BlockHitResult -> hitResult.blockPos
                else -> null
            }
        }

    private fun newWaypoint(pos: BlockPos, title: String? = null) =
        DungeonWaypoint(pos, color.copy(), filled, depthCheck, aabbAt(pos), title)

    private fun aabbAt(pos: BlockPos): AABB =
        if (!useBlockSize) AABB(BlockPos.ZERO).inflate((sizeX - 1.0) / 2.0, (sizeY - 1.0) / 2.0, (sizeZ - 1.0) / 2.0)
        else pos.getBlockBounds() ?: AABB(BlockPos.ZERO)

    override fun onKeybind() {
        allowEdits = !allowEdits
        modMessage("Skyblock Waypoint editing ${if (allowEdits) "§aenabled" else "§cdisabled"}§r!")
    }

    private fun GuiGraphicsExtractor.drawEditorHud(example: Boolean): Pair<Int, Int> {
        if (example) return drawEditorHud("§fEditing Waypoints §8|§f Placing",
            "§r#${Colors.MINECRAFT_RED.hex()}§7, §3Outline§7, §cThrough Walls§7, §2Block Size", Colors.MINECRAFT_RED)
        if (!allowEdits) return 0 to 0
        val key = currentKey() ?: return 0 to 0
        val pos = reachPosition ?: return 0 to 0

        val hovered = SkyblockWaypointPacks.visible(key).firstOrNull { it.blockPos == pos }
        return drawEditorHud(
            "§fEditing Waypoints §8|§f ${if (hovered == null) "Placing" else "Viewing"}",
            hovered?.describe() ?: describeNext(),
            hovered?.color ?: color,
        )
    }

    private fun GuiGraphicsExtractor.drawEditorHud(title: String, text: String, color: Color): Pair<Int, Int> {
        val textWidth = textDim(text, 0, 10, color).first
        centeredText(mc.font, title, textWidth / 2, 0, Colors.WHITE.rgba)
        return textWidth to 19
    }

    private fun describeNext(): String = buildString {
        append("§r#${color.hex()}§7")
        append(", ${if (filled) "§2Filled" else "§3Outline"}")
        append("§7, ${if (depthCheck) "§2Depth Check" else "§cThrough Walls"}")
        append("§7, ${if (useBlockSize) "§2Block Size" else "§3Size: ${sizeX.toFixed(2)}x${sizeY.toFixed(2)}x${sizeZ.toFixed(2)}"}")
    }

    private fun DungeonWaypoint.describe(): String = buildString {
        append("§r#${color.hex()}§7")
        title?.takeIf(String::isNotBlank)?.let { append(", §fTitle: §a$it§7") }
        append(", ${if (filled) "§2Filled" else "§3Outline"}")
        append("§7, ${if (depth) "§2Depth Check" else "§cThrough Walls"}")
        append("§7, §3Size: ${(aabb.maxX - aabb.minX).toFixed(2)}x${(aabb.maxY - aabb.minY).toFixed(2)}x${(aabb.maxZ - aabb.minZ).toFixed(2)}")
    }
}
