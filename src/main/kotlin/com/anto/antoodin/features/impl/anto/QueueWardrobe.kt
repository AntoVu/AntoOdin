package com.anto.antoodin.features.impl.anto

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.modMessage
import com.anto.antoodin.utils.Skit
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.lwjgl.glfw.GLFW
import kotlin.random.Random

object QueueWardrobe : Module(
    name = "Queue Wardrobe",
    description = "Queues a wardrobe slot to be equipped when the wardrobe opens.",
    category = Skit.ANTO
) {
    val showChatMessage by BooleanSetting(
        "Show Chat Message",
        true,
        desc = "Shows a chat message when a wardrobe slot is queued."
    )
    val delay by NumberSetting(
        "Delay",
        100,
        0,
        500,
        10,
        desc = "Delay in milliseconds before equipping the queued slot."
    )
    val delayVariety by NumberSetting(
        "Delay Variety",
        70,
        0,
        250,
        10,
        desc = "Random extra delay added on top of Delay."
    )

    private val advanced by DropdownSetting("Show Settings")
    private val wardrobe1 by KeybindSetting(
        "Wardrobe 1",
        GLFW.GLFW_KEY_UNKNOWN,
        desc = "Keybind to queue the first wardrobe slot."
    ).withDependency { advanced }
    private val wardrobe2 by KeybindSetting(
        "Wardrobe 2",
        GLFW.GLFW_KEY_UNKNOWN,
        desc = "Keybind to queue the second wardrobe slot."
    ).withDependency { advanced }
    private val wardrobe3 by KeybindSetting(
        "Wardrobe 3",
        GLFW.GLFW_KEY_UNKNOWN,
        desc = "Keybind to queue the third wardrobe slot."
    ).withDependency { advanced }
    private val wardrobe4 by KeybindSetting(
        "Wardrobe 4",
        GLFW.GLFW_KEY_UNKNOWN,
        desc = "Keybind to queue the fourth wardrobe slot."
    ).withDependency { advanced }
    private val wardrobe5 by KeybindSetting(
        "Wardrobe 5",
        GLFW.GLFW_KEY_UNKNOWN,
        desc = "Keybind to queue the fifth wardrobe slot."
    ).withDependency { advanced }
    private val wardrobe6 by KeybindSetting(
        "Wardrobe 6",
        GLFW.GLFW_KEY_UNKNOWN,
        desc = "Keybind to queue the sixth wardrobe slot."
    ).withDependency { advanced }
    private val wardrobe7 by KeybindSetting(
        "Wardrobe 7",
        GLFW.GLFW_KEY_UNKNOWN,
        desc = "Keybind to queue the seventh wardrobe slot."
    ).withDependency { advanced }
    private val wardrobe8 by KeybindSetting(
        "Wardrobe 8",
        GLFW.GLFW_KEY_UNKNOWN,
        desc = "Keybind to queue the eighth wardrobe slot."
    ).withDependency { advanced }
    private val wardrobe9 by KeybindSetting(
        "Wardrobe 9",
        GLFW.GLFW_KEY_UNKNOWN,
        desc = "Keybind to queue the ninth wardrobe slot."
    ).withDependency { advanced }

    private val wardrobeRegex = Regex("Wardrobe \\((\\d)/(\\d)\\)")
    private var queuedSlot: Int? = null // slot index (36-44)

    init {
        on<InputEvent> {
            val wardrobeSlots = arrayOf(wardrobe1, wardrobe2, wardrobe3, wardrobe4, wardrobe5, wardrobe6, wardrobe7, wardrobe8, wardrobe9)
            val keyIndex = wardrobeSlots.indexOfFirst { it.value == key.value }.takeIf { it != -1 } ?: return@on

            queuedSlot = keyIndex + 36
            if (showChatMessage) modMessage("§aQueued wardrobe slot ${keyIndex + 1}.")
            mc.player?.connection?.sendCommand("wardrobe")
        }

        on<ScreenEvent.Open> {
            val qSlot = queuedSlot ?: return@on
            val s = screen as? AbstractContainerScreen<*> ?: return@on
            if (!wardrobeRegex.matches(s.title?.string ?: "")) return@on

            val containerId = s.menu.containerId
            val finalDelay = delay.toLong() + Random.nextLong(0, delayVariety.toLong() + 1)
            val delayTicks = ((finalDelay / 1000.0) * 20).toInt().coerceAtLeast(1)

            schedule(delayTicks) {
                val currentScreen = mc.screen as? AbstractContainerScreen<*> ?: return@schedule
                if (!wardrobeRegex.matches(currentScreen.title?.string ?: "")) return@schedule
                mc.player?.clickSlot(containerId, qSlot)
                queuedSlot = null
                mc.player?.closeContainer()
            }
        }

        on<ScreenEvent.Close> {
            val s = screen as? AbstractContainerScreen<*> ?: return@on
            if (wardrobeRegex.matches(s.title?.string ?: "")) queuedSlot = null
        }
    }
}