package com.anto.antoodin.utils

import com.odtheking.odin.features.Category

object Skit {
    @JvmField
    val ANTO = Category.custom("Anto", 860, 10)

    // Features ported from NoammAddons
    @JvmField
    val NOAMM = Category.custom("Noamm", 1030, 10)

    // Features ported from skies-starred's OdinClient, which is no longer maintained
    @JvmField
    val ODINCLIENT = Category.custom("OdinClient", 1200, 10)

    // Fixes for vanilla or Hypixel bugs
    @JvmField
    val FIXES = Category.custom("Fixes", 1370, 10)
}
