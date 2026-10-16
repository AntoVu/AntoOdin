package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.features.impl.anto.ChatRules.ChatRule
import com.anto.antoodin.features.impl.anto.ChatRules.MatchType
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.modMessage
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

private const val LABEL_WIDTH = 90
private const val GAP = 4
private const val RED = 0xFFFF5555.toInt()
private const val GREEN = 0xFF55FF55.toInt()
private const val WHITE = 0xFFE0E0E0.toInt()

/** The rule list: name, on/off, edit and delete per rule. */
class ChatRulesScreen(private val previous: Screen?) : Screen(Component.literal("Chat Rules")) {
    private lateinit var list: RowList

    override fun init() {
        super.init()
        val buttonWidth = 100
        val left = width / 2 - (buttonWidth * 3 + GAP * 2) / 2
        addRenderableWidget(Button.builder(Component.literal("Add Rule")) {
            ChatRules.rules.add(ChatRule())
            ChatRules.save()
            mc.gui.setScreen(ChatRuleEditScreen(ChatRules.rules.last(), previous))
        }.bounds(left, 22, buttonWidth, 20).build())

        addRenderableWidget(Button.builder(Component.literal("Import NoFrills")) {
            modMessage(ChatRules.importNoFrills())
            rebuildRows()
        }.bounds(left + buttonWidth + GAP, 22, buttonWidth, 20)
            .tooltip(Tooltip.create(Component.literal("Adds the chat rules from NoFrills' config in this instance. Rules already here are skipped.")))
            .build())

        // Rules only run while the module is on, so the switch lives here too
        addRenderableWidget(Button.builder(enabledText()) { button ->
            ChatRules.toggle()
            button.message = enabledText()
        }.bounds(left + (buttonWidth + GAP) * 2, 22, buttonWidth, 20).build())

        list = RowList(width, height - 56, 46, 380)
        addRenderableWidget(list)
        rebuildRows()
    }

    private fun enabledText() = Component.literal(if (ChatRules.enabled) "§aModule Enabled" else "§cModule Disabled")

    private fun rebuildRows() {
        list.show(ChatRules.rules.map { rule ->
            val name = textBox(rule.name, "The name of this rule.") { rule.name = it }
            val toggle = onOff(rule.enabled, "Turns this rule on or off.") { rule.enabled = it }
            val edit = Button.builder(Component.literal("Edit")) {
                ChatRules.save()
                mc.gui.setScreen(ChatRuleEditScreen(rule, previous))
            }.width(40).build()
            val delete = Button.builder(Component.literal("§cDelete")) {
                ChatRules.rules.remove(rule)
                ChatRules.save()
                rebuildRows()
            }.width(46).build()
            Row(null, null, name, listOf(toggle.also { it.width = 40 }, edit, delete))
        })
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        guiGraphics.centeredText(font, "§lChat Rules §r§7(${ChatRules.rules.size}, first match wins)", width / 2, 8, -1)
        if (ChatRules.rules.isEmpty()) guiGraphics.centeredText(font, "§7No rules yet.", width / 2, 60, -1)
    }

    override fun onClose() {
        ChatRules.save()
        mc.gui.setScreen(previous)
    }

    override fun isPauseScreen(): Boolean = false
}

/** Every option of one rule. Esc goes back to the rule list. */
class ChatRuleEditScreen(private val rule: ChatRule, private val listPrevious: Screen?) :
    Screen(Component.literal("Chat Rule")) {

    override fun init() {
        super.init()
        val list = RowList(width, height - 56, 26, 420)
        addRenderableWidget(list)

        val sound = textBox(rule.sound, "Sound id to play, e.g. entity.experience_orb.pickup. Leave blank for none.") { rule.sound = it }
        fun colorSound(value: String) = sound.setTextColor(when {
            value.isBlank() -> WHITE
            Identifier.tryParse(value.trim())?.let(mc.soundManager::getSoundEvent) != null -> GREEN
            else -> RED
        })
        colorSound(rule.sound)
        sound.setResponder { rule.sound = it; colorSound(it) }

        list.show(listOf(
            Row("Name", "The name of this rule.", textBox(rule.name, "The name of this rule.") { rule.name = it }),
            Row("Match Text", "The text (or regex) to match.", textBox(rule.match, "The text (or regex) to match.") { rule.match = it }),
            Row("Match Type", "How the text is compared to the message.", null, listOf(
                CycleButton.builder<MatchType>({ Component.literal(it.name) }, rule.matchType)
                    .withValues(MatchType.entries)
                    .withTooltip { Tooltip.create(Component.literal(it.tip)) }
                    .displayOnlyValue()
                    .create(0, 0, 100, 20, Component.literal("Match Type")) { _, value -> rule.matchType = value }
            )),
            Row("Case Sensitive", "Whether upper and lower case must match.", null, listOf(
                onOff(rule.caseSensitive, "Whether upper and lower case must match.") { rule.caseSensitive = it }
            )),
            Row("Cancel Message", "Hides the message from chat. Other mods still see it.", null, listOf(
                onOff(rule.cancel, "Hides the message from chat. Other mods still see it.") { rule.cancel = it }
            )),
            Row("Class Filter", null, textBox(rule.classFilter,
                "Dungeon classes this rule works for, e.g. \"mage archer\". Leave blank for any class.") { rule.classFilter = it }),
            Row("Island Filter", null, textBox(rule.islandFilter,
                "Islands this rule works on, e.g. \"catacombs, crimson isle\". Any separator works. Leave blank for anywhere.") { rule.islandFilter = it },
                listOf(infoButton("§lIsland names\n§7" + ChatRules.islands.joinToString("\n") { it.displayName }))),
            Row("Title", null, textBox(rule.title, "Title to show. Use & for color codes. Leave blank for none.") { rule.title = it }, listOf(
                intBox(rule.titleFadeIn, "Fade in (ticks)") { rule.titleFadeIn = it },
                intBox(rule.titleStay, "Stay (ticks)") { rule.titleStay = it },
                intBox(rule.titleFadeOut, "Fade out (ticks)") { rule.titleFadeOut = it }
            )),
            Row("Custom Title", null, textBox(rule.customTitle,
                "Shown in the Custom Title HUD, so it doesn't replace other titles. Use & for color codes.") { rule.customTitle = it }, listOf(
                intBox(rule.customTitleStay, "Stay (ticks)") { rule.customTitleStay = it }
            )),
            Row("Sound", null, sound, listOf(
                floatBox(rule.soundVolume, "Volume") { rule.soundVolume = it },
                floatBox(rule.soundPitch, "Pitch") { rule.soundPitch = it },
                Button.builder(Component.literal("Test")) { ChatRules.testSound(rule) }.width(32).build()
            )),
            Row("Command", null, textBox(rule.command,
                "Command to run, e.g. /pc hi. Commands from other mods work too. Leave blank for none.") { rule.command = it }, listOf(
                intBox(rule.commandDelay, "Delay (ms)") { rule.commandDelay = it },
                intBox(rule.commandDelayVariety, "Random extra delay (ms)") { rule.commandDelayVariety = it }
            ))
        ))
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        guiGraphics.centeredText(font, "§lChat Rule: §r${rule.name}", width / 2, 8, -1)
    }

    override fun onClose() {
        ChatRules.save()
        mc.gui.setScreen(ChatRulesScreen(listPrevious))
    }

    override fun isPauseScreen(): Boolean = false
}

