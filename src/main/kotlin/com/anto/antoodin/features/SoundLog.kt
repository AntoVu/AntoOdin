package com.anto.antoodin.features

import com.anto.antoodin.mixin.accessors.AbstractSoundInstanceAccessor
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.resources.sounds.SoundInstance

/**
 * Debug tool (/ao dev soundlog [phrase]). No phrase: prints every played sound to chat.
 * With a phrase: stays quiet until a chat line contains it, then prints the sounds from 1 s before to 1 s after.
 * Sounds muted by AntoOdin are still logged (recorded before the mute).
 */
object SoundLog {
    private const val WINDOW_MS = 1000L

    private class Entry(val time: Long, val text: String)

    private val entries = ArrayDeque<Entry>()
    @Volatile private var active = false
    @Volatile private var phrase: String? = null

    init {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            val p = phrase ?: return@register
            if (!active || overlay) return@register
            val line = message.string.noControlCodes
            if (!line.contains(p, ignoreCase = true)) return@register
            val at = System.currentTimeMillis()
            // Wait for the sounds that come right after the line
            schedule(WINDOW_MS.toInt() / 50 + 1) { dump(line, at) }
        }
    }

    /** No arg toggles off when on. Otherwise starts (or switches) logging: all sounds, or only around [newPhrase]. */
    fun command(newPhrase: String?) {
        val p = newPhrase?.trim()?.takeIf { it.isNotEmpty() }
        if (active && p == null) {
            active = false
            return modMessage("Sound log §coff§r.")
        }
        synchronized(entries) { entries.clear() }
        phrase = p
        active = true
        modMessage(if (p == null) "Sound log §aon§r: printing every sound." else "Sound log §aon§r: printing sounds around chat lines containing \"$p\".")
    }

    /** Called from SoundManagerMixin at play HEAD. Pitch/volume are the requested ones (getPitch() isn't resolved yet). */
    @JvmStatic
    fun record(sound: SoundInstance) {
        if (!active) return
        val acc = sound as? AbstractSoundInstanceAccessor
        val text = "${sound.identifier} §7pitch ${acc?.requestedPitch ?: "?"} vol ${acc?.requestedVolume ?: "?"}"
        if (phrase == null) return modMessage("§f$text")
        val now = System.currentTimeMillis()
        synchronized(entries) {
            entries.addLast(Entry(now, text))
            while (entries.first().time < now - WINDOW_MS * 3) entries.removeFirst()
        }
    }

    private fun dump(line: String, at: Long) {
        val near = synchronized(entries) { entries.filter { it.time in at - WINDOW_MS..at + WINDOW_MS } }
        modMessage("§eSounds around: §r$line")
        if (near.isEmpty()) return modMessage("§7  (none)")
        near.forEach { modMessage("§7  ${if (it.time >= at) "+" else ""}${it.time - at} ms §f${it.text}") }
    }
}
