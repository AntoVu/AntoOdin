// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.features.Module

/** @see com.anto.antoodin.mixin.mixins.CameraMixin */
object CameraHelper : Module(
    name = "Camera Helper",
    description = "Various cheat additions to the vanilla camera.",
    category = Skit.ODINCLIENT
) {
    private val cameraClip by BooleanSetting("Camera Clip", false, desc = "Allows the camera to clip through blocks.")
    private val enableDist by BooleanSetting("Custom distance", false, desc = "Allows you to set a custom distance for the camera.")
    private val cameraDist by NumberSetting("Distance", 4f, 3.0..12.0, 0.1, desc = "The distance of the camera from the player.").withDependency { enableDist }

    @JvmStatic
    fun shouldClip(): Boolean = enabled && cameraClip

    /** Third person camera distance, or the vanilla value. */
    @JvmStatic
    fun distance(original: Double): Double = if (enabled && enableDist) cameraDist.toDouble() else original
}
