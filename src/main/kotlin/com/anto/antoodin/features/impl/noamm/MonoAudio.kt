// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import net.minecraft.world.phys.Vec3
import org.lwjgl.openal.AL10

/**
 * @see com.anto.antoodin.mixin.mixins.ChannelMixin
 * @see com.anto.antoodin.mixin.mixins.SoundEngineMixin
 */
object MonoAudio : Module(
    name = "Mono Audio",
    description = "Plays all game audio through a single channel, keeping distance falloff.",
    category = Skit.NOAMM
) {
    @JvmStatic
    fun distanceToListener(pos: Vec3): Double = pos.distanceTo(mc.soundManager.listenerTransform.position())

    // Straight ahead of a listener-relative source, so both ears hear it equally
    @JvmStatic
    fun applyCenteredPosition(source: Int, distance: Double) {
        AL10.alSource3f(source, AL10.AL_POSITION, 0f, 0f, -distance.toFloat())
    }
}
