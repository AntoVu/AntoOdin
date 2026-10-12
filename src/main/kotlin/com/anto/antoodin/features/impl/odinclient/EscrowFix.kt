// Taken from OdinFabric (Copyright (c) 2023-2025, odtheking, BSD 3-Clause)
// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.sendCommand

object EscrowFix : Module(
    name = "Escrow Fix",
    description = "Automatically reopens the ah/bz when it gets closed by escrow.",
    category = Skit.ODINCLIENT
) {
    private val messages = mapOf(
        "There was an error with the auction house! (AUCTION_EXPIRED_OR_NOT_FOUND)" to "ah",
        "There was an error with the auction house! (INVALID_BID)" to "ah",
        "Claiming BIN auction..." to "ah",
        "Visit the Auction House to collect your item!" to "ah"
    )

    private val regex = Regex("Escrow refunded (\\d+) coins for Bazaar Instant Buy Submit!")

    init {
        on<MessageEvent.Chat> {
            val command = messages[message] ?: if (message.matches(regex)) "bz" else null
            command?.let { sendCommand(it) }
        }
    }
}
