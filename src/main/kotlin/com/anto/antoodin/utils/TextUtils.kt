package com.anto.antoodin.utils

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.util.Optional

// §0 to §f in order
private val legacyColors = listOf(
    TextColor.BLACK, TextColor.DARK_BLUE, TextColor.DARK_GREEN, TextColor.DARK_AQUA, TextColor.DARK_RED, TextColor.DARK_PURPLE,
    TextColor.GOLD, TextColor.GRAY, TextColor.DARK_GRAY, TextColor.BLUE, TextColor.GREEN, TextColor.AQUA, TextColor.RED,
    TextColor.LIGHT_PURPLE, TextColor.YELLOW, TextColor.WHITE
)

/** Rebuilds the § color codes from component styles, since Hypixel's names arrive as styled components. */
fun Component.legacyText(): String = buildString {
    visit({ style, text ->
        style.color?.let { color ->
            val code = legacyColors.indexOfFirst { it.value == color.value }
            if (code >= 0) append('§').append("0123456789abcdef"[code])
        }
        append(text)
        Optional.empty<Unit>()
    }, Style.EMPTY)
}
