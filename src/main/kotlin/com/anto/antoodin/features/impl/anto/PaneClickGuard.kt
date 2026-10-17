package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.terminals.TerminalUtils
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.registries.BuiltInRegistries

object PaneClickGuard : Module(
    name = "Pane Click Guard",
    description = "Ignores clicks on the blank glass panes that fill GUIs, so nothing is sent to the server.",
    category = Skit.ANTO
) {
    // Experiment games where covers are clickable
    private val exempt = listOf("Chronomatron (", "Ultrasequencer (", "Superpairs (")

    init {
        on<GuiEvent.SlotClick> {
            // Terminal panes are blank too; Odin sets currentTerm for every terminal, solver on or off
            if (TerminalUtils.currentTerm != null) return@on
            val screen = screen as? AbstractContainerScreen<*> ?: return@on
            if (exempt.any { screen.title.string.startsWith(it) }) return@on
            val stack = screen.menu.slots.getOrNull(slotIndex)?.item ?: return@on
            if (BuiltInRegistries.ITEM.getKey(stack.item).path.endsWith("glass_pane") && stack.hoverName.string.isBlank()) cancel()
        }
    }
}
