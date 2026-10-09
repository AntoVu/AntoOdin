// Taken from OdinFabric (Copyright (c) 2023-2025, odtheking, BSD 3-Clause)
// Implementation based on skies-starred OdinClient
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.dungeon.map.DungeonScan
import com.odtheking.odin.features.impl.dungeon.map.tile.DoorType
import com.odtheking.odin.features.impl.dungeon.map.tile.DungeonDoor
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.alert
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.render.BoxStyle
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawTracer
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object DoorHighlight : Module(
    name = "Door Highlight",
    description = "Highlights wither and blood doors and keys in dungeons.",
    category = Skit.ANTO
) {
    private val announceKeySpawn by BooleanSetting("Announce Key Spawn", true, desc = "Announces when a key is spawned.")
    private val doorHighlightColor by ColorSetting("Door Highlight Color", Colors.MINECRAFT_RED.withAlpha(0.8f), true, desc = "Color for locked doors.")
    private val openableColor by ColorSetting("Openable Door Color", Colors.MINECRAFT_GREEN.withAlpha(0.8f), true, desc = "Color for doors that can be opened with a held key.")
    private val witherColor by ColorSetting("Wither Color", Colors.BLACK.withAlpha(0.8f), true, desc = "The color of the box.")
    private val bloodColor by ColorSetting("Blood Color", Colors.MINECRAFT_RED.withAlpha(0.8f), true, desc = "The color of the box.")
    private val depthCheck by BooleanSetting("Depth check", true, desc = "Disable depth check to enable ESP.")
    private val lineToDoor by BooleanSetting("Line to Door", false, desc = "Draws a line to the nearest closed wither door, or the blood door once no wither doors are left.")
    private val lineColor by ColorSetting("Line Color", Colors.WHITE, true, desc = "The color of the line.").withDependency { lineToDoor }
    private val lineThickness by NumberSetting("Line Thickness", 3f, 1.0..10.0, 0.5f, desc = "The thickness of the line.").withDependency { lineToDoor }

    private var currentKey: KeyType? = null
    private var witherKeys = 0
    private var bloodKey = false
    private var bloodOpened = false

    private val witherKeyObtainRegex = Regex("^(\\[[^]]*?])? ?(\\w{1,16}) has obtained Wither Key!?$")
    private val witherKeyPickedUpRegex = Regex("^A Wither Key was picked up!$")
    private val witherDoorOpenRegex = Regex("^(\\[[^]]*?])? ?(\\w{1,16}) opened a WITHER door!$")
    private val bloodKeyObtainRegex = Regex("^(\\[[^]]*?])? ?(\\w{1,16}) has obtained Blood Key!$")
    private val bloodKeyPickedUpRegex = Regex("^A Blood Key was picked up!$")
    private val bloodDoorOpenRegex = Regex("^The BLOOD DOOR has been opened!$")

    init {
        on<MessageEvent.Chat> {
            if (!DungeonUtils.inClear) return@on
            when {
                witherKeyObtainRegex.matches(message) || witherKeyPickedUpRegex.matches(message) -> witherKeys++
                witherDoorOpenRegex.matches(message) -> witherKeys = (witherKeys - 1).coerceAtLeast(0)
                bloodKeyObtainRegex.matches(message) || bloodKeyPickedUpRegex.matches(message) -> bloodKey = true
                bloodDoorOpenRegex.matches(message) -> { bloodKey = false; bloodOpened = true }
            }
        }

        on<EntityEvent.SetData> {
            if (!DungeonUtils.inClear) return@on
            val entity = entity as? ArmorStand ?: return@on
            if (currentKey?.entity == entity) return@on
            currentKey = KeyType.entries.find { it.displayName == entity.name.string } ?: return@on
            currentKey?.entity = entity

            if (announceKeySpawn) alert("§${currentKey?.colorCode}${entity.name.string}§7 spawned!")
        }

        on<RenderExtractEvent> {
            if (!DungeonUtils.inClear) return@on
            val playerPos = mc.player?.position()
            var target: DungeonDoor? = null
            var targetDist = Double.MAX_VALUE

            DungeonScan.doors.forEach { (_, door) ->
                if (!door.type.equalsOneOf(DoorType.Wither, DoorType.Blood)) return@forEach
                if (door.type == DoorType.Blood && bloodOpened) return@forEach

                val box = AABB(door.worldX - 1.0, 69.0, door.worldZ - 1.0, door.worldX + 2.0, 73.0, door.worldZ + 2.0)

                val isOpenable = when (door.type) {
                    DoorType.Wither -> witherKeys > 0
                    DoorType.Blood -> bloodKey
                    else -> false
                }
                drawStyledBox(box, if (isOpenable) openableColor else doorHighlightColor, BoxStyle.FILLED_OUTLINE, false)

                // Same doors as the boxes (the map scan retypes a door once it's opened), any wither door before blood
                if (!lineToDoor || playerPos == null) return@forEach
                val dist = box.center.distanceToSqr(playerPos)
                val better = when {
                    target == null -> true
                    target!!.type != door.type -> door.type == DoorType.Wither
                    else -> dist < targetDist
                }
                if (better) { target = door; targetDist = dist }
            }
            target?.let { drawTracer(Vec3(it.worldX + 0.5, 71.0, it.worldZ + 0.5), lineColor, false, lineThickness) }

            if (currentKey == null || currentKey?.entity == null) return@on
            currentKey?.let { keyType ->
                if (keyType.entity?.isAlive == false) {
                    currentKey = null
                    return@on
                }
                val position = keyType.entity?.position() ?: return@on
                drawStyledBox(AABB.unitCubeFromLowerCorner(position.add(-0.5, 1.0, -0.5)), keyType.color(), BoxStyle.FILLED_OUTLINE, depthCheck)
            }
        }

        on<LevelEvent.Load> {
            currentKey = null
            witherKeys = 0
            bloodKey = false
            bloodOpened = false
        }
    }

    private enum class KeyType(val displayName: String, val color: () -> Color, val colorCode: Char) {
        Wither("Wither Key", { witherColor }, '8'),
        Blood("Blood Key", { bloodColor }, 'c');

        var entity: Entity? = null
    }
}
