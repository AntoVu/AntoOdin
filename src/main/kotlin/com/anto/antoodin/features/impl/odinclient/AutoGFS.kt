// Taken from OdinFabric (Copyright (c) 2023-2025, odtheking, BSD 3-Clause)
// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.events.TickStartEvent
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.fillItemFromSack
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.skyblock.KuudraUtils
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils

// OdinClient's "Auto Get Draft" was left out: Noamm's Architect Draft does the same thing
object AutoGFS : Module(
    name = "Auto GFS",
    description = "Automatically refills certain items from your sacks.",
    category = Skit.ODINCLIENT
) {
    private val inSkyblock by BooleanSetting("In Skyblock", true, desc = "gfs everywhere in skyblock.")
    private val inKuudra by BooleanSetting("In Kuudra", true, desc = "Only gfs in Kuudra.").withDependency { !inSkyblock }
    private val inDungeon by BooleanSetting("In Dungeon", true, desc = "Only gfs in Dungeons.").withDependency { !inSkyblock }

    private val refillOnDungeonStart by BooleanSetting("Refill on Dungeon Start", true, desc = "Refill when a dungeon starts.")
    private val refillOnTimer by BooleanSetting("Refill on Timer", false, desc = "Refill on a timed interval.")
    private val timerIncrements by NumberSetting("Timer Increments", 5, 1..60, 1, desc = "The interval in which to refill.", unit = "s").withDependency { refillOnTimer }

    private val refillPearl by BooleanSetting("Refill Pearl", true, desc = "Refill ender pearls.")
    private val refillJerry by BooleanSetting("Refill Jerry", true, desc = "Refill inflatable jerrys.")
    private val refillTNT by BooleanSetting("Refill TNT", true, desc = "Refill superboom tnt.")
    private val refillLeap by BooleanSetting("Refill Leaps", false, desc = "Refill spirit leaps.")

    private val startRegex = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.|\\[NPC] Mort: Right-click the Orb for spells, and Left-click \\(or Drop\\) to use your Ultimate!")

    private var last = 0

    init {
        on<TickStartEvent> {
            if (!refillOnTimer) return@on
            if (++last < timerIncrements * 20) return@on

            last = 0
            refill()
        }

        on<MessageEvent.Chat> {
            if (refillOnDungeonStart && message.matches(startRegex)) refill()
        }
    }

    private fun refill() {
        if (mc.gui.screen() != null) return
        val inventory = mc.player?.inventory ?: return
        if (inSkyblock && !LocationUtils.isInSkyblock) return
        if (!inSkyblock && !((inKuudra && KuudraUtils.inKuudra) || (inDungeon && DungeonUtils.inDungeons))) return

        if (refillLeap) inventory.find { it.itemId == "SPIRIT_LEAP" }?.also { fillItemFromSack(16, "SPIRIT_LEAP", "spirit_leap", false) }
        if (refillPearl) inventory.find { it.itemId == "ENDER_PEARL" }?.also { fillItemFromSack(16, "ENDER_PEARL", "ender_pearl", false) }
        if (refillJerry) inventory.find { it.itemId == "INFLATABLE_JERRY" }?.also { fillItemFromSack(64, "INFLATABLE_JERRY", "inflatable_jerry", false) }
        if (refillTNT) inventory.find { it.itemId == "SUPERBOOM_TNT" }?.also { fillItemFromSack(64, "SUPERBOOM_TNT", "superboom_tnt", false) }
    }
}
