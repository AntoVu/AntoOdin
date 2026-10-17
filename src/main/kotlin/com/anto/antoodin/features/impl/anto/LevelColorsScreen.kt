package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.features.impl.anto.LevelColors.LevelRule
import com.anto.antoodin.features.impl.anto.LevelColors.Mode
import com.odtheking.odin.OdinMod.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

private const val PREVIEW_WIDTH = 30

// Vanilla's 16 chat colors, each name drawn in its own color
internal val MINECRAFT_COLORS = "§lMinecraft colors\n" + listOf(
    "0" to "Black #000000", "1" to "Dark Blue #0000AA", "2" to "Dark Green #00AA00", "3" to "Dark Aqua #00AAAA",
    "4" to "Dark Red #AA0000", "5" to "Dark Purple #AA00AA", "6" to "Gold #FFAA00", "7" to "Gray #AAAAAA",
    "8" to "Dark Gray #555555", "9" to "Blue #5555FF", "a" to "Green #55FF55", "b" to "Aqua #55FFFF",
    "c" to "Red #FF5555", "d" to "Light Purple #FF55FF", "e" to "Yellow #FFFF55", "f" to "White #FFFFFF"
).joinToString("\n") { (code, text) -> "§$code${text.substringBeforeLast(' ')} §7${text.substringAfterLast(' ')}" }

/** The prestige list: range, preview, on/off, edit, and delete for custom ranges. */
class LevelColorsScreen(private val previous: Screen?) : Screen(Component.literal("Level Colors")) {
    private lateinit var list: RowList

    override fun init() {
        super.init()
        val buttonWidth = 100
        val left = width / 2 - (buttonWidth * 2 + GAP) / 2
        addRenderableWidget(Button.builder(Component.literal("Add Range")) {
            LevelColors.rules.add(LevelRule())
            LevelColors.save()
            mc.gui.setScreen(LevelColorEditScreen(LevelColors.rules.last(), previous))
        }.bounds(left, 22, buttonWidth, 20).build())

        addRenderableWidget(Button.builder(enabledText()) { button ->
            LevelColors.toggle()
            button.message = enabledText()
        }.bounds(left + buttonWidth + GAP, 22, buttonWidth, 20).build())

        list = RowList(width, height - 56, 46, 340)
        addRenderableWidget(list)
        rebuildRows()
    }

    private fun enabledText() = Component.literal(if (LevelColors.enabled) "§aModule Enabled" else "§cModule Disabled")

    private fun rebuildRows() {
        val scroll = list.scrollAmount()
        list.show(LevelColors.rules.map { rule ->
            val preview = PreviewWidget(previewText(rule))
            val toggle = onOff(rule.enabled, if (rule.builtIn) "On uses your colors, off keeps Hypixel's." else "Turns this range on or off.") {
                rule.enabled = it
                preview.message = previewText(rule)
                LevelColors.rebuild()
                // A custom range changes where the prestiges it overlaps end
                if (!rule.builtIn) rebuildRows()
            }.also { it.width = 40 }
            val edit = Button.builder(Component.literal("Edit")) {
                LevelColors.save()
                mc.gui.setScreen(LevelColorEditScreen(rule, previous))
            }.width(40).build()
            val delete = Button.builder(Component.literal("§cDelete")) {
                LevelColors.rules.remove(rule)
                LevelColors.save()
                rebuildRows()
            }.width(46).build()
            delete.active = !rule.builtIn
            if (rule.builtIn) delete.setTooltip(Tooltip.create(Component.literal("Hypixel's prestiges can't be deleted. Turn them off instead.")))
            Row(rangeText(rule), null, null, listOf(preview, toggle, edit, delete))
        })
        list.setScrollAmount(scroll)
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        guiGraphics.centeredText(font, "§lLevel Colors §r§7(custom ranges win over Hypixel's)", width / 2, 8, -1)
    }

    override fun onClose() {
        LevelColors.save()
        mc.gui.setScreen(previous)
    }

    override fun isPauseScreen(): Boolean = false
}

/** Every option of one range. Esc goes back to the list. */
class LevelColorEditScreen(private val rule: LevelRule, private val listPrevious: Screen?) : Screen(Component.literal("Level Color")) {
    private lateinit var preview: PreviewWidget

