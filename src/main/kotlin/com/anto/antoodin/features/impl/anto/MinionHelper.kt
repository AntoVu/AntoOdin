package com.anto.antoodin.features.impl.anto

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.loreString
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.anto.antoodin.utils.Skit
import com.anto.antoodin.mixin.accessors.AbstractContainerScreenAccessor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.lwjgl.glfw.GLFW
import kotlin.random.Random

object MinionHelper : Module(
    name = "Minion Helper",
    description = "Keybinds and shortcuts for minions.",
    category = Skit.ANTO
) {
    private val claimKeybind by KeybindSetting(
        "Claim Minion Keybind",
        GLFW.GLFW_KEY_R,
        desc = "Collect all items from the minion then close the GUI."
    )
    private val claimCloseDelay by NumberSetting(
        "Claim Delay",
        300, 250, 500, 10,
        desc = "Delay before closing gui."
    )
    private val autoFuel by BooleanSetting(
        "Faster Fuel",
        false,
        desc = "When you click a fuel item in a minion GUI, automatically place it in the fuel slot."
    )
    private val autoClose by BooleanSetting(
        "Auto Close After Fuel",
        false,
        desc = "Close the minion GUI after fuel has been placed."
    )
    private val fuelDelay by NumberSetting(
        "Fuel Delay",
        300, 250, 500, 10,
        desc = "Base delay (ms) before placing the fuel item."
    )
    private val fuelCloseDelay by NumberSetting(
        "Close Delay",
        50, 0, 200, 10,
        desc = "Extra delay (ms) after fueling before closing the GUI."
    )

    private const val CLAIM_SLOT = 48
    private const val FUEL_SLOT = 19

    private val FUELS = setOf(
        "COAL", "COAL:1", "COAL_BLOCK",
        "ENCHANTED_COAL", "ENCHANTED_CHARCOAL",
        "HAMSTER_WHEEL", "FOUL_FLESH", "ENCHANTED_BREAD",
        "CATALYST", "HYPER_CATALYST",
        "CHEESE_FUEL", "SOLAR_PANEL",
        "ENCHANTED_LAVA_BUCKET", "MAGMA_BUCKET", "PLASMA_BUCKET",
        "EVERBURNING_FLAME", "INFERNO_HYPERGOLIC_CRUDE_GABAGOOL"
    )

    private fun isPrivateIsland() = LocationUtils.currentArea == Island.PrivateIsland

    private fun isMinionGui(screen: AbstractContainerScreen<*>) =
        screen.title.string.contains("Minion", ignoreCase = true)

    private fun hasClaimButton(screen: AbstractContainerScreen<*>) =
        screen.menu.slots.getOrNull(CLAIM_SLOT)?.item?.takeIf { !it.isEmpty }
            ?.loreString?.any { it.contains("Click to collect all items!", ignoreCase = true) } == true

    private fun msToTicks(ms: Long) = ((ms / 1000.0) * 20).toInt().coerceAtLeast(1)

    private var fuelPending = false
    private var claimPending = false

    init {
        on<ScreenEvent.KeyPress> {
            val s = screen as? AbstractContainerScreen<*> ?: return@on
            if (!isPrivateIsland()) return@on
            if (input.key != claimKeybind.value) return@on
            if (!isMinionGui(s) || !hasClaimButton(s)) return@on
            if (claimPending) return@on

            cancel()
            claimPending = true

            mc.player?.clickSlot(s.menu.containerId, CLAIM_SLOT)
            claimPending = false

            schedule(msToTicks(claimCloseDelay.toLong() + Random.nextLong(0, 80))) {
                mc.player?.closeContainer()
            }
        }

        on<GuiEvent.SlotClick> {
            if (!autoFuel) return@on
            val s = screen as? AbstractContainerScreen<*> ?: return@on
            if (!isPrivateIsland()) return@on
            if (!isMinionGui(s) || !hasClaimButton(s)) return@on
            if (fuelPending) return@on

            val hoveredSlot = (s as AbstractContainerScreenAccessor).getHoveredSlot() ?: return@on
            if (hoveredSlot.index == FUEL_SLOT) return@on
            if (hoveredSlot.item.isEmpty || hoveredSlot.item.itemId !in FUELS) return@on

            fuelPending = true

            val delayTicks = msToTicks(fuelDelay.toLong() + Random.nextLong(0, 80))
            schedule(delayTicks) {
                mc.player?.clickSlot(s.menu.containerId, FUEL_SLOT)
                fuelPending = false
                if (autoClose) {
                    schedule(msToTicks(fuelCloseDelay.toLong() + Random.nextLong(0, 60))) {
                        mc.player?.closeContainer()
                    }
                }
            }
        }
    }
}