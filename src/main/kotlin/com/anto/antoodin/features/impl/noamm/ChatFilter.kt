// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.ListSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.network.chat.Component

/**
 * Hides messages when they are added to the chat window, after every mod has already seen them.
 * Cancelling MessageEvent.Chat instead would stop Fabric's ALLOW_GAME chain, so other mods
 * (and our own chat triggers) would miss lines like [BOSS] dialogue.
 *
 * @see com.anto.antoodin.mixin.mixins.ChatComponentMixin
 */
object ChatFilter : Module(
    name = "Chat Filter",
    description = "Hides useless chat messages. Add your own patterns with /ao chathider.",
    category = Skit.NOAMM
) {
    private class FilterGroup(val enabled: () -> Boolean, patterns: List<String>) {
        val regexes = patterns.map(::Regex)
    }

    private val categoryDropdown by DropdownSetting("Categories", desc = "Which kinds of messages to hide.")
    private val lobby by BooleanSetting("Lobby & Warps", true, desc = "Warping, server sending, profile and lobby join messages.").withDependency { categoryDropdown }
    private val announcements by BooleanSetting("Announcements", true, desc = "Watchdog, link safety and event reward announcements.").withDependency { categoryDropdown }
    private val doorsAndKeys by BooleanSetting("Doors & Keys", true, desc = "Dungeon key pickups and door openings.").withDependency { categoryDropdown }
    private val blockedActions by BooleanSetting("Blocked Actions", true, desc = "\"You cannot do that\", already-used levers and chests, and slow-down warnings.").withDependency { categoryDropdown }
    private val abilities by BooleanSetting("Abilities & Mana", true, desc = "Ability cooldowns, mana warnings, class ability ready messages and milestones.").withDependency { categoryDropdown }
    private val damage by BooleanSetting("Damage", true, desc = "Damage dealt and taken messages.").withDependency { categoryDropdown }
    private val healing by BooleanSetting("Healing & Buffs", true, desc = "Heals, tethers, orbs, blessings and dungeon buffs.").withDependency { categoryDropdown }
    private val drops by BooleanSetting("Drops & Items", true, desc = "Dungeon item pickups, sacks, autopet and kill combo messages.").withDependency { categoryDropdown }
    private val experience by BooleanSetting("XP & Essence", true, desc = "Event EXP, GEXP, skill XP bonus and essence messages.").withDependency { categoryDropdown }
    private val puzzles by BooleanSetting("Puzzles", true, desc = "Puzzle solved, statue and puzzle warning messages.").withDependency { categoryDropdown }
    private val dialogue by BooleanSetting("NPC & Boss Dialogue", true, desc = "[BOSS], Mort, fairy and other NPC dialogue.").withDependency { categoryDropdown }
    private val potions by BooleanSetting("Potion Effects", true, desc = "Potion effects paused in dungeons messages.").withDependency { categoryDropdown }

    private val customPatterns by ListSetting("Custom Patterns", mutableListOf<String>())

    // Patterns from NoammAddons data/uselessMessages.json, matched against the whole message
    private val groups = listOf(
        FilterGroup({ lobby }, listOf(
            "^Warping you to your SkyBlock island\\.\\.\\.$", "^Warping...$", "Sending to server .+", "Queuing... .+",
            "Welcome to Hypixel SkyBlock!", "Latest update: SkyBlock .+", "^You are playing on profile: .+$", "^Profile ID: .+$",
            "Error initializing players: undefined Hidden", ".+ the lobby!.*", "You have 60 seconds to warp out! CLICK to warp now!"
        )),
        FilterGroup({ announcements }, listOf(
            "^ {2}Clicking sketchy links can result in your account$", "^ {2}being stolen!$", "^ {2}Link looks suspicious\\? - Don't click it!$",
            "Blacklisted modifications are a bannable offense!", "\\[WATCHDOG ANNOUNCEMENT]", "^Watchdog has banned .+ players in the last 7 days.$",
            "Staff have banned an additional .+",
            "Hoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!",
            " +You have [0-9]+ unclaimed event rewards!", " +Event rewards are deleted after 10 SkyBlock years!"
        )),
        FilterGroup({ doorsAndKeys }, listOf(
            "A .+ Key was picked up!?", "RIGHT CLICK on .+ to open it\\. This key can only be used to open 1 door!", ".+ opened a .+ door!",
            "You hear the sound of something opening...", "You do not have the key for this door!", "^A mystical force .+"
        )),
        FilterGroup({ blockedActions }, listOf(
            "You cannot use abilities in this room!", "You cannot do that in this room!", "^You don't have enough charges to break this block right now!$",
            "There are blocks in the way!", "This creature is immune to this kind of magic!", "This lever has already been used.",
            "This chest has already been searched!", "You have already opened this dungeon chest!", "Someone has already activated this lever!",
            "That chest is locked!", "Whow! Slow down there!", "Woah slow down, you're doing that too fast!",
            "Command Failed: This command is on cooldown! Try again in about a second!", "Please wait a few seconds between refreshing!",
            "Please wait a bit before doing this!", "This menu is disabled here!", "This Terminal doesn't seem to be responsive at the moment.",
            "You cannot put this item in the Potion Bag!", "You don't have any inventory space!"
        )),
        FilterGroup({ abilities }, listOf(
            "This item's ability is temporarily disabled!", "This item is on cooldown.+", "This ability is on cooldown.+",
            "Your Ultimate is currently on cooldown for .+ more seconds.", "You do not have enough mana to do this!",
            "You need at least .+ mana to activate this!", ".+ is ready to use! Press DROP to activate it!", "Throwing Axe is now available!",
            "Guided Sheep is now available!", "Your Berserk ULTIMATE Ragnarok is now available!", "Used Ragnarok!", "Used Throwing Axe!",
            ".+ is now ready!", "Your .+ stats are doubled because you are the only player using this class!",
            "(Archer|Mage|Berserker|Tank|Healer) Milestone.+", "^Creeper Veil Activated!$", "^Creeper Veil De-activated!$", "You summoned your.+"
        )),
        FilterGroup({ damage }, listOf(
            "Goldor's TNT Trap hit you for [\\d,.]+ true damage.", "Necron's Nuclear Frenzy hit you for .+ damage.",
            "Goldor's Greatsword hit you for .+ damage.", "The Frozen Adventurer used Ice Spray on you!",
            "The Lost Adventurer used Dragon's Breath on you!", "A Crypt Wither Skull exploded, hitting you for .+ damage.",
            "The .+ Trap hit you for .+ damage!", "The Mage's Magma burnt you for .+ true damage.",
            ".+ (?:struck|hit|exploded) .+ (?:for |you for ).+", "Your .+ hit .+ for [\\d,.]+ damage\\.",
            "Your .+ hit .+ (?:enemy|enemies) for .+ damage.", "Your Spirit Pet hit .+ enemy for .+ damage.", "Mute silenced you!",
            "A shiver runs down your spine...", "Giga Lightning.+"
        )),
        FilterGroup({ healing }, listOf(
            ".+ healed you for .+ health!", "You were healed for .+ health by .+!",
            "You were healed for .+ health by .+'s Healing Bow and gained \\+.+ Strength for 10 seconds.",
            "Your fairy healed yourself for .+ health!", "Your fairy healed .+ for .+ health!", ".+ fairy healed you for .+ health!",
            "Your Spirit Pet healed .+ for .+ health!", "Your tether with .+ healed you for .+ health.",
            "BUFF! You were splashed by .+ with Healing VIII!", "BUFF! You have gained Healing V!",
            "You gained .+ HP worth of absorption for 3s from .+!", ".+ granted you .+ strength for 20 seconds!",
            "Your bone plating reduced the damage you took by .+!", ".+ formed a tether with you!", ".+ used .+ on you!",
            ".+ picked up your .+ Orb!", ".+ You picked up a .+ Orb from .+ healing you for .+ and granting you \\+.+% .+ for 10 seconds.",
            "DUNGEON BUFF! .+", "A Blessing of .+ was picked up!", ".+ has obtained Blessing of .+!",
            " {5}(?:Also )?(?:grants|granted) you .+", ".*Granted you.+"
        )),
        FilterGroup({ drops }, listOf(
            ".+ has obtained Superboom TNT( x[0-9])?!", ".+ has obtained Revive Stone!", ".+ has obtained Premium Flesh!",
            ".+ has obtained Beating Heart!", "RARE DROP! Hunk of Blue Ice \\(\\+.+% Magic Find!\\)", "RARE DROP! Beating Heart .+",
            "\\[Sacks] .+", "Your Auto Recombobulator recombobulated .+!",
            "Inventory full\\? Don't forget to check out your Storage inside the SkyBlock Menu!", "You have .+ unclaimed .+",
            "Only up to 2 rules may trigger at once!", "Some of your autopet rules did not trigger.", "\\+[0-9]+ Kill Combo.*",
            "Your Kill Combo has expired! You reached a [0-9]+ Kill Combo!"
        )),
        FilterGroup({ experience }, listOf(
            "^You earned .+ Event EXP from playing SkyBlock!$", "You earned .+ GEXP from playing .+!",
            "BONUS! Temporarily earn [0-9]+% more skill experience!", "ESSENCE! .+ found .+ Essence!", ".+ unlocked .+ Essence.+",
            " {4}.+ Essence x.+", ".+ found a Wither Essence! Everyone gains an extra essence!", " Experience Team Bonus"
        )),
        FilterGroup({ puzzles }, listOf(
            "PUZZLE SOLVED!.+", "\\[STATUE].+", "You cannot move the silverfish in that direction!",
            "You cannot hit the silverfish while it's moving!", "It isn't your turn!", "Don't move diagonally! Bad!",
            "Oops! You stepped on the wrong block!"
        )),
        FilterGroup({ dialogue }, listOf(
            "\\[BOSS] .+", "\\[NPC] Hugo", ".+ Mort: .+", ".+ the Fairy: .+",
            "^The Redstone Pigmen are unhappy with you stealing their ores! Look out!$", "\\[SKULL] .+", "\\[BOMB] Creeper:.+"
        )),
        FilterGroup({ potions }, listOf(
            "Your active Potion Effects have been paused and stored. They will be restored when you leave Dungeons! You are not allowed to use existing Potion Effects while in Dungeons.",
            "You are not allowed to use Potion Effects while in Dungeon, therefore all active effects have been paused and stored\\. They will be restored when you leave Dungeon!"
        ))
    )

    private var compiledCustom: Pair<List<String>, List<Regex>> = emptyList<String>() to emptyList()

    private fun customRegexes(): List<Regex> {
        if (compiledCustom.first != customPatterns) {
            val patterns = customPatterns.toList()
            compiledCustom = patterns to patterns.mapNotNull { runCatching { Regex(it) }.getOrNull() }
        }
        return compiledCustom.second
    }

    @JvmStatic
    fun shouldHide(message: Component): Boolean {
        if (!enabled) return false
        val text = message.string.noControlCodes
        return groups.any { group -> group.enabled() && group.regexes.any { it.matches(text) } } ||
            customRegexes().any { it.matches(text) }
    }

    fun addPattern(pattern: String) {
        if (pattern in customPatterns) return modMessage("§cThat pattern is already in the list.")
        if (runCatching { Regex(pattern) }.isFailure) return modMessage("§cInvalid regex.")
        customPatterns.add(pattern)
        ModuleManager.saveConfigurations()
        modMessage("§aAdded chat filter pattern.")
    }

    fun removePattern(pattern: String) {
        val removed = customPatterns.remove(pattern)
        if (removed) ModuleManager.saveConfigurations()
        modMessage(if (removed) "§aRemoved chat filter pattern." else "§cNo matching pattern found.")
    }

    fun listPatterns() {
        if (customPatterns.isEmpty()) return modMessage("§7No custom chat filter patterns.")
        modMessage("§aCustom chat filter patterns:")
        customPatterns.forEach { modMessage("§7- §f$it") }
    }

    fun patterns(): List<String> = customPatterns.toList()
}
