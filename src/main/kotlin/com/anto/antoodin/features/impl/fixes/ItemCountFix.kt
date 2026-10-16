// Feature idea from NoFrills (GPL-3.0). Written from scratch for AntoOdin, no NoFrills code is used.
package com.anto.antoodin.features.impl.fixes

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module

/**
 * Menu containers (SimpleContainer) clamp every stack to its max stack size, so a sword with a count of 5 in a
 * Hypixel menu shows no number. Skipping the clamp keeps the server's count.
 *
 * @see com.anto.antoodin.mixin.mixins.ItemStackMixin
 */
object ItemCountFix : Module(
    name = "Item Count Fix",
    description = "Shows item counts on unstackable items in menus.",
    category = Skit.FIXES,
    toggled = true
) {
    // The integrated server shares this code, so singleplayer keeps vanilla stack limits
    @JvmStatic
    fun skipLimit(): Boolean = enabled && !mc.hasSingleplayerServer()
}
