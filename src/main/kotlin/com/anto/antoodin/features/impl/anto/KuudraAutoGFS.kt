package com.anto.antoodin.features.impl.anto

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.anto.antoodin.features.SubFeature
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import kotlin.random.Random

object KuudraAutoGFS : SubFeature(
    name = "Kuudra Auto GFS",
    description = "Automatically gets arrow poison from sacks after the ballista is built."
) {
    enum class Poison { TOXIC_ARROW_POISON, TWILIGHT_ARROW_POISON }

    private val arrowPoisonType by SelectorSetting(
        "Type",
        Poison.TOXIC_ARROW_POISON,
        desc = "Which arrow poison to pull from sacks."
    )

    private val amount by NumberSetting(
        "Amount",
        32,
        1..64,
        1,
        desc = "How much arrow poison to get from sacks."
    )

    private val ballistaRegex = Regex(
        "^\\[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!$"
    )

    init {
        on<MessageEvent.Chat> {
            if (LocationUtils.currentArea != Island.Kuudra) return@on
            if (!ballistaRegex.matches(message)) return@on

            val itemName = if (arrowPoisonType == Poison.TOXIC_ARROW_POISON) "Toxic_Arrow_Poison" else "Twilight_Arrow_Poison"
            val delayTicks = Random.nextInt(0, 3)

            schedule(delayTicks) {
                mc.player?.connection?.sendCommand("gfs $itemName $amount")
            }
        }
    }
}