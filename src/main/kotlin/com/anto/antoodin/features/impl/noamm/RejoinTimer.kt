// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.LocationUtils

/** No party message option: Odin's Dungeon Queue already has "Announce Kick". */
object RejoinTimer : Module(
    name = "Rejoin Timer",
    description = "Shows how long ago you were kicked from SkyBlock, so you know when you can rejoin.",
    category = Skit.NOAMM
) {
    private val kickMessages = setOf(
        "There was a problem joining SkyBlock, try again in a moment!",
        "A kick occurred in your connection, so you were put in the SkyBlock lobby!",
        "You were kicked while joining that server!"
    )
    private const val SHOW_MS = 60_000L
    private const val BACK_IN_SKYBLOCK_MS = 10_000L

    private var lastKick: Long? = null

    private val hud by HUD(name, "Time since you were last kicked from SkyBlock.", false) { example ->
        val kickedAt = lastKick
        val since = if (example) 12_340L else System.currentTimeMillis() - (kickedAt ?: return@HUD 0 to 0)
        if (!example && (since >= SHOW_MS || (LocationUtils.isInSkyblock && since > BACK_IN_SKYBLOCK_MS))) {
            lastKick = null
            return@HUD 0 to 0
        }
        textDim("§cLast kicked from SkyBlock §b${"%.2f".format(since / 1000.0)}s ago", 0, 0)
    }

    init {
        on<MessageEvent.Chat> {
            if (message !in kickMessages) return@on
            // Repeat kick messages while the timer runs don't restart it
            val now = System.currentTimeMillis()
            if (lastKick.let { it == null || now - it >= SHOW_MS }) lastKick = now
        }
    }
}
