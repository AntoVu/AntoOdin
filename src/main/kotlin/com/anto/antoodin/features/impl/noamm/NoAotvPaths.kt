// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.world.item.ItemStack

/**
 * The AOTV is a diamond shovel, so right-clicking grass or dirt runs vanilla's path code on the client:
 * the shovel sound on 26.2, plus a ghost path block on 26.3 (26.2 only places it server side).
 * The click still returns SUCCESS like vanilla, so the packets sent are unchanged.
 *
 * @see com.anto.antoodin.mixin.mixins.AotvPathMixin
 */
object NoAotvPaths : Module(
    name = "No AOTV Paths",
    description = "Stops the Aspect of the Void from making ghost paths and playing the shovel sound.",
    category = Skit.NOAMM
) {
    @JvmStatic
    fun shouldPrevent(stack: ItemStack): Boolean =
        enabled && LocationUtils.isInSkyblock && stack.itemId == "ASPECT_OF_THE_VOID"
}
