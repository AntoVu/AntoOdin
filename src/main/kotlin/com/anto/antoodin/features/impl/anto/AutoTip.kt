// Feature idea from Hypixel AutoTip by Lilyy2565 (MIT). Written for AntoOdin.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.network.chat.Component

object AutoTip : Module(
    name = "Auto Tip",
    description = "Runs /tipall on Hypixel every few minutes.",
    category = Skit.ANTO
) {
    private val interval by NumberSetting("Interval", 1000, 300..3600, 10, unit = "s", desc = "Time between tips. Hypixel AutoTip's default is 1000 s (about 17 min).")
    private val showMessage by BooleanSetting("Show Tip Message", true, desc = "Shows Hypixel's reply to the automatic /tipall.")

    private val tipRegex = Regex("^(You tipped \\d+ players?|You already tipped everyone|No one has a network booster active)")

    // Starts 5 s short of the interval so the first tip goes out right after joining
    private var ticks = -1
    @Volatile private var lastTip = 0L

    init {
        on<TickEvent.End> {
            val player = mc.player ?: return@on
            if (mc.currentServer?.ip?.contains("hypixel.net", true) != true) return@on
            if (ticks < 0) ticks = interval * 20 - 100
            if (++ticks < interval * 20) return@on
            ticks = 0
            lastTip = System.currentTimeMillis()
            player.connection.sendCommand("tipall")
        }
    }

    /** Hides the reply to the automatic /tipall only, so manual ones still show. */
    @JvmStatic
    fun shouldHide(message: Component): Boolean =
        enabled && !showMessage && System.currentTimeMillis() - lastTip < 10_000 && tipRegex.containsMatchIn(message.string.noControlCodes)
}
