// UI rebuilt with vanilla widgets from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.playSoundAtPlayer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

class SoundManagerScreen(private val previous: Screen?) : Screen(Component.literal("Sound Manager")) {

    private enum class Tab(val label: String, val prefix: String? = null) {
        All("All"), Recent("Recent"), Changed("Changed"), Blocks("Blocks", "block"), Entities("Entities", "entity"),
        Items("Items", "item"), Music("Music", "music"), Ambient("Ambient", "ambient"), UI("UI", "ui"), Misc("Misc")
    }

    private val allSounds = BuiltInRegistries.SOUND_EVENT.keySet().sortedBy(Identifier::toString)
    private val prefixes = Tab.entries.mapNotNull { it.prefix }

    private var tab = Tab.All
    private var search = ""
    private lateinit var list: SoundList

    override fun init() {
        super.init()
        val listX = SIDEBAR_WIDTH + 20

        val searchBox = EditBox(font, listX, 22, width - listX - 10, 18, Component.literal("Search"))
        searchBox.setHint(Component.literal("Search sounds..."))
        searchBox.value = search
        searchBox.setResponder {
            search = it
            refresh()
        }
        addRenderableWidget(searchBox)

        Tab.entries.forEachIndexed { i, entry ->
            addRenderableWidget(Button.builder(Component.literal(entry.label)) {
                tab = entry
                refresh()
            }.bounds(10, 22 + i * 22, SIDEBAR_WIDTH, 20).build())
        }

        // Volumes only apply while the module is on, so the switch lives here too
        addRenderableWidget(Button.builder(enabledText()) { button ->
            SoundManager.toggle()
            button.message = enabledText()
        }.bounds(10, 28 + Tab.entries.size * 22, SIDEBAR_WIDTH, 20).build())

        list = SoundList(width - listX - 10, height - 56, 46)
        list.setX(listX)
        addRenderableWidget(list)
        refresh()
    }

    private fun enabledText() = Component.literal(if (SoundManager.enabled) "§aEnabled" else "§cDisabled")

    private fun refresh() {
        val sounds = when (tab) {
            Tab.All -> allSounds
            Tab.Recent -> SoundManager.recentSounds()
            Tab.Changed -> SoundManager.changedSounds().sortedBy(Identifier::toString)
            Tab.Misc -> allSounds.filter { id -> prefixes.none { id.path.startsWith(it) } }
            else -> allSounds.filter { it.path.startsWith(tab.prefix!!) }
        }
        val query = search.trim().lowercase()
        list.show(sounds.filter { query.isEmpty() || query in it.toString() || query in displayName(it).lowercase() })
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        guiGraphics.centeredText(font, "§lSound Manager §r§7(${tab.label}: ${list.children().size})", width / 2, 8, -1)
    }

    override fun onClose() {
        ModuleManager.saveConfigurations()
        mc.gui.setScreen(previous)
    }

    override fun isPauseScreen(): Boolean = false

    private inner class SoundList(width: Int, height: Int, y: Int) :
        ContainerObjectSelectionList<SoundEntry>(mc, width, height, y, 24) {

        fun show(sounds: List<Identifier>) {
            replaceEntries(sounds.map(::SoundEntry))
            setScrollAmount(0.0)
        }

        override fun getRowWidth(): Int = width - 20

        // Vanilla puts the scrollbar just past the rows, which here is outside the list, so dragging it never registered
        override fun scrollBarX(): Int = right - scrollbarWidth()
    }

    private inner class SoundEntry(private val id: Identifier) : ContainerObjectSelectionList.Entry<SoundEntry>() {
        private val name = displayName(id)

        private val slider = object : AbstractSliderButton(0, 0, 120, 20, Component.empty(), SoundManager.getVolumePercent(id) / 200.0) {
            init { updateMessage() }

            private fun percent() = (value * 40).toInt() * 5

            override fun updateMessage() {
                message = Component.literal("${percent()}%")
            }

            override fun applyValue() = SoundManager.setVolumePercent(id, percent())
        }

        private val play = Button.builder(Component.literal("Play")) {
            playSoundAtPlayer(SoundEvent.createVariableRangeEvent(id), 0.25f)
        }.width(40).build()

        override fun extractContent(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, delta: Float) {
            play.setPosition(contentRight - play.width, contentY)
            play.extractRenderState(guiGraphics, mouseX, mouseY, delta)
            slider.setPosition(play.x - slider.width - 4, contentY)
            slider.extractRenderState(guiGraphics, mouseX, mouseY, delta)

            val color = if (SoundManager.getVolumePercent(id) == 100) -1 else 0xFFFFAA00.toInt()
            guiGraphics.text(font, font.plainSubstrByWidth(name, slider.x - contentX - 8), contentX, contentYMiddle - 4, color)
            if (hovered && mouseX < slider.x) guiGraphics.setTooltipForNextFrame(font, Component.literal(id.toString()), mouseX, mouseY)
        }

        override fun children(): List<GuiEventListener> = listOf(slider, play)

        override fun narratables(): List<NarratableEntry> = listOf(slider, play)
    }

    private companion object {
        const val SIDEBAR_WIDTH = 80

        fun displayName(id: Identifier): String {
            val name = id.path.removePrefix("entity.").replace('.', ' ').replace('_', ' ')
            return if (id.namespace == Identifier.DEFAULT_NAMESPACE) name else "${id.namespace}: $name"
        }
    }
}
