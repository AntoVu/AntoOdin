package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.boss.termsim.TermSimGUI
import com.odtheking.odin.utils.skyblock.dungeon.terminals.terminalhandler.TerminalHandler
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.sounds.SoundEvents

/**
 * Hypixel takes at most 5 terminal clicks in any 500 ms and silently ignores the rest, while Odin's
 * Client Prediction already shows them as done. This holds such a click back instead.
 *
 * Hypixel confirms each click it takes with a pling (volume 8, pitch 4.047619, the one Odin's
 * Terminal Sounds replaces). Our clicks are matched to plings in order, which gives the real
 * acceptance times and the round trip. A new click is predicted to land one round trip from now,
 * and is held back if 5 clicks would then be inside the window: unconfirmed ones (they may all land
 * at once after a lag spike) plus confirmed ones less than 500 ms before it.
 * Completion lines (terminal, lever, device, gate, core) come with the same pling, so each takes
 * one pling of its server tick before our clicks do.
 */
object TerminalClickLimit : Module(
    name = "Terminal Click Limit",
    description = "Holds back terminal clicks Hypixel would ignore (more than 5 in 500 ms), so Odin's solver never shows them as done. Needs Odin's Terminal Solver.",
    category = Skit.ANTO
) {
    private const val MAX_CLICKS = 5
    private const val WINDOW_NS = 500_000_000L
    private const val TICK_NS = 50_000_000L
    // Expected confirm time before any round trip is known
    private const val DEFAULT_RTT_NS = 1_000_000_000L
    // Server ticks past the expected confirm before a click is written off as ignored
    private const val GRACE_TICKS = 3
    // Written off regardless, in case server ticks stop arriving
    private const val MAX_WAIT_NS = 3_000_000_000L
    private const val PLING_VOLUME = 8f
    private const val PLING_PITCH = 4.047619f

    private val completionParts = listOf(" activated a terminal! (", " activated a lever! (", " completed a device! (")
    private val completionLines = setOf("The gate has been destroyed!", "The Core entrance is opening!")

    private class Sent(val atNs: Long, val tick: Long)

    // Packets arrive on the netty thread, clicks on the main thread: all state below is guarded by lock
    private val lock = Any()
    private val unconfirmed = ArrayDeque<Sent>()
    private val confirmedAt = ArrayDeque<Long>()
    private val roundTrips = ArrayDeque<Long>()
    private var serverTicks = 0L
    private var completionPlings = 0

    init {
        on<TickEvent.Server> {
            synchronized(lock) { serverTicks++; completionPlings = 0 }
        }

        onReceive<ClientboundSystemChatPacket> {
            if (overlay) return@onReceive
            val text = content.string.trim()
            if (text in completionLines || completionParts.any { it in text }) synchronized(lock) { completionPlings++ }
        }

        // Still called when Odin's Terminal Sounds cancels the pling (listeners don't ignore cancelled events)
        onReceive<ClientboundSoundPacket> {
            if (sound.value() != SoundEvents.NOTE_BLOCK_PLING.value() || volume != PLING_VOLUME || pitch != PLING_PITCH) return@onReceive
            val now = System.nanoTime()
            synchronized(lock) {
                if (completionPlings > 0) { completionPlings--; return@onReceive }
                val click = unconfirmed.removeFirstOrNull() ?: return@onReceive
                confirmedAt.addLast(now)
                if (confirmedAt.size > MAX_CLICKS) confirmedAt.removeFirst()
                roundTrips.addLast(now - click.atNs)
                if (roundTrips.size > 8) roundTrips.removeFirst()
            }
        }

        on<LevelEvent.Load> { reset() }
    }

    override fun onDisable() {
        super.onDisable()
        reset()
    }

    private fun reset() = synchronized(lock) {
        unconfirmed.clear(); confirmedAt.clear(); roundTrips.clear(); completionPlings = 0
    }

    /** Called by TerminalHandlerMixin at the start of every Odin terminal click; true cancels it. */
    @JvmStatic
    fun shouldBlock(term: TerminalHandler, slotIndex: Int, button: Int): Boolean {
        if (!enabled || mc.gui.screen() is TermSimGUI) return false
        // Odin returns early on these without sending anything
        if (!term.canClick(slotIndex, button) || term.shouldProtect()) return false

        val now = System.nanoTime()
        synchronized(lock) {
            dropIgnored(now)
            val lands = now + (roundTrips.minOrNull() ?: 0L)
            val inWindow = unconfirmed.size + confirmedAt.count { lands - it < WINDOW_NS }
            if (inWindow >= MAX_CLICKS) return true
            unconfirmed.addLast(Sent(now, serverTicks))
            return false
        }
    }

    // Clicks whose pling is overdue were ignored by Hypixel and don't count toward the limit
    private fun dropIgnored(now: Long) {
        // Slowest recent round trip, so a ping rise doesn't write off clicks that are still coming
        val waitTicks = (roundTrips.maxOrNull() ?: DEFAULT_RTT_NS) / TICK_NS + 1 + GRACE_TICKS
        while (unconfirmed.isNotEmpty()) {
            val oldest = unconfirmed.first()
            if (serverTicks - oldest.tick <= waitTicks && now - oldest.atNs <= MAX_WAIT_NS) break
            unconfirmed.removeFirst()
        }
    }
}
