package com.anto.antoodin.features.impl.anto

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import com.anto.antoodin.utils.Skit
import org.lwjgl.glfw.GLFW

object DropGuard : Module(
    name = "Drop Guard",
    description = "Prevents dropping items outside of GUIs during the F7/M7 terminals phase. Hold the override key to drop.",
    category = Skit.ANTO
) {
    private val overrideKey by KeybindSetting("Override Key", GLFW.GLFW_KEY_LEFT_ALT, desc = "Hold this key to allow dropping items.")

    // Terminals phase boundaries, same messages Odin's TickTimers/TerminalTimes use
    private val terminalsStartRegex = Regex("^\\[BOSS] Goldor: Who dares trespass into my domain\\?$")
    private val terminalsEndRegex = Regex("^The Core entrance is opening!$")

    private var inTerminals = false

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
    }

    override fun onDisable() {
        inTerminals = false
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
