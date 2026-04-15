package com.anto.antoodin.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.createSoundSettings
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.playSoundSettings
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.lwjgl.glfw.GLFW
import kotlin.random.Random

object WardrobeAddon : Module(
    name = "Wardrobe Keybinds (A)",
    description = "Better wardrobe hotkeys, use this instead of default ones",
    key = null
) {
    private val nextPageKeybind by KeybindSetting("Next Page", GLFW.GLFW_KEY_RIGHT, desc = "Keybind to go to the next page in the wardrobe.")
    private val previousPageKeybind by KeybindSetting("Previous Page", GLFW.GLFW_KEY_LEFT, desc = "Keybind to go to the previous page in the wardrobe.")
    private val unequipKeybind by KeybindSetting("Unequip", GLFW.GLFW_KEY_UNKNOWN, desc = "Keybind to unequip the currently equipped item in the wardrobe.")
    private val disallowUnequippingEquipped by BooleanSetting("Disable Unequip", desc = "Prevents unequipping equipped armor.")

    private val advanced by DropdownSetting("Show Settings")
    private val wardrobe1 by KeybindSetting("Wardrobe 1", GLFW.GLFW_KEY_1, desc = "Keybind to equip the first wardrobe slot.").withDependency { advanced }
    private val wardrobe2 by KeybindSetting("Wardrobe 2", GLFW.GLFW_KEY_2, desc = "Keybind to equip the second wardrobe slot.").withDependency { advanced }
    private val wardrobe3 by KeybindSetting("Wardrobe 3", GLFW.GLFW_KEY_3, desc = "Keybind to equip the third wardrobe slot.").withDependency { advanced }
    private val wardrobe4 by KeybindSetting("Wardrobe 4", GLFW.GLFW_KEY_4, desc = "Keybind to equip the fourth wardrobe slot.").withDependency { advanced }
    private val wardrobe5 by KeybindSetting("Wardrobe 5", GLFW.GLFW_KEY_5, desc = "Keybind to equip the fifth wardrobe slot.").withDependency { advanced }
    private val wardrobe6 by KeybindSetting("Wardrobe 6", GLFW.GLFW_KEY_6, desc = "Keybind to equip the sixth wardrobe slot.").withDependency { advanced }
    private val wardrobe7 by KeybindSetting("Wardrobe 7", GLFW.GLFW_KEY_7, desc = "Keybind to equip the seventh wardrobe slot.").withDependency { advanced }
    private val wardrobe8 by KeybindSetting("Wardrobe 8", GLFW.GLFW_KEY_8, desc = "Keybind to equip the eighth wardrobe slot.").withDependency { advanced }
    private val wardrobe9 by KeybindSetting("Wardrobe 9", GLFW.GLFW_KEY_9, desc = "Keybind to equip the ninth wardrobe slot.").withDependency { advanced }

    private val autoCloseDropdown by DropdownSetting("Auto Close")
    private val autoCloseToggle by BooleanSetting("Auto Close Toggle", false, desc = "Automatically closes the wardrobe after selecting a slot.").withDependency { autoCloseDropdown }
    private val autoCloseDelay by NumberSetting("Auto Close Delay", 100, 0, 500, 10, desc = "Delay in milliseconds before closing the wardrobe.").withDependency { autoCloseDropdown }
    private val delayVariety by NumberSetting("Delay Variety", 70, 0, 250, 10, desc = "Random extra delay in milliseconds added on top of Auto Close Delay.").withDependency { autoCloseDropdown }

    private val equipSoundDropdown by DropdownSetting("Equip Sounds")
    private val equipSoundToggle by BooleanSetting("Enable Equip Sound", false, desc = "Plays a sound when you equip a wardrobe slot.").withDependency { equipSoundDropdown }
    private val equipSoundSettings = createSoundSettings("Equip Sound", "entity.horse.armor") { equipSoundToggle && equipSoundDropdown }

    private val wardrobeRegex = Regex("Wardrobe \\((\\d)/(\\d)\\)")
    private val equippedRegex = Regex("Slot (\\d): Equipped")


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
        val (current, total) = wardrobeRegex.find(screen.title?.string ?: "")?.destructured?.let {
            it.component1().toIntOrNull() to it.component2().toIntOrNull()
        } ?: return false
        if (current == null || total == null) return false

        val equippedIndex = screen.menu.slots.find { equippedRegex.matches(it.item.hoverName.string) }?.index

        val wardrobeSlots = arrayOf(wardrobe1, wardrobe2, wardrobe3, wardrobe4, wardrobe5, wardrobe6, wardrobe7, wardrobe8, wardrobe9)
        val isWardrobeSlotKey = wardrobeSlots.any { it.value == keyCode }

        val index = when (keyCode) {
            nextPageKeybind.value -> if (current < total) 53 else return false
            previousPageKeybind.value -> if (current > 1) 45 else return false
            unequipKeybind.value -> equippedIndex ?: return false
            else -> {
                val keyIndex = wardrobeSlots
                    .indexOfFirst { it.value == keyCode }.takeIf { it != -1 } ?: return false

                if (equippedIndex == keyIndex + 36 && disallowUnequippingEquipped) return modMessage("§cArmor already equipped.").let { false }
                keyIndex + 36
            }
        }

        if (disallowUnequippingEquipped && screen.menu.slots[index].item?.isEmpty == true) return false
        mc.player?.clickSlot(screen.menu.containerId, index)

        if (isWardrobeSlotKey) {
            if (equipSoundToggle) playSoundSettings(equipSoundSettings())

            if (autoCloseToggle) {
                val finalDelay = autoCloseDelay.toLong() + Random.nextLong(0, delayVariety.toLong() + 1)
                Thread {
                    Thread.sleep(finalDelay)
                    mc.execute { mc.player?.closeContainer() }
                }.start()
            }
        }

        return true
    }
}