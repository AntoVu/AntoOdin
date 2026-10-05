// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.Optional

/** Hiding damage splashes is left to SkyHanni. */
object DamageSplash : Module(
    name = "Damage Splash",
    description = "Reformats damage numbers: 1,234,567 becomes 1.2m and crits get random colors.",
    category = Skit.NOAMM
) {
    private val uppercase by BooleanSetting("Uppercase", false, desc = "Writes the suffix in uppercase, e.g. 1.2M.")

    private val damageRegex = Regex("[✧✯]?(\\d{1,3}(?:,\\d{3})*[⚔+✧❤♞☄✷ﬗ✯]*)")
    private val critColors = listOf("§6", "§c", "§e", "§f")
    private val suffixes = listOf(1_000_000_000_000L to 't', 1_000_000_000L to 'b', 1_000_000L to 'm', 1_000L to 'k')

    init {
        // Posted after the data is applied, so the server's name is already set and can be replaced
        on<EntityEvent.SetData> {
            val stand = entity as? ArmorStand ?: return@on
            if (!LocationUtils.isInSkyblock || synchedDataValues.none { it.value() is Optional<*> }) return@on
            val name = stand.customName ?: return@on
            // Damage tags are always colored, plain numbers are something else
            if ('§' !in name.string && !isStyled(name)) return@on

            val text = name.string.noControlCodes
            val damage = damageRegex.matchEntire(text)?.groupValues?.get(1) ?: return@on
            val formatted = format(damage.filter { it.isDigit() }.toLongOrNull() ?: return@on)
                .let { if (uppercase) it.uppercase() else it }

            val isCrit = '✧' in text || '✯' in text
            stand.customName = Component.literal(if (isCrit) "§f✧${randomColors(formatted)}§f✧" else "§3$formatted")
        }
    }

    private fun isStyled(name: Component) =
        name.visit({ style, _ -> if (style.color != null) Optional.of(true) else Optional.empty() }, Style.EMPTY).isPresent

    // 1234 -> 1.2k, 12345 -> 12.3k, 123456 -> 123k
    private fun format(value: Long): String {
        val (divideBy, suffix) = suffixes.firstOrNull { value >= it.first } ?: return value.toString()
        val truncated = value / (divideBy / 10)
        val hasDecimal = truncated < 100 && truncated % 10 != 0L
        return if (hasDecimal) "${truncated / 10.0}$suffix" else "${truncated / 10}$suffix"
    }

    // Never the same color twice in a row
    private fun randomColors(text: String) = buildString {
        var last: String? = null
        for (char in text) {
            val color = critColors.filter { it != last }.random()
            append(color).append(char).append("§r")
            last = color
        }
    }
}