private class RowList(width: Int, height: Int, y: Int, private val rowWidth: Int) :
    ContainerObjectSelectionList<Row>(mc, width, height, y, 24) {

    fun show(rows: List<Row>) = replaceEntries(rows)

    override fun getRowWidth(): Int = minOf(rowWidth, width - 20)

    // Keeps the scrollbar inside the list so it can be dragged (see SoundManagerScreen)
    override fun scrollBarX(): Int = minOf(super.scrollBarX(), right - scrollbarWidth())
}

/** A label, then [flex] filling the space left of the [fixed] widgets. */
private class Row(
    private val label: String?,
    private val tip: String?,
    private val flex: AbstractWidget?,
    private val fixed: List<AbstractWidget> = emptyList()
) : ContainerObjectSelectionList.Entry<Row>() {
    private val widgets = listOfNotNull(flex) + fixed

    override fun extractContent(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, delta: Float) {
        val start = contentX + if (label != null) LABEL_WIDTH else 0
        var x = contentRight
        for (widget in fixed.asReversed()) {
            x -= widget.width
            widget.setPosition(x, contentYMiddle - widget.height / 2)
            x -= GAP
        }
        flex?.let {
            it.width = (x - start).coerceAtLeast(20)
            it.setPosition(start, contentYMiddle - it.height / 2)
        }
        widgets.forEach { it.extractRenderState(guiGraphics, mouseX, mouseY, delta) }

        if (label != null) {
            guiGraphics.text(mc.font, label, contentX, contentYMiddle - 4, -1)
            if (tip != null && hovered && mouseX < start) guiGraphics.setTooltipForNextFrame(mc.font, Component.literal(tip), mouseX, mouseY)
        }
    }

    override fun children(): List<GuiEventListener> = widgets

    override fun narratables(): List<NarratableEntry> = widgets
}

// A small "?" whose tooltip holds reference text
private fun infoButton(text: String) = Button.builder(Component.literal("§b?")) {}
    .width(20)
    .tooltip(Tooltip.create(Component.literal(text)))
    .build()

private fun textBox(value: String, tip: String, onChange: (String) -> Unit) =
    EditBox(mc.font, 0, 0, 100, 18, Component.literal(tip)).apply {
        setMaxLength(512)
        this.value = value
        // setValue scrolls to the end at the placeholder width; the row widens the box later, so start from the left
        moveCursorToStart(false)
        setTooltip(Tooltip.create(Component.literal(tip)))
        setResponder(onChange)
    }

private fun onOff(value: Boolean, tip: String, onChange: (Boolean) -> Unit): CycleButton<Boolean> =
    CycleButton.onOffBuilder(value)
        .displayOnlyValue()
        .withTooltip { Tooltip.create(Component.literal(tip)) }
        .create(0, 0, 50, 20, Component.literal(tip)) { _, it -> onChange(it) }

// Number boxes keep the last valid value and turn red while the text isn't a number
private fun numberBox(text: String, tip: String, width: Int, apply: (String) -> Boolean) =
    EditBox(mc.font, 0, 0, width, 18, Component.literal(tip)).apply {
        setMaxLength(8)
        value = text
        setTooltip(Tooltip.create(Component.literal(tip)))
        setResponder { setTextColor(if (apply(it)) WHITE else RED) }
    }

private fun intBox(value: Int, tip: String, onChange: (Int) -> Unit) =
    numberBox(value.toString(), tip, 36) { s -> s.toIntOrNull()?.takeIf { it >= 0 }?.also(onChange) != null }

private fun floatBox(value: Float, tip: String, onChange: (Float) -> Unit) =
    numberBox(value.toString(), tip, 32) { s -> s.toFloatOrNull()?.takeIf { it >= 0f }?.also(onChange) != null }
