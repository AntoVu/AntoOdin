// Adapted from Odin's DungeonWaypoints (Copyright (c) 2025, odtheking, BSD 3-Clause)
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.TextPromptScreen
import com.odtheking.odin.features.impl.render.Etherwarp
import com.odtheking.odin.features.impl.render.waypoints.Waypoint
import com.odtheking.odin.features.impl.render.waypoints.WaypointAreas
import com.odtheking.odin.features.impl.render.waypoints.Waypoints
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.BoxStyle
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.textDim
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

/** Right-click placement for Odin's own Waypoints module. Waypoints live in Odin's list and config, keyed by [WaypointAreas]. */
object WaypointPlacer : Module(
    name = "Waypoint Placer",
    description = "Place Odin waypoints by right clicking blocks. Manage them with /ao wp.",
    category = Skit.ANTO
) {
    private var allowEdits by BooleanSetting("Allow Edits", false, desc = "Right click a block to add or remove a waypoint.")
    private val allowTextEdit by BooleanSetting("Allow Text Edit", false, desc = "Sneak and right click to set a waypoint's label.").withDependency { allowEdits }

    private val editorHud by HUD("Editor HUD", "Shows information about the waypoint you're placing or looking at.", false) {
        drawEditorHud(it)
    }

    var color by ColorSetting("Color", Colors.MINECRAFT_GREEN, true, desc = "The color of the next waypoint you place.")

    private val editModeSettings by DropdownSetting("Edit Mode Settings", desc = "Color presets for edit mode.")
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

    init {
        on<RenderExtractEvent> {
            if (!allowEdits || WaypointAreas.current() == null) return@on
            reachPosition?.let { drawStyledBox(AABB(it), color.withAlpha(0.3f), BoxStyle.OUTLINE, true) }
        }

        on<InputEvent> {
            if (!allowEdits || key.value != InputConstants.MOUSE_BUTTON_RIGHT || mc.gui.screen() != null) return@on
            val area = WaypointAreas.current()?.key ?: return@on
            val pos = reachPosition ?: return@on
            val hovered = hoveredAt(area, pos)

            if (allowTextEdit && mc.player?.isCrouching == true) {
                mc.gui.setScreen(TextPromptScreen("Waypoint Name").setCallback { text ->
                    val wp = hovered?.run { Waypoint(area, blockPos, endPos, text.trim(), color, trigger, command, radius, enabled) }
                        ?: Waypoint(area, pos, label = text.trim(), color = color.copy())
                    submit(wp, hovered)
                    mc.gui.setScreen(null)
                })
                return@on
            }

            if (hovered != null) {
                Waypoints.remove(hovered)
                saved()
            } else submit(Waypoint(area, pos, color = color.copy()), null)
        }
    }

    private fun submit(wp: Waypoint, replacing: Waypoint?) {
        Waypoints.submit(wp, replacing)?.let { return modMessage("§c$it") }
        saved()
    }

    private fun saved() {
        ModuleManager.saveConfigurations()
        if (!Waypoints.enabled) modMessage("§eEnable Odin's Waypoints module to see waypoints.")
    }

    private fun hoveredAt(area: String, pos: BlockPos) = Waypoints.waypoints.firstOrNull { it.area == area && it.blockPos == pos }

    private val reachPosition: BlockPos?
        get() {
            val hitResult = mc.hitResult
            return when {
                hitResult?.type == HitResult.Type.MISS -> Etherwarp.getEtherPos(mc.player?.position(), 5.0, returnEnd = true).pos
                hitResult is BlockHitResult -> hitResult.blockPos
                else -> null
            }
        }

    override fun onKeybind() {
        allowEdits = !allowEdits
        modMessage("Waypoint editing ${if (allowEdits) "§aenabled" else "§cdisabled"}§r!")
    }

    private fun GuiGraphicsExtractor.drawEditorHud(example: Boolean): Pair<Int, Int> {
        if (example) return drawEditorHud("§fEditing Waypoints §8|§f Placing", "§r#${Colors.MINECRAFT_RED.hex()}", Colors.MINECRAFT_RED)
        if (!allowEdits) return 0 to 0
        val area = WaypointAreas.current()?.key ?: return 0 to 0
        val pos = reachPosition ?: return 0 to 0

        val hovered = hoveredAt(area, pos)
        return drawEditorHud(
            "§fEditing Waypoints §8|§f ${if (hovered == null) "Placing" else "Viewing"}",
            hovered?.describe() ?: "§r#${color.hex()}",
            hovered?.color ?: color,
        )
    }

    private fun GuiGraphicsExtractor.drawEditorHud(title: String, text: String, color: Color): Pair<Int, Int> {
        val textWidth = textDim(text, 0, 10, color).first
        centeredText(mc.font, title, textWidth / 2, 0, Colors.WHITE.rgba)
        return textWidth to 19
    }

    private fun Waypoint.describe(): String = buildString {
        append("§r#${color.hex()}§7")
        label.takeIf(String::isNotBlank)?.let { append(", §fLabel: §a$it§7") }
        append(", §3${trigger.name.lowercase().replaceFirstChar(Char::uppercase)}")
    }
}
