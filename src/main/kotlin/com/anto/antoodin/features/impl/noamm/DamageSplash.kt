// Ported from NoammAddons by Noamm9 (CC0-1.0). Style modeled on NEU's damage indicator (own code, NEU is LGPL)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.Optional
import kotlin.math.floor

/** Hiding damage splashes is left to SkyHanni. */
object DamageSplash : Module(
    name = "Damage Splash",
    description = "Shortens damage numbers (1,234,567 becomes 1.2m) and keeps Hypixel's crit colors.",
    category = Skit.NOAMM
) {
    private val uppercase by BooleanSetting("Uppercase", false, desc = "Writes the suffix in uppercase, e.g. 1.2M.")

    // Hypixel's own crit gradient, repeated over the characters
    private val critColors = listOf("§f", "§e", "§6", "§c", "§c", "§f")
    private const val SUFFIXES = "kmbt"

    // Legacy-formatted names: ✧ crits, ✯ overload crits, and plain colored numbers
    private val critRegex = Regex("^§f✧((?:§.|[\\d,])+)§.✧(.*)$")
    private val overloadRegex = Regex("^(§.)✯((?:§.|[\\d,])+)(§.)✯(.*)$")
    private val normalRegex = Regex("^(§.)([\\d,]+)(.*)$")
    private val formattingCode = Regex("§.")

    init {
        // Posted after the data is applied, so the server's name is already set and can be replaced
        on<EntityEvent.SetData> {
            val stand = entity as? ArmorStand ?: return@on
            if (!LocationUtils.isInSkyblock || synchedDataValues.none { it.value() is Optional<*> }) return@on
            val name = stand.customName ?: return@on
            reformat(legacy(name))?.let { stand.customName = Component.literal(it) }
        }
    }

    private fun reformat(text: String): String? {
        critRegex.matchEntire(text)?.let { match ->
            val short = shorten(match.groupValues[1]) ?: return null
            return "§f✧${critColored(short)}§f✧${match.groupValues[2]}"
        }
        overloadRegex.matchEntire(text)?.let { match ->
            val short = shorten(match.groupValues[2]) ?: return null
            return "${match.groupValues[1]}✯${critColored(short)}${match.groupValues[3]}✯${match.groupValues[4]}"
        }
        normalRegex.matchEntire(text)?.let { match ->
            val short = shorten(match.groupValues[2]) ?: return null
            return "${match.groupValues[1]}$short§r${match.groupValues[3]}"
        }
        return null
    }

    // Small hits are left as they are
    private fun shorten(digits: String): String? {
        val value = digits.replace(formattingCode, "").replace(",", "").toLongOrNull() ?: return null
        if (value <= 999) return null
        return shortFormat(value).let { if (uppercase) it.uppercase() else it }
    }

    // One decimal below 10, none above: 1234 -> 1.2k, 12345 -> 12k, 1234567 -> 1.2m
    private fun shortFormat(value: Long): String {
        var number = value.toDouble()
        var index = 0
        while (true) {
            val truncated = floor(number / 100) / 10
            if (truncated < 1000 || index == SUFFIXES.lastIndex) {
                val text = if (truncated % 1 == 0.0 || truncated > 9.99) truncated.toLong().toString() else truncated.toString()
                return text + SUFFIXES[index]
            }
            number = truncated
            index++
        }
    }

    // §0 to §f in order
    private val legacyColors = listOf(
        TextColor.BLACK, TextColor.DARK_BLUE, TextColor.DARK_GREEN, TextColor.DARK_AQUA, TextColor.DARK_RED, TextColor.DARK_PURPLE,
        TextColor.GOLD, TextColor.GRAY, TextColor.DARK_GRAY, TextColor.BLUE, TextColor.GREEN, TextColor.AQUA, TextColor.RED,
        TextColor.LIGHT_PURPLE, TextColor.YELLOW, TextColor.WHITE
    )

    private fun critColored(text: String) = text.withIndex().joinToString("") { (i, char) -> critColors[i % critColors.size] + char }

    // Rebuilds the § codes from component styles, since Hypixel's names arrive as styled components
    private fun legacy(component: Component): String = buildString {
        component.visit({ style, text ->
            style.color?.let { color ->
                val code = legacyColors.indexOfFirst { it.value == color.value }
                if (code >= 0) append('§').append("0123456789abcdef"[code])
            }
            append(text)
            Optional.empty<Unit>()
        }, Style.EMPTY)
    }
}
