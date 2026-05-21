package com.anto.antoodin.features.impl.anto

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.anto.antoodin.utils.Skit
import java.util.Random as JavaRandom

object DianaAutoWarp : Module(
    name = "Diana Auto Warp",
    description = "Automatically warps to next location after digging burrow. Requires SkyHanni.",
    category = Skit.ANTO
) {
    private val javaRandom = JavaRandom()

    private val randomDelay by BooleanSetting(
        "Random Delay",
        true,
        desc = "Use gaussian random delay instead of a fixed delay."
    )
    private val warpDelay by NumberSetting(
        "Warp Delay",
        1000,
        500,
        3000,
        100,
        desc = "Fixed delay in milliseconds before sending the warp command."
    ).withDependency { !randomDelay }
    private val minRandomDelay by NumberSetting(
        "Min Delay",
        800,
        200,
        3000,
        100,
        desc = "Minimum delay in milliseconds."
    ).withDependency { randomDelay }
    private val maxRandomDelay by NumberSetting(
        "Max Delay",
        1500,
        500,
        3000,
        100,
        desc = "Maximum delay in milliseconds."
    ).withDependency { randomDelay }

    private val burrowRegex = Regex(
        """^You (?:dug out a Griffin Burrow! \(\d+/\d+\)|finished the Griffin burrow chain! \(\d+/\d+\))$"""
    )

    private fun gaussianDelay(min: Int, max: Int): Int {
        val mean = (min + max) / 2.0
        val stdDev = (max - min) / 6.0
        val raw = (javaRandom.nextGaussian() * stdDev + mean).toInt()
        return raw.coerceIn(min, max)
    }

    private fun getDelayMs(): Int = if (randomDelay) {
        val min = minRandomDelay.coerceAtMost(maxRandomDelay)
        val max = maxRandomDelay.coerceAtLeast(minRandomDelay)
        gaussianDelay(min, max)
    } else {
        warpDelay
    }

    private fun getSkyHanniCurrentWarpName(): String? {
        val helperClass = try {
            Class.forName("at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper")
        } catch (e: ClassNotFoundException) {
            return null
        }
        val instance = try {
            helperClass.getField("INSTANCE").get(null)
        } catch (e: Exception) {
            return null
        }
        val getter = try {
            helperClass.getMethod("getCurrentWarp")
        } catch (e: Exception) {
            return null
        }
        val warpPoint = getter.invoke(instance) ?: return null
        return (warpPoint.javaClass.getMethod("name").invoke(warpPoint) as String).lowercase()
    }

    init {
        on<ChatPacketEvent> {
            if (LocationUtils.currentArea != Island.Hub) return@on
            if (!burrowRegex.matches(value)) return@on

            var attempts = 0
            fun tryWarp() {
                attempts++
                val warpName = getSkyHanniCurrentWarpName()
                if (warpName != null) {
                    val delayMs = getDelayMs()
                    val delayTicks = ((delayMs / 1000.0) * 20).toInt().coerceAtLeast(1)
                    schedule(delayTicks) {
                        mc.player?.connection?.sendCommand("warp $warpName")
                    }
                } else if (attempts < 5) {
                    schedule(2) { tryWarp() }
                }
            }

            schedule(2) { tryWarp() }
        }
    }
}