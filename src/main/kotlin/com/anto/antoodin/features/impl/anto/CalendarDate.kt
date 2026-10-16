// Feature idea from NoFrills (GPL-3.0). Written from scratch for AntoOdin, no NoFrills code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.noControlCodes
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.network.chat.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object CalendarDate : Module(
    name = "Calendar Date",
    description = "Shows the real date and time of events in the SkyBlock calendar.",
    category = Skit.ANTO
) {
    private val startsInRegex = Regex("^Starts in: (.+)$")
    private val unitRegex = Regex("(\\d+)([dhms])")
    private val dayOfWeek = DateTimeFormatter.ofPattern("EEEE ")
    private val dateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

    // Extra lines for events that have something before they start
    private val earlier = mapOf(
        "Spooky Festival" to ("§6Fear Mongerer Arrives" to Duration.ofHours(1)),
        "Season of Jerry" to ("§cWorkshop Opens" to Duration.ofHours(7).plusMinutes(40))
    )

    init {
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            if (!enabled || mc.gui.screen()?.title?.string != "Calendar and Events") return@register
            val startsIn = lines.firstNotNullOfOrNull { startsInRegex.find(it.string.noControlCodes)?.groupValues?.get(1) }
                ?: return@register
            val start = startTime(startsIn) ?: return@register

            lines.add(Component.empty())
            lines.add(line("§eDate of Event", start))
            val name = stack.hoverName.string.noControlCodes
            earlier.entries.firstOrNull { name.endsWith(it.key) }?.value?.let { (label, before) ->
                lines.add(line(label, start.minus(before)))
            }
        }
    }

    // Events start on a 5-second mark and the menu's countdown lags a little, so round up to the next one
    private fun startTime(text: String): LocalDateTime? {
        val units = unitRegex.findAll(text).toList().ifEmpty { return null }
        val seconds = units.sumOf { match ->
            val n = match.groupValues[1].toLong()
            when (match.groupValues[2]) {
                "d" -> n * 86400
                "h" -> n * 3600
                "m" -> n * 60
                else -> n
            }
        }
        val start = LocalDateTime.now().withNano(0).plusSeconds(seconds)
        return start.plusSeconds(((5 - start.second % 5) % 5).toLong())
    }

    private fun line(label: String, time: LocalDateTime) =
        Component.literal("$label: §b${time.format(dayOfWeek)}${time.format(dateTime)}")
}
