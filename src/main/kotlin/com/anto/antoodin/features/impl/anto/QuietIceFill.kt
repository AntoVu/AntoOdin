package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundEvents
import kotlin.math.abs

/** Ice Fill breaks every ice block at once, each with its own glass break sound (block break event: pitch 1.0 * 0.8). */
object QuietIceFill : Module(
    name = "Quiet Ice Fill",
    description = "Plays only one ice break sound when many Ice Fill blocks break at once.",
    category = Skit.ANTO
) {
    // Breaks that arrive together land within a tick or two; the sound itself is ~0.3 s
    private const val WINDOW_MS = 100L
    private var lastPlayed = 0L

    /** True if this sound is a repeat glass break in Ice Fill and should be dropped. Called from SoundManagerMixin. */
    @JvmStatic
    fun shouldMute(sound: SoundInstance): Boolean {
        if (!enabled || sound.identifier != SoundEvents.GLASS_BREAK.location() || abs(sound.pitch - 0.8f) > 0.01f) return false
        if (DungeonUtils.currentRoomName != "Ice Fill") return false
        val now = System.currentTimeMillis()
        if (now - lastPlayed < WINDOW_MS) return true
        lastPlayed = now
        return false
    }
}
