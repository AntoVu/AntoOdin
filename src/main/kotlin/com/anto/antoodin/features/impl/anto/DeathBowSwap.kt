package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.Floor
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import kotlin.random.Random

object DeathBowSwap : Module(
    name = "Death Bow Swap",
    description = "In M7 Storm as Archer, swaps hotbar slot and queues a loadout after you shoot your Death Bow.",
    category = Skit.ANTO
) {
    private val hotbarSlot by NumberSetting("Hotbar Slot", 1, 1..9, 1, desc = "Hotbar slot to swap to after the Death Bow shot.")
    private val swapDelay by NumberSetting("Swap Delay", 150, 0..1000, 10, desc = "Delay in milliseconds after the shot before swapping.", unit = "ms")
    private val swapDelayVariety by NumberSetting("Swap Delay Variety", 70, 0..500, 10, desc = "Random extra delay in milliseconds added on top of Swap Delay.", unit = "ms")
    private val loadout by NumberSetting("Loadout", 1, 1..12, 1, desc = "Loadout to queue after the swap. Needs Queue Loadout enabled.")
    private val loadoutDelay by NumberSetting("Loadout Delay", 150, 0..1000, 10, desc = "Delay in milliseconds after the swap before queuing the loadout.", unit = "ms")
    private val loadoutDelayVariety by NumberSetting("Loadout Delay Variety", 70, 0..500, 10, desc = "Random extra delay in milliseconds added on top of Loadout Delay.", unit = "ms")

    private const val DEATH_BOW = "DEATH_BOW"
    // A bow released before 3 ticks of draw doesn't fire an arrow
    private const val MIN_DRAW_TICKS = 3

    // Runs once per world, so later shots in the same Storm phase don't swap again
    private var triggered = false

    init {
        // The release packet is what fires a charged bow
        onSend<ServerboundPlayerActionPacket> {
            if (triggered || action != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) return@onSend
            if (!inStorm() || DungeonUtils.currentDungeonPlayer.clazz != DungeonClass.ARCHER) return@onSend
            val player = mc.player ?: return@onSend
            if (player.mainHandItem.itemId != DEATH_BOW) return@onSend
            if (player.ticksUsingItem < MIN_DRAW_TICKS) return@onSend

            triggered = true
            schedule(delayTicks(swapDelay, swapDelayVariety)) {
                if (!inStorm()) return@schedule
                mc.player?.inventory?.selectedSlot = hotbarSlot - 1

                schedule(delayTicks(loadoutDelay, loadoutDelayVariety)) {
                    if (!inStorm()) return@schedule
                    if (!QueueLoadout.enabled) return@schedule modMessage("§cDeath Bow Swap needs Queue Loadout enabled to equip loadout $loadout.")
                    QueueLoadout.queue(loadout)
                }
            }
        }

        on<LevelEvent.Load> { triggered = false }
    }

    // getF7Phase alone also matches F7 and singleplayer, so check M7 boss explicitly
    private fun inStorm() =
        DungeonUtils.inBoss && DungeonUtils.floor == Floor.M7 && DungeonUtils.getF7Phase() == M7Phases.P2

    private fun delayTicks(delay: Int, variety: Int): Int {
        val ms = delay.toLong() + Random.nextLong(0, variety.toLong() + 1)
        return ((ms / 1000.0) * 20).toInt().coerceAtLeast(1)
    }
}
