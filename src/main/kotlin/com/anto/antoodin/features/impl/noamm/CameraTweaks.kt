// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.features.Module
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player

/**
 * @see com.anto.antoodin.mixin.mixins.CameraMixin
 * @see com.anto.antoodin.mixin.mixins.AbstractClientPlayerMixin
 * @see com.anto.antoodin.mixin.mixins.BlindnessFogEnvironmentMixin
 * @see com.anto.antoodin.mixin.mixins.GameRendererMixin
 * @see com.anto.antoodin.mixin.mixins.GuiMixin
 */
object CameraTweaks : Module(
    name = "Camera Tweaks",
    description = "Custom FOV (beyond vanilla's slider), Slowness FOV strength, and no Blindness or Nausea.",
    category = Skit.NOAMM
) {
    private val customFov by BooleanSetting("Custom FOV", false, desc = "Overrides your FOV setting.")
    private val fov by NumberSetting("FOV", 110, 30, 179, 1, desc = "FOV to use instead of the vanilla setting.").withDependency { customFov }
    private val slownessFov by NumberSetting("Slowness FOV", 100, 0, 100, 5, desc = "How much Slowness narrows your FOV. 100% is vanilla, 0% ignores it. Speed and sprinting are unaffected.", unit = "%")
    private val noBlindness by BooleanSetting("No Blindness", false, desc = "Removes the Blindness fog.")
    private val noNausea by BooleanSetting("No Nausea", false, desc = "Removes the Nausea wobble and overlay.")

    private val slownessModifier = Identifier.withDefaultNamespace("effect.slowness")

    // Scales the final FOV, so zoom mods that lower the vanilla FOV still zoom
    @JvmStatic
    fun fovRatio(): Float = if (enabled && customFov) fov.toFloat() / mc.options.fov().get() else 1f

    /** Movement speed used for the FOV, with the Slowness effect's share scaled by the slider. */
    @JvmStatic
    fun fovMovementSpeed(player: Player, speed: Double): Double {
        if (!enabled || slownessFov >= 100) return speed
        val amount = player.getAttribute(Attributes.MOVEMENT_SPEED)?.getModifier(slownessModifier)?.amount() ?: return speed
        // The modifier multiplies the total by (1 + amount), so divide it back out; at -100% the speed is gone entirely
        val withoutSlowness = if (1 + amount > 0.01) speed / (1 + amount) else player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)
        return withoutSlowness + (speed - withoutSlowness) * (slownessFov / 100.0)
    }

    @JvmStatic
    fun shouldHideBlindness(): Boolean = enabled && noBlindness

    @JvmStatic
    fun shouldHideNausea(): Boolean = enabled && noNausea
}
