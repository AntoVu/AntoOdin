// Feature idea from NotEnoughUpdates (LGPL-3.0). Written from scratch for AntoOdin, no NEU code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.features.impl.anto.InventoryButtons.InvButton
import com.anto.antoodin.features.impl.anto.InventoryButtons.SIZE
import com.anto.antoodin.utils.ItemIcons
import com.odtheking.odin.OdinMod.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/** Shows every button spot around a player inventory. Click a spot to set its command and icon in the side panel. */
class InventoryButtonsScreen(private val previous: Screen?) : Screen(Component.literal("Inventory Buttons")) {
    private val inventoryTexture = Identifier.withDefaultNamespace("textures/gui/container/inventory.png")
    private val panelWidth = 150
    private var left = 0
    private var top = 0
    private var selected = -1

    private lateinit var command: EditBox
    private lateinit var icon: EditBox
    private lateinit var heldItem: Button
    private lateinit var clear: Button

    override fun init() {
        super.init()
        // Inventory plus its left/right button columns, then the panel
        left = (width - (176 + 2 * (SIZE + 2) + 10 + panelWidth)) / 2 + SIZE + 2
        top = (height - 166) / 2
        val panelX = left + 176 + SIZE + 12

        command = textBox("", "The command to run, e.g. /warp dh or /pets. Commands from other mods work too.") { editing()?.command = it }
            .also { it.setPosition(panelX, top + 14); it.width = panelWidth }
        icon = textBox("", "An item id (diamond_sword, chest) or skull:<texture hash>. Leave blank to show the command's first letter.") {
            editing()?.icon = it
            colorIcon()
        }.also { it.setPosition(panelX, top + 50); it.width = panelWidth }
        heldItem = Button.builder(Component.literal("Use Held Item")) { icon.value = ItemIcons.textOf(mc.player?.mainHandItem ?: return@builder) }
            .bounds(panelX, top + 72, panelWidth, 20)
            .tooltip(Tooltip.create(Component.literal("Sets the icon to the item in your hand. Heads keep their skin.")))
            .build()
        clear = Button.builder(Component.literal("§cRemove Button")) {
            InventoryButtons.buttons.remove(selected)
            select(selected)
        }.bounds(panelX, top + 96, panelWidth, 20).build()

        listOf(command, icon, heldItem, clear).forEach(::addRenderableWidget)
        select(selected)
    }

    private fun editing(): InvButton? = if (selected < 0) null else InventoryButtons.buttons.getOrPut(selected) { InvButton() }

    private fun select(index: Int) {
        selected = index
        val button = InventoryButtons.buttons[index]
        command.value = button?.command.orEmpty()
        icon.value = button?.icon.orEmpty()
        listOf(command, icon, heldItem, clear).forEach { it.visible = index >= 0 }
        if (index >= 0) setFocused(command)
        colorIcon()
    }

    private fun colorIcon() = icon.setTextColor(when {
        icon.value.isBlank() -> WHITE
        ItemIcons.parse(icon.value).isEmpty -> RED
        else -> GREEN
    })

    private fun spotAt(x: Double, y: Double) = InventoryButtons.spots.indices.firstOrNull { i ->
        val spot = InventoryButtons.spots[i]
        val sx = spot.x(left, 176)
        val sy = spot.y(top, 166)
        x >= sx && x < sx + SIZE && y >= sy && y < sy + SIZE
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        spotAt(event.x(), event.y())?.let {
            select(if (it == selected) -1 else it)
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        guiGraphics.centeredText(font, "§lInventory Buttons §r§7(click a spot to edit it)", width / 2, top - 34, -1)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, inventoryTexture, left, top, 0f, 0f, 176, 166, 256, 256)

        InventoryButtons.spots.forEachIndexed { i, spot ->
            val x = spot.x(left, 176)
            val y = spot.y(top, 166)
            val button = InventoryButtons.buttons[i]?.takeIf { it.command.isNotBlank() }
            val hovered = mouseX in x until x + SIZE && mouseY in y until y + SIZE
            if (button != null) InventoryButtons.drawButton(guiGraphics, x, y, button, hovered)
            else {
                // Empty spots: a faint box with a plus
                guiGraphics.fill(x, y, x + SIZE, y + SIZE, if (hovered) 0x80FFFFFF.toInt() else 0x40FFFFFF)
                guiGraphics.centeredText(font, "+", x + SIZE / 2, y + 5, 0xFFA0A0A0.toInt())
            }
            if (i == selected) guiGraphics.outline(x - 1, y - 1, SIZE + 2, SIZE + 2, 0xFFFFFF55.toInt())
            if (hovered && spot.playerInvOnly) guiGraphics.setTooltipForNextFrame(font, Component.literal("§7Only shown in your own inventory"), mouseX, mouseY)
        }

        if (selected < 0) return
        val panelX = command.x
        guiGraphics.text(font, "Command", panelX, top + 4, -1)
        guiGraphics.text(font, "Icon", panelX, top + 40, -1)
        val preview = ItemIcons.parse(icon.value)
        if (!preview.isEmpty) guiGraphics.item(preview, panelX + panelWidth - 16, top + 32)
    }

    override fun onClose() {
        InventoryButtons.save()
        mc.gui.setScreen(previous)
    }

    override fun isPauseScreen(): Boolean = false
}
