package com.anto.antoodin.features.impl.anto

import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.anto.antoodin.utils.Skit
import net.minecraft.world.item.Items
import kotlin.random.Random

object PearlRefill : Module(
    name = "Pearl Refill",
    description = "Automatically refill ender pearls when falling below set amount.",
    category = Skit.ANTO
) {
    private val refillTriggerAmount by NumberSetting(
        "Trigger",
        5,
        1,
        15,
        1,
        desc = "What value to refill at."
    )

    private var lastTriggerTime = 0L

    fun onRightClick() {
        if (!enabled) return

        val player = mc.player ?: return
        val stack = player.mainHandItem ?: return

        if (stack.item != Items.ENDER_PEARL) return

        val currentAmount = stack.count

        if (currentAmount <= refillTriggerAmount) {
            val needed = 16 - currentAmount + 1
            if (needed <= 0) return

            val now = System.currentTimeMillis()
            if (now - lastTriggerTime < 150) return
            lastTriggerTime = now

            val delayTicks = Random.nextInt(1, 5)
            schedule(delayTicks) {
                mc.player?.connection?.sendCommand("gfs ENDER_PEARL $needed")
            }
        }
    }
}