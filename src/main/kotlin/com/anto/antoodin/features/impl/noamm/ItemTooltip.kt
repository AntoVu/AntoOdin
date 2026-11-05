// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Prices
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.customData
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToLong

object ItemTooltip : Module(
    name = "Item Tooltip",
    description = "Adds lowest BIN, bazaar and NPC sell prices to item tooltips, priced for the whole stack.",
    category = Skit.NOAMM
) {
    private val showLowestBin by BooleanSetting("Lowest BIN", true, desc = "Shows the lowest BIN price.")
    private val showBazaar by BooleanSetting("Bazaar", true, desc = "Shows the bazaar buy order and sell offer prices.")
    private val showNpcSell by BooleanSetting("NPC Sell", true, desc = "Shows the NPC sell price.")
    private val showCreated by BooleanSetting("Created Date", true, desc = "Shows the date and time the item was created.")
    private val showAge by BooleanSetting("Item Age", true, desc = "Shows how old the item is, never rounded up.")
    private val showItemId by BooleanSetting("SkyBlock ID", true, desc = "Shows the item's SkyBlock ID (e.g. SUPER_UNDEAD_BOW).")

    init {
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            if (!enabled || !LocationUtils.isInSkyblock) return@register
            addPrices(stack, lines)
            addAge(stack, lines)
            if (showItemId) stack.itemId.takeIf { it.isNotEmpty() }?.let { lines.add(Component.literal("§8ID: $it")) }
        }
    }

    private fun addPrices(stack: ItemStack, lines: MutableList<Component>) {
        val id = Prices.skyblockId(stack).ifEmpty { return }
        Prices.refresh()
        val quantity = stack.count

        // Bazaar items have no BIN listings, so show one or the other like Noamm
        val bazaar = Prices.bazaar(id)
        if (bazaar != null) {
            if (showBazaar) {
                addPriceLine(lines, "Bazaar Buy", bazaar.buy, quantity)
                addPriceLine(lines, "Bazaar Sell", bazaar.sell, quantity)
            }
        } else if (showLowestBin) Prices.lowestBin(id)?.let { addPriceLine(lines, "Lowest BIN", it, quantity) }

        if (showNpcSell) Prices.npcSell(id)?.let { addPriceLine(lines, "NPC Sell", it, quantity) }
    }

    private fun addPriceLine(lines: MutableList<Component>, label: String, unitPrice: Double, quantity: Int) {
        if (unitPrice <= 0.0) return
        val stackInfo = if (quantity > 1) " §8(${format(unitPrice)} x$quantity)" else ""
        lines.add(Component.literal("§e$label: §6${format(unitPrice * quantity)}$stackInfo"))
    }

    private fun format(price: Double) = "%,d".format(price.roundToLong())

    // Item age idea from Devonian and NoFrills (GPL-3.0). Written from scratch for AntoOdin, no Devonian or NoFrills code is used.
    private val oldTimestampFormat = DateTimeFormatter.ofPattern("M/d/yy h:mm a", Locale.US)
    private val createdFormat = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)

    private fun addAge(stack: ItemStack, lines: MutableList<Component>) {
        if (!showCreated && !showAge) return
        val created = createdAt(stack)?.atZone(ZoneId.systemDefault()) ?: return
        if (showCreated) lines.add(Component.literal("§7Created: §f${created.format(createdFormat)}"))
        if (showAge) lines.add(Component.literal("§7Age: §f${age(created, ZonedDateTime.now())}"))
    }

    // Newer items store epoch millis; old ones a US Eastern "M/d/yy h:mm a" string
    private fun createdAt(stack: ItemStack): Instant? {
        val tag = stack.customData.get("timestamp") ?: return null
        tag.asLong().getOrNull()?.let { return Instant.ofEpochMilli(it) }
        val text = tag.asString().getOrNull() ?: return null
        return runCatching { LocalDateTime.parse(text, oldTimestampFormat).atZone(ZoneId.of("America/New_York")).toInstant() }.getOrNull()
    }

    // Floored, top 3 non-zero units: 5.6 years shows as "5 years, 7 months, ..."
    private fun age(from: ZonedDateTime, to: ZonedDateTime): String {
        val period = Period.between(from.toLocalDate(), to.toLocalDate()).let {
            // Not a full day yet if today's clock is earlier than the creation time
            if (to.toLocalTime() < from.toLocalTime() && !it.isZero) Period.between(from.toLocalDate(), to.toLocalDate().minusDays(1)) else it
        }
        val units = if (!period.isZero && !period.isNegative) listOf(period.years to "year", period.months to "month", period.days to "day")
        else Duration.between(from, to).let { listOf(it.toHours().toInt() to "hour", it.toMinutesPart() to "minute") }
        return units.filter { it.first > 0 }.take(3).joinToString(", ") { (n, unit) -> "$n $unit${if (n == 1) "" else "s"}" }
            .ifEmpty { "<1 minute" }
    }
}
