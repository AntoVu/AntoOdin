// Feature idea from NoFrills (GPL-3.0). Written from scratch for AntoOdin, no NoFrills code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import kotlin.math.abs

/**
 * @see com.anto.antoodin.mixin.mixins.AbstractSoundInstanceMixin
 * @see com.anto.antoodin.mixin.mixins.GameRendererMixin
 * @see com.anto.antoodin.mixin.mixins.FramerateLimitTrackerMixin
 */
object UnfocusedTweaks : Module(
    name = "Unfocused Tweaks",
    description = "Lowers sound, FPS and rendering while Minecraft isn't the focused window.",
    category = Skit.ANTO
) {
    private val skipWorldRender by BooleanSetting("Skip World Render", false, desc = "Skips drawing the world while unfocused. The HUD still draws.")
    private val volume by NumberSetting("Unfocused Volume", 100, 0..100, 5, unit = "%", desc = "Volume of all game sounds while unfocused, as a percentage of normal. 0% mutes.")
    private val allowPmSound by BooleanSetting("Allow PM Sound", true, desc = "The private message sound plays at normal volume while unfocused.")
    private val noVanillaLimit by BooleanSetting("No Vanilla Limit", false, desc = "Turns off vanilla's FPS reduction when unfocused or idle.")
    private val fpsLimit by NumberSetting("FPS Limit", 0, 0..240, 5, desc = "Max FPS while unfocused. 0 leaves it alone.")

    // Hypixel's private message sound. The pitch keeps other uses of the orb sound quiet
    private val PM_SOUND = Identifier.withDefaultNamespace("entity.experience_orb.pickup")
    private const val PM_PITCH = 1.0f

    private var wasUnfocused = false

    init {
        // Playing sounds only pick up a new volume when refreshed, so do that whenever focus changes
        on<TickEvent.End> {
            val unfocused = unfocused()
            if (unfocused != wasUnfocused) refreshVolumes()
            wasUnfocused = unfocused
        }
    }

    override fun onDisable() {
        super.onDisable()
        if (wasUnfocused) refreshVolumes()
        wasUnfocused = false
    }

    private fun unfocused() = enabled && !mc.isWindowActive

    private fun refreshVolumes() = SoundSource.entries.forEach { runCatching { mc.soundManager.refreshCategoryVolume(it) } }

    @JvmStatic
    fun volumeMultiplier(id: Identifier, pitch: Float): Float {
        if (!unfocused() || volume == 100) return 1f
        if (allowPmSound && id == PM_SOUND && abs(pitch - PM_PITCH) < 0.01f) return 1f
        return volume / 100f
    }

    @JvmStatic
    fun skipWorld(): Boolean = skipWorldRender && unfocused()

    // -1 keeps vanilla's limit
    @JvmStatic
    fun frameLimit(): Int = when {
        fpsLimit > 0 && unfocused() -> fpsLimit
        enabled && noVanillaLimit -> mc.options.framerateLimit().get()
        else -> -1
    }
}
