// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.features.Module
import java.time.LocalTime

/**
 * Changes the time the sky and fog are drawn at. Render only: the world clock and everything else keep the server's time.
 *
 * @see com.anto.antoodin.mixin.mixins.SkyRendererMixin
 * @see com.anto.antoodin.mixin.mixins.AtmosphericFogEnvironmentMixin
 * @see com.anto.antoodin.mixin.mixins.AttributeTrackSamplerMixin
 */
object TimeChanger : Module(
    name = "Time Changer",
    description = "Changes the time of day the sky is drawn at.",
    category = Skit.NOAMM
) {
    enum class Time(val ticks: Long) {
        DAY(1000), NOON(6000), SUNSET(12000), NIGHT(13000), MIDNIGHT(18000), SUNRISE(23000), REAL_TIME(-1), CUSTOM(-1)
    }

    private val time by SelectorSetting("Time", Time.DAY, desc = "Time of day to show.")
    private val customTime by NumberSetting("Custom Time", 6000, 0..23999, 100, desc = "The world time in ticks (0 = sunrise, 6000 = noon, 18000 = midnight).").withDependency { time == Time.CUSTOM }

    // Set while the sky or fog is being extracted, so only those samples see the changed time (render thread only)
    @JvmField var extractingSky = false

    @JvmStatic
    fun getTime(): Long = when (time) {
        Time.REAL_TIME -> LocalTime.now().let { Math.floorMod(it.hour * 1000L + (it.minute * 16.66).toLong() - 6000, 24000L) }
        Time.CUSTOM -> customTime.toLong()
        else -> time.ticks
    }
}
