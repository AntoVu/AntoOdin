// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.formatNumber
import com.odtheking.odin.utils.modMessage

object ExplosiveShot : Module(
    name = "Explosive Shot",
    description = "Shows the damage per enemy of Explosive Shot.",
    category = Skit.NOAMM
) {
    private val explosiveShotRegex = Regex("^Your Explosive Shot hit (\\d+) (?:enemy|enemies) for ([\\d,.]+) damage\\.$")

    init {
        on<MessageEvent.Chat> {
            val (hits, damage) = explosiveShotRegex.find(message)?.destructured ?: return@on
            val total = damage.replace(",", "").toDoubleOrNull() ?: return@on
            val perEnemy = total / hits.toInt().coerceAtLeast(1)
            modMessage("§aExplosive Shot did §e${formatNumber(perEnemy.toString())}§a damage per enemy.")
        }
    }
}
