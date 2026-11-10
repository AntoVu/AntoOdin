package com.anto.antoodin.features.impl.anto

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Per-class Fast Leap targets: one row per phase, class letter (A/B/M/H/T) or player name; Door Opener is on/off. */
class FastLeapScreen(private val previous: Screen?, private val clazz: DungeonClass = DungeonClass.ARCHER) :
    Screen(Component.literal("Fast Leap")) {

    private val phases = FastLeap.Phase.entries
    private val rows = (phases.size + 1) / 2

    private fun columnX(col: Int) = width / 2 - COLUMN_WIDTH - 10 + col * (COLUMN_WIDTH + 20)
    private fun rowY(row: Int) = 52 + row * 22

    override fun init() {
        super.init()
        val tabX = width / 2 - FastLeap.classes.size * 42
        FastLeap.classes.forEachIndexed { i, c ->
            addRenderableWidget(Button.builder(Component.literal(c.name.lowercase().replaceFirstChar(Char::uppercase))) {
                mc.gui.setScreen(FastLeapScreen(previous, c))
            }.bounds(tabX + i * 84, 22, 80, 20).build()).active = c != clazz
        }

        phases.forEachIndexed { i, phase ->
            val x = columnX(i / rows) + COLUMN_WIDTH - FIELD_WIDTH
            val y = rowY(i % rows)
            if (phase == FastLeap.Phase.DOOR_OPENER) {
                addRenderableWidget(Button.builder(doorText()) { button ->
                    FastLeap.set(clazz, phase, if (FastLeap.get(clazz, phase).isBlank()) "on" else "")
                    button.message = doorText()
                }.bounds(x, y, FIELD_WIDTH, 18).build())
            } else {
                val box = EditBox(font, x, y, FIELD_WIDTH, 18, Component.literal(phase.label))
                box.setMaxLength(16)
                box.setHint(Component.literal(if (phase.name.startsWith("DURING")) "Letter/name/@melody" else "Letter or name"))
                box.value = FastLeap.get(clazz, phase)
                box.setResponder { FastLeap.set(clazz, phase, it) }
                addRenderableWidget(box)
            }
        }

        addRenderableWidget(Button.builder(Component.literal("Done")) { onClose() }
            .bounds(width / 2 - 50, rowY(rows) + 6, 100, 20).build())
    }

    private fun doorText() = Component.literal(if (FastLeap.get(clazz, FastLeap.Phase.DOOR_OPENER).isBlank()) "Off" else "On")

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        guiGraphics.centeredText(font, "Fast Leap for ${clazz.name.lowercase()}: A/B/M/H/T, player name or @melody (During S#). Blank = off", width / 2, 8, -1)
        phases.forEachIndexed { i, phase ->
            guiGraphics.text(font, phase.label, columnX(i / rows), rowY(i % rows) + 5, -1)
        }
    }

    override fun onClose() {
        FastLeap.save()
        mc.gui.setScreen(previous)
    }

    override fun isPauseScreen(): Boolean = false

    private companion object {
        const val COLUMN_WIDTH = 224
        const val FIELD_WIDTH = 116
    }
}
