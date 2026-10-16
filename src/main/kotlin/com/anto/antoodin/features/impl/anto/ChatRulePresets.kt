// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.anto

/** A built-in hide-only chat rule. Every pattern must match the whole message, case-sensitive. */
class ChatRulePreset(val name: String, val desc: String, val patterns: List<String>) {
    val rule = ChatRules.ChatRule(
        name = name,
        match = patterns.joinToString("|") { "(?:$it)" },
        matchType = ChatRules.MatchType.Regex,
        caseSensitive = true,
        cancel = true
    )
}

private typealias Preset = ChatRulePreset

// Patterns from NoammAddons data/uselessMessages.json (formerly the Chat Filter module)
val chatRulePresets = listOf(
    Preset("Lobby & Warps", "Warping, server sending, profile and lobby join messages.", listOf(
        "^Warping you to your SkyBlock island\\.\\.\\.$", "^Warping...$", "Sending to server .+", "Queuing... .+",
        "Welcome to Hypixel SkyBlock!", "Latest update: SkyBlock .+", "^You are playing on profile: .+$", "^Profile ID: .+$",
        "Error initializing players: undefined Hidden", ".+ the lobby!.*", "You have 60 seconds to warp out! CLICK to warp now!"
    )),
    Preset("Announcements", "Watchdog, link safety and event reward announcements.", listOf(
        "^ {2}Clicking sketchy links can result in your account$", "^ {2}being stolen!$", "^ {2}Link looks suspicious\\? - Don't click it!$",
        "Blacklisted modifications are a bannable offense!", "\\[WATCHDOG ANNOUNCEMENT]", "^Watchdog has banned .+ players in the last 7 days.$",
        "Staff have banned an additional .+",
        "Hoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!",
        " +You have [0-9]+ unclaimed event rewards!", " +Event rewards are deleted after 10 SkyBlock years!"
    )),
    Preset("Doors & Keys", "Dungeon key pickups and door openings.", listOf(
        "A .+ Key was picked up!?", "RIGHT CLICK on .+ to open it\\. This key can only be used to open 1 door!", ".+ opened a .+ door!",
        "You hear the sound of something opening...", "You do not have the key for this door!", "^A mystical force .+"
    )),
    Preset("Blocked Actions", "\"You cannot do that\", already-used levers and chests, and slow-down warnings.", listOf(
        "You cannot use abilities in this room!", "You cannot do that in this room!", "^You don't have enough charges to break this block right now!$",
        "There are blocks in the way!", "This creature is immune to this kind of magic!", "This lever has already been used.",
        "This chest has already been searched!", "You have already opened this dungeon chest!", "Someone has already activated this lever!",
        "That chest is locked!", "Whow! Slow down there!", "Woah slow down, you're doing that too fast!",
        "Command Failed: This command is on cooldown! Try again in about a second!", "Please wait a few seconds between refreshing!",
        "Please wait a bit before doing this!", "This menu is disabled here!", "This Terminal doesn't seem to be responsive at the moment.",
        "You cannot put this item in the Potion Bag!", "You don't have any inventory space!"
    )),
    Preset("Abilities & Mana", "Ability cooldowns, mana warnings, class ability ready messages and milestones.", listOf(
        "This item's ability is temporarily disabled!", "This item is on cooldown.+", "This ability is on cooldown.+",
        "Your Ultimate is currently on cooldown for .+ more seconds.", "You do not have enough mana to do this!",
        "You need at least .+ mana to activate this!", ".+ is ready to use! Press DROP to activate it!", "Throwing Axe is now available!",
        "Guided Sheep is now available!", "Your Berserk ULTIMATE Ragnarok is now available!", "Used Ragnarok!", "Used Throwing Axe!",
        ".+ is now ready!", "Your .+ stats are doubled because you are the only player using this class!",
        "(Archer|Mage|Berserker|Tank|Healer) Milestone.+", "^Creeper Veil Activated!$", "^Creeper Veil De-activated!$", "You summoned your.+"
    )),
    Preset("Damage", "Damage dealt and taken messages.", listOf(
        "Goldor's TNT Trap hit you for [\\d,.]+ true damage.", "Necron's Nuclear Frenzy hit you for .+ damage.",
        "Goldor's Greatsword hit you for .+ damage.", "The Frozen Adventurer used Ice Spray on you!",
        "The Lost Adventurer used Dragon's Breath on you!", "A Crypt Wither Skull exploded, hitting you for .+ damage.",
        "The .+ Trap hit you for .+ damage!", "The Mage's Magma burnt you for .+ true damage.",
        ".+ (?:struck|hit|exploded) .+ (?:for |you for ).+", "Your .+ hit .+ for [\\d,.]+ damage\\.",
        "Your .+ hit .+ (?:enemy|enemies) for .+ damage.", "Your Spirit Pet hit .+ enemy for .+ damage.", "Mute silenced you!",
        "A shiver runs down your spine...", "Giga Lightning.+"
    )),
    Preset("Healing & Buffs", "Heals, tethers, orbs, blessings and dungeon buffs.", listOf(
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
    Preset("Drops & Items", "Dungeon item pickups, sacks, autopet and kill combo messages.", listOf(
        ".+ has obtained Superboom TNT( x[0-9])?!", ".+ has obtained Revive Stone!", ".+ has obtained Premium Flesh!",
        ".+ has obtained Beating Heart!", "RARE DROP! Hunk of Blue Ice \\(\\+.+% Magic Find!\\)", "RARE DROP! Beating Heart .+",
        "\\[Sacks] .+", "Your Auto Recombobulator recombobulated .+!",
        "Inventory full\\? Don't forget to check out your Storage inside the SkyBlock Menu!", "You have .+ unclaimed .+",
        "Only up to 2 rules may trigger at once!", "Some of your autopet rules did not trigger.", "\\+[0-9]+ Kill Combo.*",
        "Your Kill Combo has expired! You reached a [0-9]+ Kill Combo!"
    )),
    Preset("XP & Essence", "Event EXP, GEXP, skill XP bonus and essence messages.", listOf(
        "^You earned .+ Event EXP from playing SkyBlock!$", "You earned .+ GEXP from playing .+!",
        "BONUS! Temporarily earn [0-9]+% more skill experience!", "ESSENCE! .+ found .+ Essence!", ".+ unlocked .+ Essence.+",
        " {4}.+ Essence x.+", ".+ found a Wither Essence! Everyone gains an extra essence!", " Experience Team Bonus"
    )),
    Preset("Puzzles", "Puzzle solved, statue and puzzle warning messages.", listOf(
        "PUZZLE SOLVED!.+", "\\[STATUE].+", "You cannot move the silverfish in that direction!",
        "You cannot hit the silverfish while it's moving!", "It isn't your turn!", "Don't move diagonally! Bad!",
        "Oops! You stepped on the wrong block!"
    )),
    Preset("NPC & Boss Dialogue", "[BOSS], Mort, fairy and other NPC dialogue.", listOf(
        "\\[BOSS] .+", "\\[NPC] Hugo", ".+ Mort: .+", ".+ the Fairy: .+",
        "^The Redstone Pigmen are unhappy with you stealing their ores! Look out!$", "\\[SKULL] .+", "\\[BOMB] Creeper:.+"
    )),
    Preset("Potion Effects", "Potion effects paused in dungeons messages.", listOf(
        "Your active Potion Effects have been paused and stored. They will be restored when you leave Dungeons! You are not allowed to use existing Potion Effects while in Dungeons.",
        "You are not allowed to use Potion Effects while in Dungeon, therefore all active effects have been paused and stored\\. They will be restored when you leave Dungeon!"
    ))
)
