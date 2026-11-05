package com.anto.antoodin.utils

import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.OdinMod.mc
//? if >= 26.3 {
/*import com.odtheking.odin.clickgui.settings.impl.KeybindSetting.Companion.isDown
*///?} else {
import org.lwjgl.glfw.GLFW
//?}

/** Whether a keyboard or mouse bind is held. False for an unbound key (Odin's isDown throws on it). */
fun InputConstants.Key.isHeld(): Boolean {
    if (this == InputConstants.UNKNOWN) return false
    //? if >= 26.3 {
    /*return isDown()
    *///?} else {
    // Odin 0.3.6's isDown only reads the keyboard, so mouse binds never count as held
    return if (type == InputConstants.Type.MOUSE) GLFW.glfwGetMouseButton(mc.window.handle(), value) == GLFW.GLFW_PRESS
    else InputConstants.isKeyDown(mc.window, value)
    //?}
}
