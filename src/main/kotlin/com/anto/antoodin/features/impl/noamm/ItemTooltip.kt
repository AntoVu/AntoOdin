// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Prices
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import kotlin.math.roundToLong

object ItemTooltip : Module(
    name = "Item Tooltip",
    description = "Adds lowest BIN, bazaar and NPC sell prices to item tooltips. Hold Shift for the whole stack.",
    category = Skit.NOAMM
) {
    private val showLowestBin by BooleanSetting("Lowest BIN", true, desc = "Shows the lowest BIN price.")
    private val showBazaar by BooleanSetting("Bazaar", true, desc = "Shows the bazaar buy order and sell offer prices.")
    private val showNpcSell by BooleanSetting("NPC Sell", true, desc = "Shows the NPC sell price.")

    init {
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            if (enabled && LocationUtils.isInSkyblock) addPrices(stack, lines)
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
        val showStack = quantity > 1 && mc.hasShiftDown()
        val price = if (showStack) unitPrice * quantity else unitPrice
        val stackInfo = if (showStack) " §8(${quantity}x ${format(unitPrice)})" else ""
        lines.add(Component.literal("§e$label: §6${format(price)}$stackInfo"))
    }

    private fun format(price: Double) = "%,d".format(price.roundToLong())
}
