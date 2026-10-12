// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.client.renderer.entity.state.CreeperRenderState
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Creeper

/** @see com.anto.antoodin.mixin.mixins.CreeperRendererMixin */
object Ghosts : Module(
    name = "Ghosts",
    description = "Things for Ghosts",
    category = Skit.ODINCLIENT
) {
    private val showGhosts by BooleanSetting("Show Ghosts", false, desc = "Show the creeper entities.")
    private val showPowered by BooleanSetting("Show Powered Layer", true, desc = "Show the powered layer for creepers.")

    // Ghosts are Dwarven Mines creepers with a million max health
    @JvmStatic
    fun modifyRenderState(creeper: Creeper, state: CreeperRenderState) {
        if (!enabled || LocationUtils.currentArea != Island.DwarvenMines) return
        if (creeper.getAttributeBaseValue(Attributes.MAX_HEALTH) < 1_000_000) return

        if (state.isPowered && !showPowered) state.isPowered = false
        if (state.isInvisible && showGhosts) state.isInvisible = false
    }
}
