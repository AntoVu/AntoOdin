package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Prices
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.client.gui.screens.inventory.InventoryScreen

object HighlightTrash : Module(
    name = "Highlight Trash",
    description = "Highlights junk M7 chest books in your inventory while in the Dungeon Hub.",
    category = Skit.ANTO
) {
    private val color by ColorSetting("Color", Color(255, 0, 0, 0.5f), true, desc = "Color of the highlight.")

    // Price ids from Prices.skyblockId
    private val TRASH = setOf(
        "FEATHER_FALLING_6", "FEATHER_FALLING_7", "INFINITE_QUIVER_6", "INFINITE_QUIVER_7",
        "ULTIMATE_COMBO_1", "ULTIMATE_COMBO_2", "ULTIMATE_JERRY_1", "ULTIMATE_JERRY_2", "ULTIMATE_JERRY_3",
        "ULTIMATE_BANK_1", "ULTIMATE_BANK_2", "ULTIMATE_BANK_3", "ULTIMATE_NO_PAIN_NO_GAIN_1", "ULTIMATE_NO_PAIN_NO_GAIN_2"
    ).mapTo(HashSet()) { "ENCHANTMENT_$it" }

    init {
        // RenderSlot fires before the item draws, so the fill sits under it
        on<GuiEvent.RenderSlot> {
            if (screen !is InventoryScreen || LocationUtils.currentArea != Island.DungeonHub) return@on
            if (Prices.skyblockId(slot.item) in TRASH) guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color.rgba)
        }
    }
}
