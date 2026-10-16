// Feature idea from NoFrills (GPL-3.0). Written from scratch for AntoOdin, no NoFrills code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.loreString
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Chronomatron and Ultrasequencer: marks the next slots to click and blocks wrong clicks.
 * Superpairs: keeps showing rewards after they're covered again and highlights pairs and powerups.
 *
 * Slots are compared every frame rather than on Odin's SetSlotEvent, which misses the full-menu packets that start a round.
 */
object ExperimentSolver : Module(
    name = "Experiment Solver",
    description = "Solves the Experimentation Table games and blocks wrong clicks.",
    category = Skit.ANTO
) {
    private val chronomatron by BooleanSetting("Chronomatron", true, desc = "Marks the slots to click in Chronomatron.")
    private val ultrasequencer by BooleanSetting("Ultrasequencer", true, desc = "Marks the order to click in Ultrasequencer.")
    private val superpairs by BooleanSetting("Superpairs", true, desc = "Remembers uncovered Superpairs rewards and highlights pairs.")
    private val nextColor by ColorSetting("Next Color", Color(85, 255, 85, 0.8f), true, desc = "The slot to click now.").withDependency { chronomatron || ultrasequencer }
    private val afterColor by ColorSetting("After Color", Color(255, 170, 0, 0.8f), true, desc = "The slot to click after that.").withDependency { chronomatron || ultrasequencer }
    private val pairColor by ColorSetting("Pair Color", Color(255, 255, 85, 0.5f), true, desc = "Superpairs rewards whose match has been seen.").withDependency { superpairs }
    private val powerupColor by ColorSetting("Powerup Color", Color(255, 85, 255, 0.5f), true, desc = "Superpairs powerups.").withDependency { superpairs }

    private enum class Game(val prefix: String) { Chronomatron("Chronomatron ("), Ultrasequencer("Ultrasequencer ("), Superpairs("Superpairs (") }

    // Status item in the middle of the bottom row: glowstone while the game shows, a clock on your turn
    private const val STATUS_SLOT = 49
    private const val REST_COLOR = 0xCC404040.toInt()

    private var seen = arrayOfNulls<ItemStack>(54)
    private var showing = true

    // Chronomatron: each step is the group of slots lit together
    private val steps = ArrayDeque<MutableSet<Int>>()
    private var stepOpen = false

    // Ultrasequencer: slot -> number while showing, then the click order
    private val numbers = HashMap<Int, Int>()
    private val order = ArrayDeque<Int>()

    // Superpairs: rewards seen under each slot
    private val rewards = HashMap<Int, ItemStack>()

    init {
        on<ScreenEvent.Open> { reset() }

        on<GuiEvent.Render> {
            val game = game(screen) ?: return@on
            val slots = (screen as AbstractContainerScreen<*>).menu.slots
            val size = minOf(54, slots.size - 36)
            // The status slot first, so the phase is current for the other changes this frame
            for (i in listOf(STATUS_SLOT) + (0 until size).filter { it != STATUS_SLOT }) {
                if (i >= size) continue
                val stack = slots[i].item
                val old = seen[i]
                if (old != null && ItemStack.matches(old, stack)) continue
                seen[i] = stack.copy()
                onChange(game, i, stack)
            }
        }

        on<GuiEvent.SlotClick> {
            val game = game(screen) ?: return@on
            if (slotIndex !in 0 until 54 || game == Game.Superpairs) return@on
            if (wrongClick(game, slotIndex)) cancel()
        }

        on<GuiEvent.RenderSlot> {
            val game = game(screen) ?: return@on
            if (slot.index >= 54) return@on
            val x = slot.x
            val y = slot.y
            when (game) {
                Game.Chronomatron -> {
                    if (showing) return@on
                    val color = when (slot.index) {
                        in steps.firstOrNull().orEmpty() -> nextColor.rgba
                        in steps.getOrNull(1).orEmpty() -> afterColor.rgba
                        else -> return@on
                    }
                    guiGraphics.fill(x, y, x + 16, y + 16, color)
                    cancel()
                }
                Game.Ultrasequencer -> {
                    if (showing) return@on
                    val place = order.indexOf(slot.index).takeIf { it >= 0 } ?: return@on
                    guiGraphics.fill(x, y, x + 16, y + 16, when (place) { 0 -> nextColor.rgba; 1 -> afterColor.rgba; else -> REST_COLOR })
                    val text = numbers[slot.index].toString()
                    guiGraphics.text(mc.font, text, x + 9 - mc.font.width(text) / 2, y + 4, -1, true)
                    cancel()
                }
                Game.Superpairs -> {
                    val reward = rewards[slot.index] ?: return@on
                    val color = when {
                        isPowerup(reward) -> powerupColor.rgba
                        rewards.any { (i, other) -> i != slot.index && samePrize(reward, other) } -> pairColor.rgba
                        else -> null
                    }
                    color?.let { guiGraphics.fill(x, y, x + 16, y + 16, it) }
                    // Covered again: draw the remembered reward instead of the cover
                    if (isCover(slot.item)) {
                        guiGraphics.item(reward, x, y)
                        guiGraphics.itemDecorations(mc.font, reward, x, y)
                        cancel()
                    }
                }
            }
        }
    }

    private fun game(screen: Screen): Game? {
        if (screen !is AbstractContainerScreen<*>) return null
        val title = screen.title.string
        return Game.entries.firstOrNull { title.startsWith(it.prefix) }?.takeIf {
            when (it) {
                Game.Chronomatron -> chronomatron
                Game.Ultrasequencer -> ultrasequencer
                Game.Superpairs -> superpairs
            }
        }
    }

    private fun onChange(game: Game, slot: Int, stack: ItemStack) {
        if (slot == STATUS_SLOT && game != Game.Superpairs) {
            when (stack.item) {
                Items.GLOWSTONE -> if (!showing) {
                    // A new round replays the whole sequence
                    showing = true
                    steps.clear()
                    stepOpen = false
                    numbers.clear()
                    order.clear()
                }
                Items.CLOCK -> if (showing) {
                    showing = false
                    stepOpen = false
                    order.clear()
                    order.addAll(numbers.entries.sortedBy { it.value }.map { it.key })
                }
            }
            return
        }

        when (game) {
            Game.Chronomatron -> if (showing) when {
                path(stack).endsWith("terracotta") -> {
                    if (!stepOpen) steps.addLast(mutableSetOf())
                    stepOpen = true
                    steps.last().add(slot)
                }
                path(stack).endsWith("stained_glass") && stepOpen && slot in steps.last() -> stepOpen = false
            }
            Game.Ultrasequencer -> if (showing && isDye(stack)) numbers[slot] = stack.count
            Game.Superpairs -> if (!stack.isEmpty && !isCover(stack) && !isStatus(stack)) rewards[slot] = stack.copy()
        }
    }

    // Also moves the solution on when the click is the right one
    private fun wrongClick(game: Game, slot: Int): Boolean {
        if (showing) return true
        return when (game) {
            Game.Chronomatron -> {
                val next = steps.firstOrNull() ?: return false
                if (slot in next) { steps.removeFirst(); false } else steps.any { slot in it }
            }
            Game.Ultrasequencer -> {
                val next = order.firstOrNull() ?: return false
                if (slot == next) { order.removeFirst(); false } else slot in numbers
            }
            Game.Superpairs -> false
        }
    }

    private fun reset() {
        seen = arrayOfNulls(54)
        showing = true
        steps.clear()
        stepOpen = false
        numbers.clear()
        order.clear()
        rewards.clear()
    }

    private fun path(stack: ItemStack) = BuiltInRegistries.ITEM.getKey(stack.item).path

    private fun isDye(stack: ItemStack) =
        path(stack).endsWith("_dye") || stack.item in listOf(Items.INK_SAC, Items.BONE_MEAL, Items.LAPIS_LAZULI, Items.COCOA_BEANS)

    private fun isCover(stack: ItemStack) = path(stack).let { it.endsWith("stained_glass") || it.endsWith("stained_glass_pane") }

    private fun isStatus(stack: ItemStack) = when (stack.item) {
        Items.CLOCK, Items.BOOKSHELF, Items.CAULDRON -> true
        Items.GLOWSTONE -> stack.hoverName.string.noControlCodes != "Enchanted Book"
        else -> false
    }

    private fun isPowerup(stack: ItemStack) = stack.loreString.any { "powerup" in it.lowercase() }

    // Heads share an item and often a name, so their skin has to match too
    private fun samePrize(a: ItemStack, b: ItemStack) =
        a.item == b.item && a.count == b.count && a.hoverName.string == b.hoverName.string &&
            a.get(DataComponents.PROFILE) == b.get(DataComponents.PROFILE)
}
