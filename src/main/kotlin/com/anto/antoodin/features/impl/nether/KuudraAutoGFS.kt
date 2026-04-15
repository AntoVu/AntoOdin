package com.anto.antoodin.features.impl.nether

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import kotlin.random.Random

object KuudraAutoGFS : Module(
    name = "Kuudra Auto GFS",
    description = "Automatically gets arrow poison from sacks after the ballista is built.",
    key = null
) {
    private val arrowPoisonType by SelectorSetting(
        "Type",
        "Toxic Arrow Poison",
        listOf("Toxic Arrow Poison", "Twilight Arrow Poison"),
        desc = "Which arrow poison to pull from sacks."
    )

    private val amount by NumberSetting(
        "Amount",
        32,
        1,
        64,
        1,
        desc = "How much arrow poison to get from sacks."
    )

    private val ballistaRegex = Regex(
        "^\\[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!$"
    )

    init {
        on<ChatPacketEvent> {
            if (LocationUtils.currentArea != Island.Kuudra) return@on
            if (!ballistaRegex.matches(value)) return@on

            val itemName = if (arrowPoisonType == 0) "Toxic_Arrow_Poison" else "Twilight_Arrow_Poison"
            val delay = Random.nextLong(0, 101)

            Thread {
                Thread.sleep(delay)
                mc.execute {
                    mc.player?.connection?.sendCommand("gfs $itemName $amount")
                }
            }.start()
        }
    }
}