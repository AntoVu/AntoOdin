package com.anto.antoodin.features.impl.fixes

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module

/**
 * Odin downloads its devs' custom names at startup and then regex-scans every string the font draws for them,
 * whether or not anyone with a custom name is around (about 3.6% of the render thread in a spark profile).
 * This makes Odin's `CustomNameReplacer.isEnabled()` return false, which every caller checks first.
 *
 * @see com.anto.antoodin.mixin.mixins.CustomNameReplacerMixin
 */
object NameReplacerOff : Module(
    name = "Name Replacer Off",
    description = "Turns off Odin's dev custom names, which scan all on-screen text every frame.",
    category = Skit.FIXES
) {
    @JvmStatic
    fun shouldDisable() = enabled
}
