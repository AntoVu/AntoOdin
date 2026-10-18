package com.anto.antoodin.utils

import java.awt.Font
import java.awt.GraphicsEnvironment

/** Installed font families, named without spaces like Devonian does (`Comic Sans MS` -> `ComicSansMS`). */
object SystemFonts {
    private val families: Map<String, String> by lazy {
        GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.associateBy { it.replace(" ", "").lowercase() }
    }

    val names: List<String> get() = families.values.map { it.replace(" ", "") }

    /** The font for [name], or null for `Minecraft` and unknown names (vanilla font). */
    fun find(name: String): Font? = families[name.replace(" ", "").lowercase()]?.let { Font(it, Font.PLAIN, 1) }
}
