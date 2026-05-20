package com.anto.antoodin.features.impl.anto

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.anto.antoodin.utils.Skit
import kotlin.random.Random

object DianaAutoWarp : Module(
    name = "Diana Auto Warp",
    description = "Automatically warps to next location after digging burrow. Requires SkyHanni.",
    category = Skit.ANTO
) {
    private val warpDelay by NumberSetting(
        "Warp Delay",
        100,
        0,
        500,
        10,
        desc = "Delay in milliseconds before sending the warp command."
    )
    private val delayVariety by NumberSetting(
        "Delay Variety",
        70,
        0,
        250,
        10,
        desc = "Random extra delay in milliseconds added on top of Warp Delay."
    )

    private val burrowRegex = Regex(
        """^You (?:dug out a Griffin Burrow! \(\d+/\d+\)|finished the Griffin burrow chain! \(\d+/\d+\))$"""
    )

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
                    val finalDelay = warpDelay.toLong() + Random.nextLong(0, delayVariety.toLong() + 1)
                    val delayTicks = ((finalDelay / 1000.0) * 20).toInt().coerceAtLeast(1)
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