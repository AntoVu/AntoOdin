package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.features.SubFeature
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundEvents

/** Ice Fill breaks every ice block at once, each with its own glass break sound. */
object QuietIceFill : SubFeature(
    name = "Quiet Ice Fill",
    description = "Plays only one ice break sound when many Ice Fill blocks break at once."
) {
    // Breaks that arrive together land within a tick or two; the sound itself is ~0.3 s
    private const val WINDOW_MS = 100L
    private var lastPlayed = 0L

    /** True if this sound is a repeat glass break in Ice Fill and should be dropped. Called from SoundManagerMixin. */
    @JvmStatic
    fun shouldMute(sound: SoundInstance): Boolean {
        // Don't read pitch/volume: at play HEAD the sound isn't resolved yet and they throw (disconnects the client)
        if (!enabled || DungeonUtils.currentRoomName != "Ice Fill" || sound.identifier != SoundEvents.GLASS_BREAK.location()) return false
        val now = System.currentTimeMillis()
        if (now - lastPlayed < WINDOW_MS) return true
        lastPlayed = now
        return false
    }
}
