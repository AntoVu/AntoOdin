// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import net.minecraft.client.KeyMapping
import net.minecraft.world.entity.player.Input

/**
 * @see com.anto.antoodin.mixin.mixins.KeyboardInputMixin
 */
object SnappyTappy : Module(
    name = "Snappy Tappy",
    description = "Opposing movement keys don't cancel out: the most recently pressed one wins.",
    category = Skit.NOAMM
) {
    private val pressTimes = HashMap<KeyMapping, Long>()

    @JvmStatic
    fun resolveInput(input: Input): Input {
        if (!enabled || mc.gui.screen() != null) {
            pressTimes.clear()
            return input
        }

        val options = mc.options
        listOf(options.keyUp, options.keyDown, options.keyLeft, options.keyRight).forEach { key ->
            if (key.isDown) pressTimes.putIfAbsent(key, System.nanoTime()) else pressTimes.remove(key)
        }

        var forward = input.forward()
        var backward = input.backward()
        var left = input.left()
        var right = input.right()

        if (forward && backward) if (isNewer(options.keyUp, options.keyDown)) backward = false else forward = false
        if (left && right) if (isNewer(options.keyLeft, options.keyRight)) right = false else left = false

        return Input(forward, backward, left, right, input.jump(), input.shift(), input.sprint())
    }

    private fun isNewer(a: KeyMapping, b: KeyMapping) = (pressTimes[a] ?: 0L) >= (pressTimes[b] ?: 0L)
}
