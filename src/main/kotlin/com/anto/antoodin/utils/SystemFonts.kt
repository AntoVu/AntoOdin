package com.anto.antoodin.utils

import java.io.File

/**
 * Installed TrueType fonts, keyed by family name without spaces (e.g. `ComicSansMS` -> comic.ttf).
 * `.otf` is skipped because Minecraft's TTF loader rejects non-TrueType fonts.
 */
object SystemFonts {
    private val dirs = listOfNotNull(
        System.getenv("WINDIR")?.let { "$it/Fonts" },
        System.getenv("LOCALAPPDATA")?.let { "$it/Microsoft/Windows/Fonts" },
        "/usr/share/fonts", "/usr/local/share/fonts", "${System.getProperty("user.home")}/.local/share/fonts",
        "/Library/Fonts", "/System/Library/Fonts", "${System.getProperty("user.home")}/Library/Fonts"
    )

    private val byName: Map<String, File> by lazy {
        val map = sortedMapOf<String, File>(String.CASE_INSENSITIVE_ORDER)
        for (dir in dirs) File(dir).walk().filter { it.isFile && it.extension.lowercase() in setOf("ttf", "ttc") }.forEach { file ->
            // Family names come from the font's name table; files AWT can't read are still reachable by file name
            val family = runCatching { java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, file).family }.getOrNull()
            map.putIfAbsent((family ?: file.nameWithoutExtension).replace(" ", ""), file)
        }
        map
    }

    val names: Collection<String> get() = byName.keys

    /** The font file for [name] (family or file name), or null for the vanilla font. */
    fun find(name: String): File? {
        val key = name.replace(" ", "")
        if (key.isEmpty() || key.equals("Minecraft", true)) return null
        return byName[key] ?: byName.values.firstOrNull { it.nameWithoutExtension.equals(key, true) }
    }
}
