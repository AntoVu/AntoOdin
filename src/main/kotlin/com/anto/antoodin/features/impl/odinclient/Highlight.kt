// Taken from OdinFabric (Copyright (c) 2023-2025, odtheking, BSD 3-Clause)
// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.MapSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.BoxStyle
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawTracer
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.renderPos
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player

// Starred mobs and Teammate Class Glow were left out: Odin's own Highlight module does both
object Highlight : Module(
    name = "Highlight (C)",
    description = "ESP for withers, bats and mobs added with /ao highlight.",
    category = Skit.ODINCLIENT
) {
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to enable ESP")
    private val color by ColorSetting("Highlight color", Colors.WHITE, true, desc = "Default color for mobs added without one.")
    private val renderStyle by SelectorSetting("Render Style", BoxStyle.OUTLINE, desc = "Style of the box.")
    private val highlightWither by BooleanSetting("Highlight Withers", true, desc = "Highlights Necron, Goldor, Storm and Maxor.")
    private val witherColor by ColorSetting("Wither ESP Color", Color(255, 0, 0, 1f), true, desc = "The color of the wither highlight.").withDependency { highlightWither }
    private val witherTracer by BooleanSetting("Wither Tracer", true, desc = "Draws a tracer to the wither boss in P3.").withDependency { highlightWither }
    private val highlightBats by BooleanSetting("Highlight Bats", true, desc = "Highlights bats in dungeons.")
    private val batColor by ColorSetting("Bat color", Color(0, 255, 255, 1f), true, desc = "The color of the bat highlight.").withDependency { highlightBats }
    private val customTracer by BooleanSetting("Custom tracer", false, desc = "Draws a tracer to the mobs added manually")

    private val colorRegex = Regex("^(.*?)(?:\\s+#?([0-9a-fA-F]{6}))?$")

    // Lowercase name part -> color
    private val highlightMap by MapSetting("highlightMap", mutableMapOf<String, Color>())

    private val customIds = hashMapOf<Int, Color>()
    private val witherIds = hashSetOf<Int>()
    private val spiritSceptreIds = hashSetOf<Int>()

    init {
        // Main thread entity data, which also fires when a name tag changes
        on<EntityEvent.SetData> {
            if (!entity.isAlive) return@on
            val d = DungeonUtils.inDungeons
            val b = DungeonUtils.inBoss

            when {
                d && DungeonUtils.getF7Phase() != M7Phases.P5 && highlightWither && entity is WitherBoss && !entity.isInvisible && (entity as WitherBoss).invulnerableTicks != 800 ->
                    witherIds.add(entity.id)

                // Spirit Sceptre bats spawn on the player, so skip those
                d && !b && highlightBats && entity is Bat && !entity.isPassenger && !entity.isInvisible -> {
                    val player = mc.player ?: return@on
                    if (player.distanceTo(entity) < 1.0) spiritSceptreIds.add(entity.id)
                }

                !b && highlightMap.isNotEmpty() && entity is ArmorStand -> {
                    val nameLower = entity.customName?.string?.noControlCodes?.lowercase() ?: return@on
                    val match = highlightMap.entries.firstOrNull { nameLower.contains(it.key) } ?: return@on
                    (entity as ArmorStand).mobBelow()?.let { customIds[it.id] = match.value }
                }
            }
        }

        on<RenderExtractEvent> {
            val world = mc.level ?: return@on
            val tracerWither = witherTracer && DungeonUtils.getF7Phase() == M7Phases.P3

            witherIds.forEach { id ->
                val entity = world.getEntity(id) as? WitherBoss ?: return@forEach
                if (!entity.isAlive) return@forEach
                drawStyledBox(entity.renderBoundingBox, witherColor, renderStyle, depthCheck)
                if (entity.invulnerableTicks != 0 && tracerWither) drawTracer(entity.renderPos, witherColor, depthCheck)
            }

            if (highlightBats && DungeonUtils.inDungeons && !DungeonUtils.inBoss) {
                world.entitiesForRendering()
                    .filterIsInstance<Bat>()
                    .filter { !it.isPassenger && !it.isInvisible && it.isAlive && it.id !in spiritSceptreIds }
                    .forEach { drawStyledBox(it.renderBoundingBox, batColor, renderStyle, depthCheck) }
            }

            customIds.forEach { (id, color) ->
                val entity = world.getEntity(id) ?: return@forEach
                if (!entity.isAlive) return@forEach
                drawStyledBox(entity.renderBoundingBox, color, renderStyle, depthCheck)
                if (customTracer) drawTracer(entity.renderPos, color, depthCheck)
            }
        }

        on<LevelEvent.Load> {
            customIds.clear()
            witherIds.clear()
            spiritSceptreIds.clear()
        }
    }

    fun add(arg: String) {
        val (key, color) = arg.parse() ?: return modMessage("Invalid format. Use: /ao highlight add <mob name> [#RRGGBB]")
        if (highlightMap.containsKey(key)) return modMessage("\"$key\" is already highlighted.")

        highlightMap[key] = color ?: this.color
        ModuleManager.saveConfigurations()
        modMessage("Added \"$key\" to highlight list${color?.let { " with color #%02X%02X%02X".format(it.red, it.green, it.blue) } ?: ""}.")
    }

    fun remove(name: String) {
        val key = name.trim().lowercase()
        if (highlightMap.remove(key) == null) return modMessage("\"$key\" isn't highlighted.")
        ModuleManager.saveConfigurations()
        modMessage("Removed \"$key\" from highlight list.")
    }

    fun clear() {
        if (highlightMap.isEmpty()) return modMessage("Highlight list is already empty.")
        highlightMap.clear()
        ModuleManager.saveConfigurations()
        modMessage("Highlight list cleared.")
    }

    fun list() {
        if (highlightMap.isEmpty()) return modMessage("Highlight list is empty.")
        modMessage("Highlight list:\n" + highlightMap.entries.joinToString("\n") { (name, color) -> "$name - #%02X%02X%02X".format(color.red, color.green, color.blue) })
    }

    fun names(): Set<String> = highlightMap.keys

    // The mob a name tag stand belongs to: overlapping it, or spawned right before it
    private fun ArmorStand.mobBelow(): Entity? =
        mc.level?.getEntities(this, boundingBox.inflate(0.0, 1.0, 0.0)) { isValidEntity(it) }?.firstOrNull()
            ?: mc.level?.getEntity(id - 1)?.takeIf { isValidEntity(it) }

    private fun isValidEntity(entity: Entity): Boolean =
        when (entity) {
            is ArmorStand -> false
            is Player -> entity.uuid.version() == 2 && entity != mc.player
            else -> true
        }

    private fun String.parse(): Pair<String, Color?>? {
        val match = colorRegex.matchEntire(trim()) ?: return null
        val name = match.groupValues[1].trim().takeIf { it.isNotEmpty() } ?: return null
        val hex = match.groupValues[2]
        return name.lowercase() to if (hex.isNotEmpty()) Color(hex + "ff") else null
    }
}
