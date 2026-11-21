package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.mixin.accessors.AbstractSoundInstanceAccessor
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundEvents
import kotlin.math.abs

/**
 * The Magic Find drop jingle (recorded with /ao soundlog): four rising notes 100 ms apart, each a note block pling
 * at volume 0.8 with a 0.4 echo, plus an XP orb pickup at volume 0.2. Matched by exact pitch and volume.
 */
object MuteMagicFind : Module(
    name = "Mute Magic Find",
    description = "Mutes the Magic Find drop sound in dungeons.",
    category = Skit.ANTO
) {
    private val PLING_PITCHES = floatArrayOf(0.5873016f, 0.7936508f, 1.0476191f, 1.1746032f)
    private val ORB_PITCHES = floatArrayOf(0.6984127f, 0.93650794f, 1.2539682f, 1.4126984f)

    /** Called from SoundManagerMixin at play HEAD. Uses the requested pitch/volume: getPitch() isn't resolved yet there. */
    @JvmStatic
    fun shouldMute(sound: SoundInstance): Boolean {
        if (!enabled || !DungeonUtils.inDungeons) return false
        val acc = sound as? AbstractSoundInstanceAccessor ?: return false
        val pitches = when (sound.identifier) {
            SoundEvents.NOTE_BLOCK_PLING.value().location() ->
                if (near(acc.requestedVolume, 0.8f) || near(acc.requestedVolume, 0.4f)) PLING_PITCHES else return false
            SoundEvents.EXPERIENCE_ORB_PICKUP.location() -> if (near(acc.requestedVolume, 0.2f)) ORB_PITCHES else return false
            else -> return false
        }
        return pitches.any { near(acc.requestedPitch, it) }
    }

    private fun near(a: Float, b: Float) = abs(a - b) < 0.001f
}
