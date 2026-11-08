package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.events.TickStartEvent
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.itemId

/**
 * Mining right after swapping to the Dungeon Breaker starts with the old item's ~0.3 s break time,
 * because the server hasn't seen the swap yet. Hypixel sends nothing back for a swap, so wait a set time instead.
 */
object BreakerSwapDelay : Module(
    name = "Breaker Swap Delay",
    description = "Blocks mining with the Dungeon Breaker for a moment after swapping to it, so it breaks instantly.",
    category = Skit.ANTO
) {
    private val delay by NumberSetting("Delay", 150, 0..1000, 10, desc = "Time to wait after swapping before mining. About your ping + 50.", unit = "ms")

    // When the breaker was first held, 0 while holding something else
    private var since = 0L

    init {
        on<TickStartEvent> { holding() }
    }

    private fun holding(): Boolean {
        val breaker = mc.player?.mainHandItem?.itemId == "DUNGEONBREAKER"
        if (!breaker) since = 0L else if (since == 0L) since = System.currentTimeMillis()
        return breaker
    }

    /** True if a block click should be dropped: the breaker was swapped to less than [delay] ms ago. */
    @JvmStatic
    fun shouldBlockMining(): Boolean =
        enabled && holding() && System.currentTimeMillis() - since < delay
}
