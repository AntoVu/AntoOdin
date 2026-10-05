// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.dungeon.map.tile.RoomType
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import kotlin.random.Random

object ArchitectDraft : Module(
    name = "Architect Draft",
    description = "Gets an Architect's First Draft from your sacks when you fail a puzzle.",
    category = Skit.NOAMM
) {
    private val delay by NumberSetting("Delay", 1500, 0, 3000, 50, desc = "Delay in milliseconds before getting the draft.")
    private val delayVariety by NumberSetting("Delay Variety", 300, 0, 1000, 50, desc = "Random extra delay in milliseconds added on top of Delay.")

    private val failRegexes = listOf(
        Regex("^PUZZLE FAIL! (\\w{1,16}) .+$"),
        Regex("^\\[STATUE] Oruo the Omniscient: (\\w{1,16}) chose the wrong answer! I shall never forget this moment of misrememberance\\.$")
    )

    init {
        on<MessageEvent.Chat> {
            if (!DungeonUtils.inDungeons || DungeonUtils.inBoss) return@on
            if (DungeonUtils.currentRoom?.data?.type != RoomType.PUZZLE) return@on
            val player = failRegexes.firstNotNullOfOrNull { it.find(message) }?.groupValues?.get(1) ?: return@on
            if (player != mc.user.name) return@on

            val finalDelay = delay.toLong() + Random.nextLong(0, delayVariety.toLong() + 1)
            schedule(((finalDelay / 1000.0) * 20).toInt().coerceAtLeast(1)) {
                sendCommand("gfs ARCHITECT_FIRST_DRAFT 1")
            }
        }
    }
}
