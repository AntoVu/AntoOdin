// Feature idea from NoFrills (GPL-3.0). Written from scratch for AntoOdin, no NoFrills code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.playSoundAtPlayer
import com.odtheking.odin.utils.render.getStringWidth
import com.odtheking.odin.utils.render.text
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import java.io.File
import java.util.regex.Pattern
import kotlin.random.Random

/**
 * Per-message rules: the first enabled rule that matches a chat line shows a title, plays a sound,
 * runs a command and/or hides the line. Rules are edited in [ChatRulesScreen] (/ao chatrules).
 *
 * @see com.anto.antoodin.mixin.mixins.ChatComponentMixin
 */
object ChatRules : Module(
    name = "Chat Rules",
    description = "Titles, sounds, commands or hiding for chat messages you pick. Edit them with /ao chatrules.",
    category = Skit.ANTO
) {
    enum class MatchType(val tip: String) {
        Equals("The message must equal the text."),
        Contains("The message must contain the text."),
        StartsWith("The message must start with the text."),
        EndsWith("The message must end with the text."),
        Regex("The whole message must match the Java regex.")
    }

    // Field names match NoFrills' saved rules, so its config can be imported as-is
    data class ChatRule(
        var name: String = "New Rule",
        var enabled: Boolean = false,
        var match: String = "",
        var matchType: MatchType = MatchType.Equals,
        var caseSensitive: Boolean = false,
        var cancel: Boolean = false,
        var classFilter: String = "",
        var islandFilter: String = "",
        var title: String = "",
        var titleFadeIn: Int = 0,
        var titleStay: Int = 30,
        var titleFadeOut: Int = 10,
        var customTitle: String = "",
        var customTitleStay: Int = 40,
        var sound: String = "",
        var soundVolume: Float = 1f,
        var soundPitch: Float = 1f,
        var command: String = "",
        var commandDelay: Int = 0,
        var commandDelayVariety: Int = 0
    ) {
        @Transient var lastCommandAt = 0L
    }

    private val openScreen by ActionSetting("Open Chat Rules", desc = "Opens the chat rule editor.") {
        mc.gui.setScreen(ChatRulesScreen(mc.gui.screen()))
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val rulesType = object : TypeToken<MutableList<ChatRule>>() {}.type
    private val rulesFile = File(OdinMod.configFile, "addons/antoodin-chat-rules.json")

    val rules: MutableList<ChatRule> = load()

    // A command that makes its own matching message (e.g. /pc of the matched text) would loop, so each rule waits this long
    private const val COMMAND_COOLDOWN_MS = 1000L

    private val regexCache = HashMap<Pair<String, Boolean>, Regex?>()

    private var customTitle = ""
    private var customTitleTicks = 0

    private val customTitleHud by HUD("Custom Title", "Where custom titles from chat rules show. They don't replace vanilla titles.", false) { example ->
        val text = if (example) "§cCustom Title" else customTitle.takeIf { customTitleTicks > 0 } ?: return@HUD 0 to 0
        text(text, 0, 0, Colors.WHITE)
        getStringWidth(text) to 9
    }

    init {
        on<MessageEvent.Chat> {
            val rule = firstMatch(message) ?: return@on
            if (rule.title.isNotEmpty()) {
                mc.gui.hud.setTimes(rule.titleFadeIn, rule.titleStay, rule.titleFadeOut)
                mc.gui.hud.setTitle(Component.literal(rule.title.formatCodes()))
            }
            if (rule.customTitle.isNotEmpty()) {
                customTitle = rule.customTitle.formatCodes()
                customTitleTicks = rule.customTitleStay
            }
            Identifier.tryParse(rule.sound.trim())?.takeIf { rule.sound.isNotBlank() }?.let {
                playSoundAtPlayer(SoundEvent.createVariableRangeEvent(it), rule.soundVolume, rule.soundPitch)
            }
            if (rule.command.isNotBlank()) runCommand(rule)
        }

        on<TickEvent.End> { if (customTitleTicks > 0) customTitleTicks-- }
    }

    // Hides at display time, after every mod has seen the line (see ChatFilter)
    @JvmStatic
    fun shouldHide(message: Component): Boolean = enabled && firstMatch(message.string.noControlCodes)?.cancel == true

    private fun firstMatch(message: String): ChatRule? {
        val className = DungeonUtils.currentDungeonPlayer.clazz.takeIf { it != DungeonClass.EMPTY }?.name?.lowercase()
        val area = LocationUtils.currentArea.takeIf { it in islands }
        return rules.firstOrNull { rule ->
            rule.enabled && rule.match.isNotEmpty() &&
                // An unknown class or area never filters a rule out
                (rule.classFilter.isBlank() || className == null || className in rule.classFilter.lowercase()) &&
                (rule.islandFilter.isBlank() || area == null || area in islandsIn(rule.islandFilter)) &&
                matches(rule, message)
        }
    }

    private fun matches(rule: ChatRule, message: String): Boolean {
        if (rule.matchType == MatchType.Regex) return regex(rule)?.matches(message) == true
        val text = if (rule.caseSensitive) message else message.lowercase()
        val match = if (rule.caseSensitive) rule.match else rule.match.lowercase()
        return when (rule.matchType) {
            MatchType.Equals -> text == match
            MatchType.Contains -> match in text
            MatchType.StartsWith -> text.startsWith(match)
            MatchType.EndsWith -> text.endsWith(match)
            MatchType.Regex -> false
        }
    }

    // Islands a filter can name: the same list Odin's waypoint areas come from
    val islands = Island.entries.filter { it != Island.Unknown && it != Island.SinglePlayer }
    private val islandFilterCache = HashMap<String, Set<Island>>()

    // Longest names first, each cut out once found, so "dungeon hub" doesn't also count as "hub".
    // Any separator works, and apostrophes are ignored ("spiders den")
    private fun islandsIn(filter: String): Set<Island> = islandFilterCache.getOrPut(filter) {
        var text = filter.lowercase().replace("'", "")
        islands.sortedByDescending { it.displayName.length }.filterTo(HashSet()) { island ->
            val name = island.displayName.lowercase().replace("'", "")
            (name in text).also { if (it) text = text.replace(name, "|") }
        }
    }

    // Compiled once per pattern; a broken pattern is reported once and then skipped
    private fun regex(rule: ChatRule): Regex? = regexCache.getOrPut(rule.match to rule.caseSensitive) {
        val flags = if (rule.caseSensitive) 0 else Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        runCatching { Pattern.compile(rule.match, flags).toRegex() }
            .onFailure { modMessage("§cChat rule \"${rule.name}\" has an invalid regex: ${it.message?.lineSequence()?.first()}") }
            .getOrNull()
    }

    private fun runCommand(rule: ChatRule) {
        val now = System.currentTimeMillis()
        if (now - rule.lastCommandAt < COMMAND_COOLDOWN_MS) return
        rule.lastCommandAt = now

        val command = rule.command.trim().removePrefix("/")
        val delayMs = rule.commandDelay.coerceAtLeast(0) + Random.nextInt(0, rule.commandDelayVariety.coerceAtLeast(0) + 1)
        val ticks = ((delayMs / 1000.0) * 20).toInt()
        // sendCommand also runs other mods' client commands: Fabric checks its client command tree before sending
        if (ticks == 0) mc.player?.connection?.sendCommand(command)
        else schedule(ticks) { mc.player?.connection?.sendCommand(command) }
    }

    fun testSound(rule: ChatRule) {
        val id = Identifier.tryParse(rule.sound.trim()) ?: return
        playSoundAtPlayer(SoundEvent.createVariableRangeEvent(id), rule.soundVolume, rule.soundPitch)
    }

    private fun String.formatCodes() = replace('&', '§')

    private fun load(): MutableList<ChatRule> = runCatching {
        gson.fromJson<MutableList<ChatRule>>(rulesFile.readText(), rulesType).onEach(::sanitize)
    }.getOrNull() ?: mutableListOf()

    // Gson bypasses Kotlin's null checks, so an unknown or missing match type arrives as null
    @Suppress("SENSELESS_COMPARISON")
    private fun sanitize(rule: ChatRule) {
        if (rule.matchType == null) rule.matchType = MatchType.Equals
    }

    fun save() {
        regexCache.clear()
        islandFilterCache.clear()
        runCatching {
            rulesFile.parentFile.mkdirs()
            rulesFile.writeText(gson.toJson(rules, rulesType))
        }.onFailure { modMessage("§cCouldn't save chat rules: ${it.message}") }
    }

    // Reads NoFrills' own config from this instance and adds every rule not already here
    fun importNoFrills(): String {
        val file = FabricLoader.getInstance().configDir.resolve("NoFrills/Configuration.json").toFile()
        if (!file.isFile) return "§cNo NoFrills config found at ${file.path}."
        val imported = runCatching {
            val array = JsonParser.parseString(file.readText()).asJsonObject
                .getAsJsonObject("chatRules")?.getAsJsonObject("data")?.getAsJsonArray("rules")
                ?: return "§cNoFrills config has no chat rules."
            gson.fromJson<MutableList<ChatRule>>(array, rulesType).onEach(::sanitize)
        }.getOrElse { return "§cCouldn't read the NoFrills config: ${it.message}" }

        val new = imported.filter { rule -> rules.none { it.name == rule.name && it.match == rule.match } }
        rules.addAll(new)
        save()
        return "§aImported ${new.size} chat rule${if (new.size == 1) "" else "s"}" +
            if (new.size < imported.size) " §7(${imported.size - new.size} already here)." else "."
    }
}
