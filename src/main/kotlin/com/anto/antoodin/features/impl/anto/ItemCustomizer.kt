// Feature idea from NotEnoughUpdates (LGPL-3.0). Written from scratch for AntoOdin, no NEU code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.mixin.accessors.CustomDataAccessor
import com.anto.antoodin.utils.ItemIcons
import com.anto.antoodin.utils.Skit
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.odtheking.odin.OdinMod
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.UUID_STRING
import com.odtheking.odin.utils.modMessage
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import java.io.File

/**
 * Per-item looks keyed by the SkyBlock item UUID: name, enchant glint, model (any item, or a head skin) and leather dye.
 * Edited with [ItemCustomizeScreen] (/ao customize while holding the item).
 *
 * Display only: the real stack is untouched, so other mods still read the real name and item.
 * The model, glint and dye come from a restyled copy handed to the item renderer.
 *
 * @see com.anto.antoodin.mixin.mixins.ItemModelResolverMixin
 */
object ItemCustomizer : Module(
    name = "Item Customizer",
    description = "Change an item's name, glint, model and dye color, only for you. Hold the item and use /ao customize.",
    category = Skit.ANTO
) {
    enum class Glint { Default, On, Off }

    data class Custom(var name: String = "", var glint: Glint = Glint.Default, var model: String = "", var dye: String = "") {
        fun isEmpty() = name.isBlank() && glint == Glint.Default && model.isBlank() && dye.isBlank()
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val customsType = object : TypeToken<MutableMap<String, Custom>>() {}.type
    private val customsFile = File(OdinMod.configFile, "addons/antoodin-item-customizer.json")

    /** Item UUID → custom looks. */
    val customs: MutableMap<String, Custom> = runCatching {
        gson.fromJson<MutableMap<String, Custom>>(customsFile.readText(), customsType).onEach { (_, c) -> sanitize(c) }
    }.getOrNull() ?: mutableMapOf()

    // Gson bypasses Kotlin's null checks, so an unknown or missing glint arrives as null
    @Suppress("SENSELESS_COMPARISON")
    private fun sanitize(custom: Custom) {
        if (custom.glint == null) custom.glint = Glint.Default
    }

    fun save() {
        customs.values.removeIf(Custom::isEmpty)
        changed()
        runCatching {
            customsFile.parentFile.mkdirs()
            customsFile.writeText(gson.toJson(customs, customsType))
        }.onFailure { modMessage("§cCouldn't save item customizations: ${it.message}") }
    }

    init {
        // Line 0 is the item's name
        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            if (lines.isNotEmpty()) customName(stack)?.let { lines[0] = it }
        }
    }

    fun uuid(stack: ItemStack): String? {
        val data = stack.get(DataComponents.CUSTOM_DATA) ?: return null
        return (data as CustomDataAccessor).tag.getString(UUID_STRING).orElse(null)?.takeIf { it.isNotEmpty() }
    }

    private fun customFor(stack: ItemStack): Custom? {
        if (!enabled || customs.isEmpty() || stack.isEmpty) return null
        return customs[uuid(stack) ?: return null]
    }

    fun customName(stack: ItemStack): Component? =
        customFor(stack)?.name?.takeIf { it.isNotBlank() }?.let { Component.literal(it.replace('&', '§')) }

    // Last restyled copy per UUID, reused while the item renderer keeps getting the same stack object
    private val displayCache = HashMap<String, Pair<ItemStack, ItemStack>>()

    /** Clears the restyled copies, so the next frame picks up an edit. */
    fun changed() = displayCache.clear()

    /** The stack the renderer should draw: a restyled copy for customized items, otherwise [stack] itself. */
    fun displayStack(stack: ItemStack): ItemStack {
        val custom = customFor(stack) ?: return stack
        if (custom.glint == Glint.Default && custom.model.isBlank() && custom.dye.isBlank()) return stack
        val uuid = uuid(stack) ?: return stack
        displayCache[uuid]?.let { (original, copy) -> if (original === stack) return copy }
        return restyle(stack, custom).also { displayCache[uuid] = stack to it }
    }

    private fun restyle(stack: ItemStack, custom: Custom): ItemStack {
        val copy = stack.copy()
        when (custom.glint) {
            Glint.On -> copy.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
            Glint.Off -> copy.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false)
            Glint.Default -> {}
        }
        val model = ItemIcons.parse(custom.model)
        if (!model.isEmpty) {
            copy.set(DataComponents.ITEM_MODEL, model.get(DataComponents.ITEM_MODEL))
            model.get(DataComponents.PROFILE)?.let { copy.set(DataComponents.PROFILE, it) }
        }
        parseHex(custom.dye)?.let { copy.set(DataComponents.DYED_COLOR, DyedItemColor(it)) }
        return copy
    }

    /** "RRGGBB" (with or without #) → RGB int, null when blank or invalid. */
    fun parseHex(text: String): Int? = text.trim().removePrefix("#").takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }?.toInt(16)
}
