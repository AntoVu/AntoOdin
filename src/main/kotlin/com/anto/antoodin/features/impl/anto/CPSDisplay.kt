package com.anto.antoodin.features.impl.anto

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.render.textDim
import com.anto.antoodin.utils.Skit
import net.minecraft.client.gui.GuiGraphics

object CPSDisplay : Module(
    name = "CPS Display",
    description = "Displays your clicks per second.",
    category = Skit.ANTO
) {
    private val advanced by DropdownSetting("Show Settings", false)
    private val button by SelectorSetting(
        "Button",
        "Both",
        listOf("Left", "Right", "Both"),
        desc = "The button to display the CPS of."
    ).withDependency { advanced }
    private val mouseText by BooleanSetting("Show Button", true, desc = "Shows the button name.").withDependency { advanced }
    private val nameColor by ColorSetting("Name Color", Color(50, 150, 220), desc = "The color of the label.")
    private val valueColor by ColorSetting("Value Color", Colors.WHITE, desc = "The color of the CPS value.")

    private val leftClicks = mutableListOf<Long>()
    private val rightClicks = mutableListOf<Long>()

    private const val LEFT = 0
    private const val RIGHT = 1
    private const val BOTH = 2

    fun onLeftClick() {
        if (!enabled) return
        leftClicks.add(System.currentTimeMillis())
    }

    fun onRightClick() {
        if (!enabled) return
        rightClicks.add(System.currentTimeMillis())
    }

    private val hud by HUD(name, "Displays your clicks per second.", false) {
        val now = System.currentTimeMillis()
        leftClicks.removeAll { now - it > 1000 }
        rightClicks.removeAll { now - it > 1000 }

        val lineHeight = mc.font.lineHeight
        var width = 1

        fun GuiGraphics.renderCPS(label: String, count: Int) {
            val w = drawCPSText(if (mouseText) label else "", "$count ", width, 1)
            width += w
        }

        when (button) {
            LEFT -> renderCPS("LMB: ", leftClicks.size)
            RIGHT -> renderCPS("RMB: ", rightClicks.size)
            BOTH -> {
                renderCPS("LMB: ", leftClicks.size)
                renderCPS("RMB: ", rightClicks.size)
            }
        }

        width to lineHeight
    }

    private fun GuiGraphics.drawCPSText(label: String, value: String, x: Int, y: Int): Int {
        var width = 0
        if (label.isNotEmpty()) width += textDim(label, x, y, nameColor, true).first
        width += textDim(value, x + width, y, valueColor, true).first
        return width
    }
}