    override fun init() {
        super.init()
        val list = RowList(width, height - 56, 26, 340)
        addRenderableWidget(list)
        preview = PreviewWidget(previewText(rule))

        // Hex boxes turn red while the text isn't a valid color
        val bracket = textBox(rule.bracket, "Color of [ and ], e.g. #000000. Leave blank to keep Hypixel's gray.") { }
        fun colorBracket(text: String) = bracket.setTextColor(if (text.isBlank() || LevelColors.parseHex(text) != null) WHITE else RED)
        colorBracket(rule.bracket)
        bracket.setResponder { rule.bracket = it; colorBracket(it); changed() }

        val colors = textBox(rule.colors, "Digit colors, separated by commas, e.g. #FF5555, #5555FF.") { }
        fun colorColors(text: String) = colors.setTextColor(if (LevelColors.parseColors(text) != null) WHITE else RED)
        colorColors(rule.colors)
        colors.setResponder { rule.colors = it; colorColors(it); changed() }

        val rows = mutableListOf<Row>()
        if (!rule.builtIn) rows += Row("Levels", "The first and last level of this range.", null, listOf(
            intBox(rule.from, "From") { rule.from = it.coerceAtMost(LevelColors.MAX_LEVEL); changed() },
            // Blank means no upper end (shown as 520+)
            numberBox(if (rule.to >= LevelColors.MAX_LEVEL) "" else rule.to.toString(), "To (leave blank for no limit)", 36) { s ->
                val to = if (s.isBlank()) LevelColors.MAX_LEVEL else s.toIntOrNull()?.takeIf { it >= 0 } ?: return@numberBox false
                rule.to = to.coerceAtMost(LevelColors.MAX_LEVEL)
                changed()
                true
            }
        ))
        rows += Row("Enabled", null, null, listOf(onOff(rule.enabled, "Off keeps Hypixel's colors for these levels.") { rule.enabled = it; changed() }))
        rows += Row("Bracket Color", null, bracket, listOf(infoButton(MINECRAFT_COLORS)))
        rows += Row("Digit Colors", null, colors, listOf(infoButton(MINECRAFT_COLORS)))
        rows += Row("Mode", "How the digit colors are used.", null, listOf(
            CycleButton.builder<Mode>({ Component.literal(it.label) }, rule.mode)
                .withValues(Mode.entries)
                .withTooltip { Tooltip.create(Component.literal(it.tip)) }
                .displayOnlyValue()
                .create(0, 0, 100, 20, Component.literal("Mode")) { _, value -> rule.mode = value; changed() }
        ))
        rows += Row("Preview", null, null, listOf(preview))
        list.show(rows)
    }

    private fun changed() {
        preview.message = previewText(rule)
        LevelColors.rebuild()
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        guiGraphics.centeredText(font, "§lLevels ${rangeText(rule)}", width / 2, 8, -1)
    }

    override fun onClose() {
        LevelColors.save()
        mc.gui.setScreen(LevelColorsScreen(listPrevious))
    }

    override fun isPauseScreen(): Boolean = false
}

// A Hypixel prestige ends where the first enabled custom range inside it starts, since custom ranges win
private fun rangeText(rule: LevelRule): String {
    val end = if (!rule.builtIn) rule.to else LevelColors.rules
        .filter { !it.builtIn && it.enabled && it.from in rule.from + 1..rule.to }
        .minOfOrNull { it.from - 1 } ?: rule.to
    return if (end >= LevelColors.MAX_LEVEL) "${rule.from}+" else "${rule.from}–$end"
}

// Fixed width, so the row doesn't shift when the text changes
private class PreviewWidget(text: Component) : StringWidget(PREVIEW_WIDTH, 20, text, mc.font) {
    override fun getWidth(): Int = PREVIEW_WIDTH
}

// The range's first level, or its last when the first has fewer than three digits (for gradients)
private fun previewText(rule: LevelRule): Component {
    val level = if (rule.from >= 100) rule.from else rule.to.coerceIn(rule.from, 621)
    return if (rule.enabled) LevelColors.preview(rule, level) else LevelColors.hypixelPreview(level)
}
