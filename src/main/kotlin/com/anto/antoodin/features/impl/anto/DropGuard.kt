package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.mixin.accessors.AbstractContainerScreenAccessor
import com.anto.antoodin.mixin.accessors.KeyMappingAccessor
import com.anto.antoodin.utils.guiClick
import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import com.anto.antoodin.utils.Skit
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ContainerInput
import org.lwjgl.glfw.GLFW

object DropGuard : Module(
    name = "Drop Guard",
    description = "Prevents dropping items outside of GUIs during the F7/M7 terminals phase, and can skip the key repeat delay when dropping in GUIs.",
    category = Skit.ANTO
) {
    private val overrideKey by KeybindSetting("Override Key", GLFW.GLFW_KEY_LEFT_ALT, desc = "Hold this key to allow dropping items.")

    // Terminals phase boundaries, same messages Odin's TickTimers/TerminalTimes use
    private val terminalsStartRegex = Regex("^\\[BOSS] Goldor: Who dares trespass into my domain\\?$")
    private val terminalsEndRegex = Regex("^The Core entrance is opening!$")

    private var inTerminals = false

    private val fastGuiDrop by BooleanSetting("Fast GUI Drop", false, desc = "Holding the drop key over an item in a GUI throws at full rate right away, without the key repeat delay.")
    private val dropInterval by NumberSetting("Drop Interval", 33, 20..200, 1, desc = "Time between throws while the drop key is held.", unit = "ms")
        .withDependency { fastGuiDrop }

    // Screen the drop key was pressed in; throws stop once it closes or the key is released
    private var dropScreen: AbstractContainerScreen<*>? = null
    private var lastThrow = 0L

    init {
        on<MessageEvent.Chat> {
            when {
                terminalsStartRegex.matches(message) -> inTerminals = true
                terminalsEndRegex.matches(message) -> inTerminals = false
            }
        }

        on<LevelEvent.Load> {
            inTerminals = false
        }

        // Cancels the press and the OS key repeats, and throws on a timer instead
        on<ScreenEvent.KeyPress> {
            val screen = screen as? AbstractContainerScreen<*> ?: return@on
            if (!fastGuiDrop || !mc.options.keyDrop.matches(input)) return@on
            cancel()
            if (dropScreen == null) {
                dropScreen = screen
                throwHovered(screen)
            }
        }

        on<ScreenEvent.Render> {
            val held = dropScreen ?: return@on
            if (screen !== held || !(mc.options.keyDrop as KeyMappingAccessor).boundKey.isHeld()) {
                dropScreen = null
                return@on
            }
            if (System.currentTimeMillis() - lastThrow >= dropInterval) throwHovered(held)
        }
    }

    // Same click vanilla sends for the drop key; Ctrl throws the whole stack
    private fun throwHovered(screen: AbstractContainerScreen<*>) {
        lastThrow = System.currentTimeMillis()
        val slot = (screen as AbstractContainerScreenAccessor).hoveredSlot?.takeIf { it.hasItem() } ?: return
        guiClick(screen.menu.containerId, slot.index, if (mc.hasControlDown()) 1 else 0, ContainerInput.THROW)
    }

    override fun onDisable() {
        inTerminals = false
        dropScreen = null
        super.onDisable()
    }

    fun shouldBlockDrop(): Boolean {
        if (!enabled || !inTerminals) return false
        if (mc.gui.screen() != null) return false
        // Unknown means not in the F7/M7 boss, so a stale flag can never block elsewhere
        if (DungeonUtils.getF7Phase() == M7Phases.Unknown) return false
        return !overrideKey.isHeld()
    }

    private fun InputConstants.Key.isHeld(): Boolean = when {
        this == InputConstants.UNKNOWN -> false
        type == InputConstants.Type.MOUSE -> GLFW.glfwGetMouseButton(mc.window.handle(), value) == GLFW.GLFW_PRESS
        else -> InputConstants.isKeyDown(mc.window, value)
    }
}
