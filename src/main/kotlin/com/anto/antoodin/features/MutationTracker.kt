package com.anto.antoodin.features

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.loreString
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.setClipboardContent
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.random.Random

object MutationTracker {
    val MUTATIONS = mapOf(
        "jellybean" to listOf("sugar_cane", "sunflower", "moonflower"),
        "thunderling" to listOf("wild_rose", "melon", "cactus"),
        "aloe" to listOf("wheat", "sunflower", "moonflower"),
        "devourer" to listOf("pumpkin", "mushroom")
    )

    private val NPC_PRICES = mapOf(
        "wild_rose" to 4, "wheat" to 6, "seeds" to 3, "melon" to 2, "cocoa_beans" to 3, "moonflower" to 4,
        "carrot" to 3, "sugar_cane" to 4, "pumpkin" to 10, "mushroom" to 10, "nether_wart" to 4,
        "sunflower" to 4, "cactus" to 4, "potato" to 3
    )

    private const val COLLECTION_SLOT = 18
    private val collectionRegex = Regex("""([\d,]+)/[\d,]+""")

    private var busy = false
    private var startType: String? = null
    private var startValues: Map<String, Long> = emptyMap()

    fun start(type: String) {
        val crops = MUTATIONS[type] ?: return modMessage("§cUnknown mutation type: $type")
        if (busy) return modMessage("§cTracker is busy.")
        busy = true
        modMessage("§aTracker is now on, logging collections...")
        scan(crops) { values ->
            startType = type
            startValues = values
            modMessage("§aLogging done.")
        }
    }

    fun stop(type: String) {
        val crops = MUTATIONS[type] ?: return modMessage("§cUnknown mutation type: $type")
        if (busy) return modMessage("§cTracker is busy.")
        if (startType != type) return modMessage("§c$type tracking was never started.")
        busy = true
        scan(crops) { values ->
            val deltas = crops.associateWith { values.getValue(it) - startValues.getValue(it) }
            val profit = deltas.entries.sumOf { (crop, delta) -> delta * (NPC_PRICES[crop] ?: 0) }
            setClipboardContent(formatResult(deltas, profit))
            startType = null
            modMessage("§aTracking stopped, info copied to clipboard.")
        }
    }

    fun formatResult(deltas: Map<String, Long>, profit: Long): String =
        deltas.entries.joinToString("\n", postfix = "\nCrop Profit: ${"%,d".format(profit)}") { (crop, delta) ->
            "${"%,d".format(delta)} ${crop.split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }}"
        }

    fun parseCollection(lore: List<String>): Long? =
        lore.firstNotNullOfOrNull { collectionRegex.find(it.noControlCodes) }?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()

    private fun scan(crops: List<String>, values: Map<String, Long> = emptyMap(), onDone: (Map<String, Long>) -> Unit) {
        val crop = crops.getOrNull(values.size) ?: return run { busy = false; onDone(values) }

        schedule(5) {
            mc.player?.connection?.sendCommand("collections $crop")
            schedule(msToTicks(1000 + Random.nextLong(0, 1001))) {
                val screen = mc.screen as? AbstractContainerScreen<*>
                val value = screen?.takeIf { it.title.string.contains("Collection") }
                    ?.menu?.slots?.getOrNull(COLLECTION_SLOT)?.item?.loreString?.let(::parseCollection)
                mc.player?.closeContainer()

                if (value == null) {
                    busy = false
                    return@schedule modMessage("§cFailed to read $crop collection.")
                }
                scan(crops, values + (crop to value), onDone)
            }
        }
    }

    private fun msToTicks(ms: Long) = ((ms / 1000.0) * 20).toInt().coerceAtLeast(1)
}
