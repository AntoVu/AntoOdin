package com.anto.antoodin.features.impl.anto

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.createSoundSettings
import com.odtheking.odin.utils.playSoundSettings
import com.anto.antoodin.utils.Skit
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.lwjgl.glfw.GLFW
import kotlin.random.Random

object LoadoutAddon : Module(
    name = "Loadout Addon (A)",
    description = "Better loadout hotkeys, use this instead of default ones",
    category = Skit.ANTO
) {
    private val nextPageKeybind by KeybindSetting("Next Page", GLFW.GLFW_KEY_RIGHT, desc = "Keybind to go to the next page in loadouts.")
    private val previousPageKeybind by KeybindSetting("Previous Page", GLFW.GLFW_KEY_LEFT, desc = "Keybind to go to the previous page in loadouts.")

    private val advanced by DropdownSetting("Show Settings")
    private val loadout1 by KeybindSetting("Loadout 1", GLFW.GLFW_KEY_1, desc = "Keybind to equip the first loadout slot.").withDependency { advanced }
    private val loadout2 by KeybindSetting("Loadout 2", GLFW.GLFW_KEY_2, desc = "Keybind to equip the second loadout slot.").withDependency { advanced }
    private val loadout3 by KeybindSetting("Loadout 3", GLFW.GLFW_KEY_3, desc = "Keybind to equip the third loadout slot.").withDependency { advanced }
    private val loadout4 by KeybindSetting("Loadout 4", GLFW.GLFW_KEY_4, desc = "Keybind to equip the fourth loadout slot.").withDependency { advanced }
    private val loadout5 by KeybindSetting("Loadout 5", GLFW.GLFW_KEY_5, desc = "Keybind to equip the fifth loadout slot.").withDependency { advanced }
    private val loadout6 by KeybindSetting("Loadout 6", GLFW.GLFW_KEY_6, desc = "Keybind to equip the sixth loadout slot.").withDependency { advanced }
    private val loadout7 by KeybindSetting("Loadout 7", GLFW.GLFW_KEY_7, desc = "Keybind to equip the seventh loadout slot.").withDependency { advanced }
    private val loadout8 by KeybindSetting("Loadout 8", GLFW.GLFW_KEY_8, desc = "Keybind to equip the eighth loadout slot.").withDependency { advanced }
    private val loadout9 by KeybindSetting("Loadout 9", GLFW.GLFW_KEY_9, desc = "Keybind to equip the ninth loadout slot.").withDependency { advanced }
    private val loadout10 by KeybindSetting("Loadout 10", GLFW.GLFW_KEY_0, desc = "Keybind to equip the tenth loadout slot.").withDependency { advanced }
    private val loadout11 by KeybindSetting("Loadout 11", GLFW.GLFW_KEY_R, desc = "Keybind to equip the eleventh loadout slot.").withDependency { advanced }
    private val loadout12 by KeybindSetting("Loadout 12", GLFW.GLFW_KEY_F, desc = "Keybind to equip the twelfth loadout slot.").withDependency { advanced }

    private val autoCloseDropdown by DropdownSetting("Auto Close")
    private val autoCloseToggle by BooleanSetting("Auto Close Toggle", false, desc = "Automatically closes the loadout menu after selecting a slot.").withDependency { autoCloseDropdown }
    private val autoCloseDelay by NumberSetting("Auto Close Delay", 100, 0, 500, 10, desc = "Delay in milliseconds before closing the loadout menu.").withDependency { autoCloseDropdown }
    private val delayVariety by NumberSetting("Delay Variety", 70, 0, 250, 10, desc = "Random extra delay in milliseconds added on top of Auto Close Delay.").withDependency { autoCloseDropdown }

    private val equipSoundDropdown by DropdownSetting("Equip Sounds")
    private val equipSoundToggle by BooleanSetting("Enable Equip Sound", false, desc = "Plays a sound when you equip a loadout slot.").withDependency { equipSoundDropdown }
    private val equipSoundSettings = createSoundSettings("Equip Sound", "entity.horse.armor") { equipSoundToggle && equipSoundDropdown }

    private val loadoutRegex = Regex("\\((\\d)/(\\d)\\) Loadouts")


    init {
        on<ScreenEvent.MouseClick> {
            val s = screen
            if (s is AbstractContainerScreen<*> && onClick(s, click.button())) cancel()
        }

        on<ScreenEvent.KeyPress> {
            val s = screen
            if (s is AbstractContainerScreen<*> && onClick(s, input.key)) cancel()
        }
    }

    private fun onClick(screen: AbstractContainerScreen<*>, keyCode: Int): Boolean {
        val (current, total) = loadoutRegex.find(screen.title?.string ?: "")?.destructured?.let {
            it.component1().toIntOrNull() to it.component2().toIntOrNull()
        } ?: return false
        if (current == null || total == null) return false

        val loadoutSlots = arrayOf(loadout1, loadout2, loadout3, loadout4, loadout5, loadout6, loadout7, loadout8, loadout9, loadout10, loadout11, loadout12)
        val isLoadoutSlotKey = loadoutSlots.any { it.value == keyCode }

        val index = when (keyCode) {
            nextPageKeybind.value -> if (current < total) 44 else return false
            previousPageKeybind.value -> if (current > 1) 17 else return false
            else -> {
                val keyIndex = loadoutSlots
                    .indexOfFirst { it.value == keyCode }.takeIf { it != -1 } ?: return false

                keyIndex + 14 + 6 * (keyIndex / 3)
            }
        }

        if (screen.menu.slots[index].item?.isEmpty == true) return false
        mc.player?.clickSlot(index)

        if (isLoadoutSlotKey) {
            if (equipSoundToggle) playSoundSettings(equipSoundSettings())

            if (autoCloseToggle) {
                val finalDelay = autoCloseDelay.toLong() + Random.nextLong(0, delayVariety.toLong() + 1)
                val delayTicks = ((finalDelay / 1000.0) * 20).toInt().coerceAtLeast(1)

                schedule(delayTicks) {
                    mc.player?.closeContainer()
                }
            }
        }

        return true
    }
}