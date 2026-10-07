package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.BoxStyle
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB

object AloeHighlight : Module(
    name = "Aloe Highlight",
    description = "Highlights aloe in the Garden by growth stage.",
    category = Skit.ANTO
) {
    private val notReadyColor by ColorSetting("Not Ready Color", Colors.MINECRAFT_RED, true, desc = "Color for Stage 1-10.")
    private val notOptimalColor by ColorSetting("Not Optimal Color", Colors.MINECRAFT_YELLOW, true, desc = "Color for Stage 11-12.")
    private val readyColor by ColorSetting("Ready Color", Colors.MINECRAFT_GREEN, true, desc = "Color for Stage 13-14.")
    private val harvestNowColor by ColorSetting("Harvest Now Color", Colors.MINECRAFT_AQUA, true, desc = "Color for Stage 15 and above.")
    private val renderStyle by SelectorSetting("Render Style", BoxStyle.OUTLINE, desc = "Style of the box.")
    private val depthCheck by BooleanSetting("Depth Check", true, desc = "Disable to see aloe through other crops and blocks.")

    private val stageRegex = Regex("^Stage (\\d+)$")

    // Name tag stand -> stage, kept up to date from entity data instead of scanning every tick
    private val aloes = HashMap<Entity, Int>()

    init {
        on<EntityEvent.SetData> { update(entity) }

        on<EntityEvent.Remove> { aloes.remove(entity) }

        on<LevelEvent.Load> { aloes.clear() }

        on<RenderExtractEvent> {
            if (aloes.isEmpty() || !LocationUtils.isCurrentArea(Island.Garden)) return@on
            aloes.forEach { (stand, stage) ->
                if (!stand.isAlive) return@forEach
                // The stand floats in the block above the crop
                val pos = BlockPos.containing(stand.x, stand.y, stand.z).below()
                drawStyledBox(AABB(pos), colorFor(stage), renderStyle, depthCheck)
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        // Entity data only reaches us while enabled, so pick up stands that already exist
        mc.level?.entitiesForRendering()?.forEach(::update)
    }

    override fun onDisable() {
        super.onDisable()
        aloes.clear()
    }

    private fun update(entity: Entity) {
        if (entity !is ArmorStand) return
        val stage = stageRegex.find(entity.name.string.noControlCodes)?.groupValues?.get(1)?.toIntOrNull()
        if (stage != null) aloes[entity] = stage else aloes.remove(entity)
    }

    private fun colorFor(stage: Int): Color = when {
        stage >= 15 -> harvestNowColor
        stage >= 13 -> readyColor
        stage >= 11 -> notOptimalColor
        else -> notReadyColor
    }
}
