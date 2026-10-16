// Ported from NoammAddons by Noamm9 (CC0-1.0). Style modeled on NEU's damage indicator (own code, NEU is LGPL)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.anto.antoodin.utils.legacyText
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.SyncedDataHolder
import net.minecraft.network.syncher.SynchedEntityData.DataValue
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

    /** @see com.anto.antoodin.mixin.mixins.SynchedEntityDataMixin */
    @JvmStatic
    fun rewrite(holder: SyncedDataHolder, items: List<DataValue<*>>): List<DataValue<*>> {
        if (!enabled || holder !is ArmorStand || !LocationUtils.isInSkyblock) return items
        return items.map { value ->
            // The custom name is an armor stand's only Optional<Component> field
            val name = (value.value() as? Optional<*>)?.orElse(null) as? Component ?: return@map value
            val text = reformat(name.legacyText()) ?: return@map value
            @Suppress("UNCHECKED_CAST")
            DataValue(value.id(), value.serializer() as EntityDataSerializer<Optional<Component>>, Optional.of(Component.literal(text)))
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

    private fun critColored(text: String) = text.withIndex().joinToString("") { (i, char) -> critColors[i % critColors.size] + char }
}
