// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.mixin.accessors.AbstractSignEditScreenAccessor
import com.anto.antoodin.utils.Prices
import com.anto.antoodin.utils.Skit
import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.formatNumber
import com.odtheking.odin.utils.itemId
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.SignBlockEntity

object AuctionPriceInput : Module(
    name = "Auction Price Input",
    description = "Replaces the auction price sign with a text box that has an undercut mode. Enter confirms auctions.",
    category = Skit.NOAMM
) {
    private val defaultMode by SelectorSetting("Default Mode", "Undercut", listOf("Normal", "Undercut"), desc = "Input mode used when the price box opens.")
    private val rememberText by BooleanSetting("Remember Text", true, desc = "Keeps the last price you typed when the box reopens.")
    private val rememberMode by BooleanSetting("Remember Mode", true, desc = "Keeps the last mode you used when the box reopens.")

    private val createTitles = setOf("Create BIN Auction", "Create Auction")
    private val confirmTitles = setOf("Confirm BIN Auction", "Confirm Auction")
    private val suffixes = mapOf('k' to 1e3, 'm' to 1e6, 'b' to 1e9, 't' to 1e12)

    private var item: ItemStack? = null
    private var undercut: Boolean? = null
    private var input = ""


    init {
        // Clicking the price button (slot 31) opens the sign, so remember the item being auctioned
        on<GuiEvent.SlotClick> {
            val container = screen as? AbstractContainerScreen<*> ?: return@on
            if (slotIndex != 31 || container.title.string !in createTitles) return@on
            item = container.menu.getSlot(13).item.takeIf { it.itemId.isNotEmpty() }?.copy()
        }

        // Enter clicks the green "Create" / "Confirm" button
        on<ScreenEvent.KeyPress> {
            if (input.key != InputConstants.KEY_RETURN && input.key != InputConstants.KEY_NUMPADENTER) return@on
            val container = screen as? AbstractContainerScreen<*> ?: return@on
            val (slot, titles) = when (container.title.string) {
                in createTitles -> 29 to createTitles
                in confirmTitles -> 11 to confirmTitles
                else -> return@on
            }
            val stack = container.menu.slots.getOrNull(slot)?.item ?: return@on
            if (!stack.`is`(Items.GREEN_TERRACOTTA) || stack.hoverName.string !in titles) return@on
            mc.player?.clickSlot(slot)
            cancel()
        }

        ScreenEvents.AFTER_INIT.register { _, screen, width, height ->
            if (!enabled || screen !is AbstractSignEditScreen) return@register
            val stack = item ?: return@register
            val sign = (screen as AbstractSignEditScreenAccessor).sign
            val lines = Array(4) { sign.frontText.getMessage(it, false).string }
            if (lines[1] != "^^^^^^^^^^^^^^^" || lines[2] != "Your auction" || lines[3] != "starting bid") return@register

            // Set the field directly: setScreen would close the sign and send its empty text
            mc.execute { mc.screen = InputScreen(sign, lines, stack).apply { init(width, height) } }
        }
    }

    // "10m", "2.5k", "1,000,000"
    private fun parseCompactNumber(text: String): Long? {
        val clean = text.lowercase().replace(",", "").trim()
        if (clean.isEmpty()) return null
        clean.toLongOrNull()?.let { return it }
        val multiplier = suffixes[clean.last()] ?: return null
        return clean.dropLast(1).toDoubleOrNull()?.let { (it * multiplier).toLong() }
    }

    private class InputScreen(
        private val sign: SignBlockEntity,
        private val originalLines: Array<String>,
        private val stack: ItemStack
    ) : Screen(Component.literal("Auction Price Input")) {
        private lateinit var inputField: EditBox

        override fun init() {
            super.init()
            Prices.refresh()
            if (!rememberText) input = ""
            if (!rememberMode || undercut == null) undercut = defaultMode == 1

            val centerX = width / 2
            val centerY = height / 2

            inputField = EditBox(font, centerX - 100, centerY - 20, 200, 20, Component.literal("Price"))
            inputField.setMaxLength(32)
            inputField.value = input
            inputField.setResponder { input = it }
            addRenderableWidget(inputField)
            setInitialFocus(inputField)

            addRenderableWidget(Button.builder(Component.literal("Done")) { finish() }
                .bounds(centerX - 100, centerY + 10, 200, 20).build())

            addRenderableWidget(Button.builder(modeText()) { button ->
                undercut = undercut != true
                button.message = modeText()
            }.bounds(centerX - 100, centerY + 35, 200, 20).build())
        }

        private fun modeText() = Component.literal("Mode: ${if (undercut == true) "Undercut" else "Normal"}")

        private fun lowestBin() = Prices.lowestBin(Prices.skyblockId(stack))?.toLong() ?: 0L

        private fun value(): Long? {
            val typed = parseCompactNumber(input) ?: return null
            return if (undercut == true) (lowestBin() - typed).coerceAtLeast(0) else typed
        }

        override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
            val centerX = width / 2
            val centerY = height / 2

            val itemX = centerX - 8
            val itemY = centerY - 75
            guiGraphics.item(stack, itemX, itemY)
            guiGraphics.itemDecorations(font, stack, itemX, itemY)
            if (mouseX in itemX..itemX + 16 && mouseY in itemY..itemY + 16) guiGraphics.setTooltipForNextFrame(font, stack, mouseX, mouseY)

            val orange = 0xFFFFAA00.toInt()
            guiGraphics.centeredText(font, if (undercut == true) "Undercut Mode" else "Set Auction Price", centerX, centerY - 55, orange)
            guiGraphics.centeredText(font, "Lowest BIN: ${formatNumber(lowestBin().toString())}", centerX, centerY - 45, orange)

            val value = value()
            val status = when {
                value != null -> "§aValue: §e${"%,d".format(value)}"
                input.isEmpty() -> "§7Enter a value (e.g. 10m, 5k)"
                else -> "§cInvalid format"
            }
            guiGraphics.centeredText(font, status, centerX, centerY - 33, -1)

            super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        }

        override fun keyPressed(event: KeyEvent): Boolean {
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                finish()
                return true
            }
            return super.keyPressed(event)
        }

        private fun finish() {
            val line = value()?.toString() ?: input
            mc.connection?.send(ServerboundSignUpdatePacket(sign.blockPos, true, line, originalLines[1], originalLines[2], originalLines[3]))
            onClose()
        }

        override fun isPauseScreen(): Boolean = false
    }
}
