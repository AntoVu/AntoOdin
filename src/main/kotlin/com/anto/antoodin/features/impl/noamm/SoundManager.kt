// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.MapSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

/**
 * @see com.anto.antoodin.mixin.mixins.AbstractSoundInstanceMixin
 * @see com.anto.antoodin.mixin.mixins.SoundManagerMixin
 */
object SoundManager : Module(
    name = "Sound Manager",
    description = "Adjust the volume of every sound in the game. Open it with /ao sounds.",
    category = Skit.NOAMM,
    toggled = true
) {
    private val openScreen by ActionSetting("Open Sound Manager", desc = "Opens the per-sound volume menu.") {
        mc.setScreen(SoundManagerScreen(mc.screen))
    }

    // Sound id -> volume multiplier. Sounds at 100% are left out
    private val volumes by MapSetting("Volumes", mutableMapOf<String, Float>())

    private const val MAX_RECENT = 100
    private val recentSounds = LinkedHashSet<Identifier>()

    init {
        on<LevelEvent.Load> { synchronized(recentSounds) { recentSounds.clear() } }
    }

    @JvmStatic
    fun getMultiplier(id: Identifier): Float = if (enabled) volumes[id.toString()] ?: 1f else 1f

    // Recorded even while disabled so the Recent list is ready when the module is turned on
    @JvmStatic
    fun recordPlayedSound(sound: SoundInstance) {
        if (mc.screen is SoundManagerScreen) return
        synchronized(recentSounds) {
            recentSounds.remove(sound.identifier)
            recentSounds.add(sound.identifier)
            if (recentSounds.size > MAX_RECENT) recentSounds.remove(recentSounds.first())
        }
    }

    fun recentSounds(): List<Identifier> = synchronized(recentSounds) { recentSounds.toList().asReversed() }

    fun changedSounds(): List<Identifier> = volumes.keys.mapNotNull(Identifier::tryParse)

    fun getVolumePercent(id: Identifier): Int = ((volumes[id.toString()] ?: 1f) * 100f).roundToInt()

    // Steps of 5, 0-200%
    fun setVolumePercent(id: Identifier, percent: Int) {
        val rounded = ((percent / 5f).roundToInt() * 5).coerceIn(0, 200)
        if (rounded == 100) volumes.remove(id.toString()) else volumes[id.toString()] = rounded / 100f
    }
}
