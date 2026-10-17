// Feature idea from NotEnoughUpdates (LGPL-3.0). Written from scratch for AntoOdin, no NEU code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.mixin.accessors.AbstractContainerScreenAccessor
import com.anto.antoodin.utils.Skit
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.createSkullStack
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.texture
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import java.io.File
import java.util.Base64

/**
 * Buttons around container screens that run a command when clicked, placed with [InventoryButtonsScreen] (/ao buttons).
 * Each button sits on one of the fixed [spots], relative to the screen's edges, so it follows any container size.
 */
object InventoryButtons : Module(
    name = "Inventory Buttons",
    description = "Buttons around your inventory that run commands. Place them with /ao buttons.",
    category = Skit.ANTO
) {
    private val inContainers by BooleanSetting("Show In Containers", true, desc = "Also shows the buttons on the outside edges in chests and other menus, not just your inventory.")
    private val openEditor by ActionSetting("Open Editor", desc = "Opens the button editor (/ao buttons).") {
        mc.gui.setScreen(InventoryButtonsScreen(mc.gui.screen()))
    }

    data class InvButton(var command: String = "", var icon: String = "")

    /** A button position: offset from the screen's top left, or from its right/bottom edge when anchored there. */
    class Spot(val x: Int, val y: Int, val right: Boolean = false, val bottom: Boolean = false, val playerInvOnly: Boolean = false) {
        fun x(left: Int, width: Int) = left + x + if (right) width else 0
        fun y(top: Int, height: Int) = top + y + if (bottom) height else 0
    }

    const val SIZE = 18

    // Inside the player inventory (crafting grid, around the player model, beside the recipe book button), then the four outside edges
    val spots: List<Spot> = buildList {
        listOf(97 to 17, 115 to 17, 97 to 35, 115 to 35, 153 to 27, 26 to 8, 60 to 8, 26 to 60, 60 to 60, 128 to 61, 150 to 61)
            .forEach { (x, y) -> add(Spot(x, y, playerInvOnly = true)) }
        for (y in listOf(2, 22, 42, 62)) add(Spot(-SIZE - 2, y))
        for (y in listOf(-84, -64, -44, -24)) add(Spot(-SIZE - 2, y, bottom = true))
        for (y in listOf(2, 22, 42, 62)) add(Spot(2, y, right = true))
        for (y in listOf(-84, -64, -44, -24)) add(Spot(2, y, right = true, bottom = true))
        for (i in 0..7) add(Spot(4 + 21 * i, -SIZE - 2))
        for (i in 0..7) add(Spot(4 + 21 * i, 2, bottom = true))
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val buttonsType = object : TypeToken<MutableMap<Int, InvButton>>() {}.type
    private val buttonsFile = File(OdinMod.configFile, "addons/antoodin-inventory-buttons.json")

    /** Spot index → button. */
    val buttons: MutableMap<Int, InvButton> = runCatching {
        gson.fromJson<MutableMap<Int, InvButton>>(buttonsFile.readText(), buttonsType)
    }.getOrNull() ?: mutableMapOf()

    fun save() {
        buttons.values.removeIf { it.command.isBlank() && it.icon.isBlank() }
        runCatching {
            buttonsFile.parentFile.mkdirs()
            buttonsFile.writeText(gson.toJson(buttons, buttonsType))
        }.onFailure { modMessage("§cCouldn't save inventory buttons: ${it.message}") }
    }

    init {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen !is AbstractContainerScreen<*>) return@register
            ScreenEvents.afterExtract(screen).register { _, graphics, mouseX, mouseY, _ ->
                if (enabled) forEachButton(screen) { button, x, y ->
                    val hovered = mouseX in x until x + SIZE && mouseY in y until y + SIZE
                    drawButton(graphics, x, y, button, hovered)
                    if (hovered) graphics.setTooltipForNextFrame(mc.font, Component.literal("§7/${button.command.removePrefix("/")}"), mouseX, mouseY)
                }
            }
            // Returning false cancels the click, so the slot or widget underneath never sees it
            ScreenMouseEvents.allowMouseClick(screen).register { _, event ->
                if (!enabled || event.button() != 0) return@register true
                forEachButton(screen) { button, x, y ->
                    if (event.x() >= x && event.x() < x + SIZE && event.y() >= y && event.y() < y + SIZE) {
                        mc.player?.connection?.sendCommand(button.command.trim().removePrefix("/"))
                        return@register false
                    }
                }
                true
            }
        }
    }

    private inline fun forEachButton(screen: AbstractContainerScreen<*>, action: (InvButton, Int, Int) -> Unit) {
        val playerInv = screen is InventoryScreen
        if (!playerInv && !inContainers) return
        val acc = screen as AbstractContainerScreenAccessor
        for ((index, button) in buttons) {
            val spot = spots.getOrNull(index) ?: continue
            if (button.command.isBlank() || (spot.playerInvOnly && !playerInv)) continue
            action(button, spot.x(acc.leftPos, acc.imageWidth), spot.y(acc.topPos, acc.imageHeight))
        }
    }

    /** A raised gray button like the vanilla GUI, with the icon (or the command's first letter) on top. */
    fun drawButton(graphics: GuiGraphicsExtractor, x: Int, y: Int, button: InvButton?, hovered: Boolean) {
        graphics.fill(x, y, x + SIZE, y + SIZE, 0xFF555555.toInt())
        graphics.fill(x, y, x + SIZE - 1, y + SIZE - 1, 0xFFFFFFFF.toInt())
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, if (hovered) 0xFFDADADA.toInt() else 0xFFC6C6C6.toInt())
        if (button == null) return
        val stack = icon(button.icon)
        if (!stack.isEmpty) return graphics.item(stack, x + 1, y + 1)
        val letter = button.command.trim().removePrefix("/").take(1).uppercase()
        graphics.centeredText(mc.font, letter, x + SIZE / 2, y + 5, 0xFF404040.toInt())
    }

    private val iconCache = HashMap<String, ItemStack>()

    /** "skull:<texture hash>" gives a player head, anything else is read as an item id ("diamond_sword", "minecraft:chest"). */
    fun icon(text: String): ItemStack = iconCache.getOrPut(text) {
        val value = text.trim().lowercase()
        if (value.startsWith("skull:")) {
            val hash = value.removePrefix("skull:")
            return@getOrPut if (hash.matches(Regex("[0-9a-f]+"))) createSkullStack(hash) else ItemStack.EMPTY
        }
        Identifier.tryParse(value)?.let(BuiltInRegistries.ITEM::getOptional)?.map(::ItemStack)?.orElse(null) ?: ItemStack.EMPTY
    }

    /** The icon text for a stack: its skin as "skull:<hash>" for heads, otherwise its item id. */
    fun iconOf(stack: ItemStack): String {
        stack.texture?.let { texture ->
            val json = runCatching { String(Base64.getDecoder().decode(texture)) }.getOrNull()
            json?.let { Regex("texture/([0-9a-fA-F]+)").find(it) }?.let { return "skull:${it.groupValues[1]}" }
        }
        val key = BuiltInRegistries.ITEM.getKey(stack.item)
        return if (key.namespace == "minecraft") key.path else key.toString()
    }
}
