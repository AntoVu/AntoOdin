package com.anto.antoodin.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.odtheking.odin.OdinMod
import com.odtheking.odin.utils.customData
import com.odtheking.odin.utils.network.WebUtils
import kotlinx.coroutines.launch
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrNull

/**
 * Lowest BIN (Odin's price server), bazaar and NPC sell prices (Hypixel's public endpoints, no API key).
 * Maps are swapped whole from the fetch coroutine and only read elsewhere.
 * Item id resolution ported from NoammAddons by Noamm9 (CC0-1.0).
 */
object Prices {
    private const val LOWEST_BIN_URL = "https://lb.odtheking.com/lowestbins"
    private const val BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar"
    private const val ITEMS_URL = "https://api.hypixel.net/v2/resources/skyblock/items"
    private const val REFRESH_MS = 10 * 60 * 1000L

    /** Top buy order and lowest sell offer, per unit. */
    class BazaarPrice(val buy: Double, val sell: Double)

    @Volatile private var lowestBins = emptyMap<String, Double>()
    @Volatile private var bazaar = emptyMap<String, BazaarPrice>()
    @Volatile private var npcPrices = emptyMap<String, Double>()
    @Volatile private var npcPricesLoaded = false
    private var fetchedAt = 0L

    // Called from the main thread whenever prices are about to be used
    fun refresh() {
        if (System.currentTimeMillis() - fetchedAt < REFRESH_MS) return
        fetchedAt = System.currentTimeMillis()
        OdinMod.scope.launch {
            WebUtils.fetchJson<Map<String, Double>>(LOWEST_BIN_URL).onSuccess { lowestBins = it }
            WebUtils.fetchString(BAZAAR_URL).onSuccess { bazaar = parseBazaar(it) }
            // NPC prices barely change, so once per session
            if (!npcPricesLoaded) WebUtils.fetchString(ITEMS_URL).onSuccess {
                npcPrices = parseNpcPrices(it)
                npcPricesLoaded = true
            }
        }
    }

    // Enchanted books are listed as ENCHANTED_BOOK-<NAME>-<LEVEL> on the auction house
    fun lowestBin(id: String): Double? {
        lowestBins[id]?.let { return it }
        if (!id.startsWith("ENCHANTMENT_")) return null
        return lowestBins["ENCHANTED_BOOK-${id.removePrefix("ENCHANTMENT_").substringBeforeLast('_')}-${id.substringAfterLast('_')}"]
    }

    fun bazaar(id: String): BazaarPrice? = bazaar[id]

    fun npcSell(id: String): Double? = npcPrices[id]

    private fun parseBazaar(json: String): Map<String, BazaarPrice> {
        val products = JsonParser.parseString(json).asJsonObject.getAsJsonObject("products") ?: return emptyMap()
        return products.entrySet().associate { (id, element) ->
            val product = element.asJsonObject
            id to BazaarPrice(topPrice(product, "sell_summary"), topPrice(product, "buy_summary"))
        }
    }

    private fun topPrice(product: JsonObject, summary: String): Double =
        product.getAsJsonArray(summary)?.firstOrNull()?.asJsonObject?.get("pricePerUnit")?.asDouble ?: 0.0

    private fun parseNpcPrices(json: String): Map<String, Double> =
        JsonParser.parseString(json).asJsonObject.getAsJsonArray("items").mapNotNull { element ->
            val item = element.asJsonObject
            val price = item.get("npc_sell_price")?.asDouble ?: return@mapNotNull null
            item.get("id").asString.replace(':', '-') to price
        }.toMap()

    /** The id prices are keyed by: pets, books, runes, potions and shards carry their variant in the id. */
    fun skyblockId(stack: ItemStack): String {
        if (stack.isEmpty) return ""
        val data = stack.customData
        val id = data.getString("id").getOrNull()?.replace(':', '-').orEmpty()

        return when (id) {
            "PET" -> data.getString("petInfo").getOrNull()?.let { raw ->
                val pet = JsonParser.parseString(raw).asJsonObject
                "PET-${pet.get("type")?.asString}-${pet.get("tier")?.asString}"
            } ?: id

            "ENCHANTED_BOOK" -> data.getCompound("enchantments").getOrNull()?.let { enchants ->
                val enchant = enchants.keySet().singleOrNull() ?: return@let null
                val level = enchants.getIntOr(enchant, 0)
                if (level > 0) "ENCHANTMENT_${enchant.uppercase()}_$level" else null
            } ?: id

            "RUNE", "UNIQUE_RUNE" -> data.getCompound("runes").getOrNull()?.let { runes ->
                val rune = runes.keySet().singleOrNull() ?: return@let null
                val level = runes.getIntOr(rune, 0)
                if (level > 0) "RUNE-${rune.uppercase()}-$level" else null
            } ?: id

            "POTION" -> {
                val potion = data.getString("potion").getOrNull()?.takeIf { it.isNotEmpty() } ?: return id
                val level = data.getIntOr("potion_level", 0).takeIf { it > 0 } ?: return id
                "POTION-${potion.uppercase()}-$level${if (data.getBooleanOr("enhanced", false)) "-ENHANCED" else ""}"
            }

            "ATTRIBUTE_SHARD" -> shardId(stack.hoverName.string)
            else -> id
        }
    }

    private val shardCountSuffix = Regex(" X\\d+$")

    // Shards whose display name differs from their id
    private val shardIdOverrides = mapOf(
        "BOGGED" to "SHARD_SEA_ARCHER", "LOTUSFISH" to "SHARD_LOTUS_FISH", "INKLING" to "SHARD_NIGHT_SQUID",
        "LOCH_EMPEROR" to "SHARD_SEA_EMPEROR", "INFERNO_DEMONLORD" to "SHARD_BURNINGSOUL",
        "END_STONE_PROTECTOR" to "SHARD_ENDSTONE_PROTECTOR", "CINDERBAT" to "SHARD_CINDER_BAT",
        "BEETLE" to "SHARD_CROPEETLE", "ABYSSAL_LANTERNFISH" to "SHARD_ABYSSAL_LANTERN", "SEASHINE" to "SHARD_SEA_SHINE",
        "WITHER_SPECTRE" to "SHARD_WITHER_SPECTER", "FIELD_MOUSE" to "SHARD_PEST", "ZEALOT_BRUISER" to "SHARD_BRUISER",
        "STRIDERSURFER" to "SHARD_STRIDER_SURFER", "EARTHWORM" to "SHARD_TERMITE", "FLIPFLOPPER" to "SHARD_FLIP_FLOPPER"
    )

    private fun shardId(displayName: String): String {
        val name = displayName.uppercase().replace(shardCountSuffix, "").removeSuffix(" SHARD").replace(' ', '_')
        return shardIdOverrides[name] ?: "SHARD_$name"
    }
}
