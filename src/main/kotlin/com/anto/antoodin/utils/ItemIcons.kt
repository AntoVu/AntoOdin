package com.anto.antoodin.utils

import com.odtheking.odin.utils.createSkullStack
import com.odtheking.odin.utils.texture
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import java.util.Base64

/** Text ↔ display item, for icons and model swaps (Inventory Buttons, Item Customizer). */
object ItemIcons {
    private val cache = HashMap<String, ItemStack>()

    /** "skull:<texture hash>" gives a player head, anything else is read as an item id ("diamond_sword", "minecraft:chest"). */
    fun parse(text: String): ItemStack = cache.getOrPut(text) {
        val value = text.trim().lowercase()
        if (value.startsWith("skull:")) {
            val hash = value.removePrefix("skull:")
            return@getOrPut if (hash.matches(Regex("[0-9a-f]+"))) createSkullStack(hash) else ItemStack.EMPTY
        }
        Identifier.tryParse(value)?.let(BuiltInRegistries.ITEM::getOptional)?.map(::ItemStack)?.orElse(null) ?: ItemStack.EMPTY
    }

    /** The text for a stack: its skin as "skull:<hash>" for heads, otherwise its item id. */
    fun textOf(stack: ItemStack): String {
        stack.texture?.let { texture ->
            val json = runCatching { String(Base64.getDecoder().decode(texture)) }.getOrNull()
            json?.let { Regex("texture/([0-9a-fA-F]+)").find(it) }?.let { return "skull:${it.groupValues[1]}" }
        }
        val key = BuiltInRegistries.ITEM.getKey(stack.item)
        return if (key.namespace == "minecraft") key.path else key.toString()
    }
}
