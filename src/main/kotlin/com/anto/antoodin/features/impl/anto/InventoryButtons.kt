// Feature idea from NotEnoughUpdates (LGPL-3.0). Written from scratch for AntoOdin, no NEU code is used.
package com.anto.antoodin.features.impl.anto

import com.mojang.blaze3d.platform.InputConstants
import com.anto.antoodin.mixin.accessors.AbstractContainerScreenAccessor
import com.anto.antoodin.utils.ItemIcons
import com.anto.antoodin.utils.Skit
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component
import java.io.File

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

    /** Background looks to pick from per button. Light styles get a dark letter, the rest a white one. */
    enum class Style(val light: Boolean) { Raised(true), Slot(true), Dark(false), Flat(false), Clear(false) }

    data class InvButton(var command: String = "", var icon: String = "", var style: Style = Style.Raised)

    /** A button position: offset from the screen's top left, or from its right/bottom edge when anchored there. */
    class Spot(val x: Int, val y: Int, val right: Boolean = false, val bottom: Boolean = false, val playerInvOnly: Boolean = false) {
        fun x(left: Int, width: Int) = left + x + if (right) width else 0
        fun y(top: Int, height: Int) = top + y + if (bottom) height else 0
    }

    const val SIZE = 18

    // Inside the player inventory (crafting grid, the player model's corners, beside the recipe book button), then the four outside edges.
    // The model's frame is x 26-74, y 8-77 inside, so its buttons sit 1px in from it.
    val spots: List<Spot> = buildList {
        listOf(97 to 17, 115 to 17, 97 to 35, 115 to 35, 153 to 27, 27 to 9, 56 to 9, 27 to 59, 56 to 59, 128 to 61, 150 to 61)
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
        gson.fromJson<MutableMap<Int, InvButton>>(buttonsFile.readText(), buttonsType).onEach { (_, b) -> sanitize(b) }
    }.getOrNull() ?: mutableMapOf()

    // Gson bypasses Kotlin's null checks, so a missing or unknown style arrives as null
    @Suppress("SENSELESS_COMPARISON")
    private fun sanitize(button: InvButton) {
        if (button.style == null) button.style = Style.Raised
    }

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
                if (!enabled || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return@register true
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

    /** The button's [Style] background, with the icon (or the command's first letter) on top. */
    fun drawButton(graphics: GuiGraphicsExtractor, x: Int, y: Int, button: InvButton, hovered: Boolean) {
        val r = x + SIZE
        val b = y + SIZE
        when (button.style) {
            // Light top-left edge, dark bottom-right edge, like vanilla's raised panels (Dark is the same, darker)
            Style.Raised, Style.Dark -> {
                val dark = button.style == Style.Dark
                graphics.fill(x, y, r, b, if (dark) 0xFF1E1E1E.toInt() else 0xFF555555.toInt())
                graphics.fill(x, y, r - 1, b - 1, if (dark) 0xFF6A6A6A.toInt() else 0xFFFFFFFF.toInt())
                graphics.fill(x + 1, y + 1, r - 1, b - 1, when {
                    dark && hovered -> 0xFF4A4A4A.toInt()
                    dark -> 0xFF373737.toInt()
                    hovered -> 0xFFDADADA.toInt()
                    else -> 0xFFC6C6C6.toInt()
                })
            }
            // Sunken, like an inventory slot
            Style.Slot -> {
                graphics.fill(x, y, r, b, 0xFFFFFFFF.toInt())
                graphics.fill(x, y, r - 1, b - 1, 0xFF373737.toInt())
                graphics.fill(x + 1, y + 1, r - 1, b - 1, if (hovered) 0xFFA0A0A0.toInt() else 0xFF8B8B8B.toInt())
            }
            Style.Flat -> graphics.fill(x, y, r, b, if (hovered) 0xB0000000.toInt() else 0x80000000.toInt())
            Style.Clear -> if (hovered) graphics.fill(x, y, r, b, 0x40FFFFFF)
        }
        val stack = ItemIcons.parse(button.icon)
        if (!stack.isEmpty) return graphics.item(stack, x + 1, y + 1)
        val letter = button.command.trim().removePrefix("/").take(1).uppercase()
        graphics.centeredText(mc.font, letter, x + SIZE / 2, y + 5, if (button.style.light) 0xFF404040.toInt() else 0xFFFFFFFF.toInt())
    }
}
