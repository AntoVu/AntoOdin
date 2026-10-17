package com.anto.antoodin.features.impl.anto

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.loreString
import com.odtheking.odin.utils.modMessage
import com.anto.antoodin.utils.Skit
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.random.Random

object QueueLoadout : Module(
    name = "Queue Loadout",
    description = "Queues a loadout to be equipped when the loadout menu opens.",
    category = Skit.ANTO
) {
    val showChatMessage by BooleanSetting(
        "Show Chat Message",
        true,
        desc = "Shows a chat message when a loadout is queued."
    )
    val delay by NumberSetting(
        "Delay",
        100,
        0..500,
        10,
        desc = "Delay in milliseconds before equipping the queued loadout."
    )
    val delayVariety by NumberSetting(
        "Delay Variety",
        70,
        0..250,
        10,
        desc = "Random extra delay added on top of Delay."
    )

    private val advanced by DropdownSetting("Show Settings", desc = "Shows the per-loadout keybinds.")
    private val loadoutBinds = (1..12).map { n ->
        +KeybindSetting("Loadout $n", InputConstants.UNKNOWN, desc = "Keybind to queue loadout $n.").withDependency { advanced }
    }

    // Dropped if the menu never opens (e.g. /loadouts is blocked here)
    private const val QUEUE_TIMEOUT_MS = 5000L
    private var queuedSlot: Int? = null // container slot index
    private var queuedNumber = 0
    private var queuedAt = 0L
    private var clickScheduled = false

    private fun loadoutOpen(): AbstractContainerScreen<*>? =
        (mc.gui.screen() as? AbstractContainerScreen<*>)?.takeIf { LoadoutAddon.loadoutRegex.containsMatchIn(it.title.string) }

    init {
        on<InputEvent> {
            val keyIndex = loadoutBinds.indexOfFirst { it.value == key }.takeIf { it != -1 } ?: return@on

            queuedSlot = LoadoutAddon.loadoutSlot(keyIndex)
            queuedNumber = keyIndex + 1
            queuedAt = System.currentTimeMillis()
            clickScheduled = false
            if (showChatMessage) modMessage("§aQueued loadout $queuedNumber.")
            mc.player?.connection?.sendCommand("loadouts")
        }

        // Slot items arrive after the screen opens, so wait until the queued slot has loaded
        on<TickEvent.End> {
            val qSlot = queuedSlot ?: return@on
            if (System.currentTimeMillis() - queuedAt > QUEUE_TIMEOUT_MS) {
                queuedSlot = null
                return@on
            }
            if (clickScheduled) return@on

            val screen = loadoutOpen() ?: return@on
            val item = screen.menu.slots.getOrNull(qSlot)?.item ?: return@on
            if (item.isEmpty) return@on

            // Placeholder and already-equipped loadouts have no equip prompt
            if (item.loreString.none { "Left-click to equip!" in it }) {
                queuedSlot = null
                modMessage("§cLoadout $queuedNumber is already equipped or empty.")
                mc.player?.closeContainer()
                return@on
            }

            clickScheduled = true
            val finalDelay = delay.toLong() + Random.nextLong(0, delayVariety.toLong() + 1)
            val delayTicks = ((finalDelay / 1000.0) * 20).toInt().coerceAtLeast(1)

            schedule(delayTicks) {
                if (queuedSlot != qSlot) return@schedule
                queuedSlot = null
                loadoutOpen() ?: return@schedule
                mc.player?.clickSlot(qSlot)
                // Close after Hypixel's menu refresh, otherwise the refresh reopens the menu
                schedule(delayTicks) { if (loadoutOpen() != null) mc.player?.closeContainer() }
            }
        }
    }
}
