// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.client.color.block.BlockTintSources
import net.minecraft.client.renderer.block.FluidModel
import net.minecraft.client.renderer.block.FluidStateModelSet
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids

/**
 * Lava gets the whole water model, including its translucent layer, so it's see-through.
 *
 * @see com.anto.antoodin.mixin.mixins.FluidStateModelSetMixin
 * @see com.anto.antoodin.mixin.mixins.LavaFogEnvironmentMixin
 */
object LavaToWater : Module(
    name = "Lava To Water",
    description = "Renders lava as see-through water and removes lava fog.",
    category = Skit.NOAMM
) {
    private val areas by DropdownSetting("Areas", desc = "Where lava is replaced.")
    private val everywhere by BooleanSetting("Everywhere", false, desc = "Replaces lava everywhere, ignoring the areas below.").withDependency { areas }
    private val catacombs by BooleanSetting("Catacombs", true, desc = "Replaces lava in dungeons.").withDependency { areas && !everywhere }
    private val kuudra by BooleanSetting("Kuudra", true, desc = "Replaces lava in Kuudra.").withDependency { areas && !everywhere }
    private val crimsonIsle by BooleanSetting("Crimson Isle", true, desc = "Replaces lava on the Crimson Isle.").withDependency { areas && !everywhere }

    private val colorTint by BooleanSetting("Color Tint", false, desc = "Tints the replaced lava and its fog.")
    private val tintColor by ColorSetting("Tint Color", Color(63, 118, 228), desc = "Color of the tint.").withDependency { colorTint }
    private val hideFog by BooleanSetting("Hide Fog", true, desc = "Removes the fog while you are in lava.")

    // Read on chunk build threads, so only these fields are touched there
    @Volatile private var active = false
    @Volatile private var tint: Int? = null
    // What the built chunks currently show; starts as plain lava so joining doesn't trigger a reload
    private var applied: Pair<Boolean, Int?> = false to null

    init {
        // Settings have no change listener and the area is known a bit after joining, so poll
        on<TickEvent.End> { update() }
    }

    override fun onEnable() {
        super.onEnable()
        update()
    }

    override fun onDisable() {
        super.onDisable()
        active = false
        rebuildIfChanged()
    }

    private fun update() {
        active = everywhere || (catacombs && LocationUtils.isCurrentArea(Island.Dungeon)) ||
            (kuudra && LocationUtils.isCurrentArea(Island.Kuudra)) || (crimsonIsle && LocationUtils.isCurrentArea(Island.CrimsonIsle))
        tint = if (colorTint) tintColor.rgba else null
        rebuildIfChanged()
    }

    private fun rebuildIfChanged() {
        val state = active to tint.takeIf { active }
        if (state == applied) return
        applied = state
        mc.execute { mc.levelExtractor.allChanged() }
    }

    @JvmStatic
    fun replaceModel(self: FluidStateModelSet, state: FluidState, original: FluidModel): FluidModel {
        if (!active || (state.type != Fluids.LAVA && state.type != Fluids.FLOWING_LAVA)) return original
        val water = self.get(Fluids.WATER.defaultFluidState())
        val color = tint ?: return water
        return FluidModel(water.layer(), water.stillMaterial(), water.flowingMaterial(), water.overlayMaterial(), BlockTintSources.constant(color, color))
    }

    @JvmStatic
    fun shouldHideFog(): Boolean = active && hideFog

    /** Fog color while in lava, null to keep vanilla's. */
    @JvmStatic
    fun fogColor(): Int? = if (active) tint else null

    @JvmStatic
    fun isActive(): Boolean = active
}
