package com.anto.antoodin.features.impl.anto

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import com.anto.antoodin.utils.Skit
import org.lwjgl.glfw.GLFW

object DropGuard : Module(
    name = "Drop Guard",
    description = "Prevents dropping items outside of GUIs during the F7/M7 terminal phase (P3). Hold the override key to drop.",
    category = Skit.ANTO
) {
    private val overrideKey by KeybindSetting("Override Key", GLFW.GLFW_KEY_LEFT_ALT, desc = "Hold this key to allow dropping items.")

    fun shouldBlockDrop(): Boolean {
        if (!enabled) return false
        if (mc.screen != null) return false
        if (DungeonUtils.getF7Phase() != M7Phases.P3) return false
        return !overrideKey.isHeld()
    }

    private fun InputConstants.Key.isHeld(): Boolean = when {
        this == InputConstants.UNKNOWN -> false
        type == InputConstants.Type.MOUSE -> GLFW.glfwGetMouseButton(mc.window.handle(), value) == GLFW.GLFW_PRESS
        else -> InputConstants.isKeyDown(mc.window, value)
    }
}
