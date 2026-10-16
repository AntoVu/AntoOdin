// Caching based on skies-starred's library (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import java.io.File
import java.util.Optional

/**
 * Recolors SkyBlock level prefixes ([480]) by prestige range wherever text is drawn: chat, tab, nametags, lore.
 * Rules are edited in [LevelColorsScreen] (/ao levelcolors).
 *
 * @see com.anto.antoodin.mixin.mixins.ClientLanguageMixin
 */
object LevelColors : Module(
    name = "Level Colors",
    description = "Custom colors for SkyBlock level prefixes, including levels past 480. Edit them with /ao levelcolors.",
    category = Skit.ANTO
) {
    enum class Mode(val label: String, val tip: String) {
        Solid("Solid", "The first color for every digit."),
        Gradient("Gradient", "Blends through the colors across the digits."),
        PerCharacter("Per character", "One color per digit, in order. The last color repeats.")
    }

    data class LevelRule(
        var from: Int = 520,
        var to: Int = 559,
        var enabled: Boolean = true,
        var bracket: String = "",
        var colors: String = "#5555FF",
        var mode: Mode = Mode.Solid,
        var builtIn: Boolean = false
    )

    // Hypixel's colors per 40 levels; 480 and up stay dark red
    private val HYPIXEL_COLORS = intArrayOf(
        0xAAAAAA, 0xFFFFFF, 0xFFFF55, 0x55FF55, 0x00AA00, 0x55FFFF, 0x00AAAA,
        0x5555FF, 0xFF55FF, 0xAA00AA, 0xFFAA00, 0xFF5555, 0xAA0000
    )
    const val MAX_LEVEL = 999

    // Marks the editor's previews, so the live recoloring leaves them as drawn
    private const val PREVIEW = "antoodin:level_preview"

    private val openScreen by ActionSetting("Open Level Colors", desc = "Opens the level color editor.") {
        mc.gui.setScreen(LevelColorsScreen(mc.gui.screen()))
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val rulesType = object : TypeToken<MutableList<LevelRule>>() {}.type
    private val rulesFile = File(OdinMod.configFile, "addons/antoodin-level-colors.json")

    val rules: MutableList<LevelRule> = load()

    // Parsed colors for each level that has an active rule, null where Hypixel's color stays
    private class Compiled(val bracket: Int?, val colors: IntArray, val mode: Mode)
    private var byLevel = arrayOfNulls<Compiled>(MAX_LEVEL + 1)
    private var anyActive = false

    // Recolored lines by text and style. A null result means the line has no level to recolor.
    private class Entry(@JvmField val text: String, @JvmField val styleHash: Int, @JvmField val result: FormattedCharSequence?)
    private val cache = arrayOfNulls<Entry>(4096)

    init {
        rebuild()
    }

    override fun onEnable() {
        super.onEnable()
        refreshChat()
    }

    override fun onDisable() {
        super.onDisable()
        refreshChat()
    }

    @JvmStatic
    fun recolor(original: FormattedCharSequence?, text: FormattedText?): FormattedCharSequence? {
        if (original == null || text == null || !enabled || !anyActive) return original

        val builder = StringBuilder()
        var styleHash = 0
        text.visit<Unit>({ style, str ->
            builder.append(str)
            styleHash = 31 * styleHash + styleHash(style)
            Optional.empty()
        }, Style.EMPTY)
        val string = builder.toString()
        if (string.indexOf('[') == -1) return original

        val slot = (string.hashCode() * 31 + styleHash) and (cache.size - 1)
        val hit = cache[slot]
        if (hit != null && hit.styleHash == styleHash && hit.text == string) return hit.result ?: original

        val result = recolorSequence(original)
        cache[slot] = Entry(string, styleHash, result)
        return result ?: original
    }

    private fun styleHash(style: Style): Int {
        if (style.isEmpty) return 0
        val flags = (if (style.isBold) 1 else 0) or (if (style.isItalic) 2 else 0) or (if (style.isUnderlined) 4 else 0) or
            (if (style.isStrikethrough) 8 else 0) or (if (style.isObfuscated) 16 else 0)
        return flags xor ((style.color?.value ?: -1) * 31)
    }

    // Finds [level] whose digits carry Hypixel's color for that level, so other bracketed numbers stay as they are
    private fun recolorSequence(original: FormattedCharSequence): FormattedCharSequence? {
        val codepoints = ArrayList<Int>()
        val styles = ArrayList<Style>()
        original.accept { _, style, cp ->
            codepoints += cp
            styles += style
            true
        }

        var changed = false
        var i = 0
        while (i < codepoints.size) {
            if (codepoints[i] != '['.code) { i++; continue }
            var end = i + 1
            while (end < codepoints.size && end - i <= 3 && codepoints[end] in '0'.code..'9'.code) end++
            val digits = end - i - 1
            if (digits !in 1..3 || end >= codepoints.size || codepoints[end] != ']'.code) { i++; continue }

            val level = (i + 1 until end).fold(0) { acc, k -> acc * 10 + (codepoints[k] - '0'.code) }
            val rule = byLevel[level]
            val color = styles[i + 1].color?.value
            if (rule == null || styles[i + 1].insertion == PREVIEW || color != hypixelColor(level) || (i + 1 until end).any { styles[it].color?.value != color }) { i = end; continue }

            rule.bracket?.let { styles[i] = styles[i].withColor(it); styles[end] = styles[end].withColor(it) }
            for (k in 0 until digits) styles[i + 1 + k] = styles[i + 1 + k].withColor(digitColor(rule.colors, rule.mode, k, digits))
            changed = true
            i = end + 1
        }
        if (!changed) return null

        val cps = codepoints.toIntArray()
        val finalStyles = styles.toTypedArray()
        return FormattedCharSequence { sink ->
            for (k in cps.indices) if (!sink.accept(k, finalStyles[k], cps[k])) return@FormattedCharSequence false
            true
        }
    }

    private fun hypixelColor(level: Int) = HYPIXEL_COLORS[minOf(level / 40, HYPIXEL_COLORS.lastIndex)]

    private fun digitColor(colors: IntArray, mode: Mode, index: Int, count: Int): Int = when {
        mode == Mode.PerCharacter -> colors[minOf(index, colors.lastIndex)]
        mode == Mode.Gradient && colors.size > 1 && count > 1 -> {
            val position = index.toFloat() / (count - 1) * (colors.size - 1)
            val from = minOf(position.toInt(), colors.size - 2)
            lerp(colors[from], colors[from + 1], position - from)
        }
        else -> colors[0]
    }

    private fun lerp(a: Int, b: Int, t: Float): Int {
        fun channel(shift: Int) = ((a shr shift and 0xFF) + ((b shr shift and 0xFF) - (a shr shift and 0xFF)) * t).toInt() shl shift
        return channel(16) or channel(8) or channel(0)
    }

    /** "#RRGGBB", "RRGGBB" or "#RGB"; null when invalid. */
    fun parseHex(text: String): Int? {
        val hex = text.trim().removePrefix("#")
        val full = if (hex.length == 3) hex.map { "$it$it" }.joinToString("") else hex
        return if (full.length == 6) full.toIntOrNull(16) else null
    }

    fun parseColors(text: String): IntArray? =
        text.split(',', ' ').filter { it.isNotBlank() }.map { parseHex(it) ?: return null }.toIntArray().takeIf { it.isNotEmpty() }

    private fun compile(rule: LevelRule): Compiled? {
        val colors = parseColors(rule.colors) ?: return null
        val bracket = if (rule.bracket.isBlank()) null else parseHex(rule.bracket) ?: return null
        return Compiled(bracket, colors, rule.mode)
    }

    /** "[level]" drawn with the rule's colors, for the editor. */
    fun preview(rule: LevelRule, level: Int): Component {
        val compiled = compile(rule) ?: return Component.literal("§cInvalid color")
        val digits = level.toString()
        fun bracket(char: String): MutableComponent = Component.literal(char).withColor(compiled.bracket ?: 0x555555)
        return bracket("[").apply {
            digits.forEachIndexed { k, c ->
                append(Component.literal(c.toString()).withColor(digitColor(compiled.colors, compiled.mode, k, digits.length)).withStyle { it.withInsertion(PREVIEW) })
            }
            append(bracket("]"))
        }
    }

    fun hypixelPreview(level: Int): Component =
        Component.literal("§8[").append(Component.literal("$level").withColor(hypixelColor(level)).withStyle { it.withInsertion(PREVIEW) }).append("§8]")

    /** Applies edits right away; built-ins first, so custom ranges win where they overlap. */
    fun rebuild() {
        val next = arrayOfNulls<Compiled>(MAX_LEVEL + 1)
        for (rule in rules.sortedBy { !it.builtIn }) {
            if (!rule.enabled) continue
            val compiled = compile(rule) ?: continue
            for (level in rule.from.coerceAtLeast(0)..rule.to.coerceAtMost(MAX_LEVEL)) next[level] = compiled
        }
        byLevel = next
        anyActive = next.any { it != null }
        cache.fill(null)
    }

    // Chat lines are turned into draw-ready text once, when added, so they need a re-wrap to pick up changes
    private fun refreshChat() = mc.execute { mc.gui.hud.chat.rescaleChat() }

    private fun load(): MutableList<LevelRule> {
        val loaded = runCatching { gson.fromJson<MutableList<LevelRule>>(rulesFile.readText(), rulesType) }.getOrNull()
        return loaded?.onEach(::sanitize) ?: defaults()
    }

    private fun defaults() = HYPIXEL_COLORS.indices.mapTo(mutableListOf()) { i ->
        LevelRule(i * 40, if (i == HYPIXEL_COLORS.lastIndex) MAX_LEVEL else i * 40 + 39, false, "", "#%06X".format(HYPIXEL_COLORS[i]), Mode.Solid, true)
    }

    // Gson bypasses Kotlin's null checks
    @Suppress("SENSELESS_COMPARISON")
    private fun sanitize(rule: LevelRule) {
        if (rule.mode == null) rule.mode = Mode.Solid
        if (rule.bracket == null) rule.bracket = ""
        if (rule.colors == null) rule.colors = ""
    }

    fun save() {
        rebuild()
        refreshChat()
        runCatching {
            rulesFile.parentFile.mkdirs()
            rulesFile.writeText(gson.toJson(rules, rulesType))
        }.onFailure { modMessage("§cCouldn't save level colors: ${it.message}") }
    }
}
