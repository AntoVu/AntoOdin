# AntoOdin
Simple odin addon for more features I wanted

Built for two Minecraft versions: grab `anto-odin-<version>+26.2.jar` (Odin 0.3.6) or `anto-odin-<version>+26.3.jar`
(Odin 0.3.7). Features that need newer Odin APIs may be 26.3-only.

<details>
<summary><b>Anto</b></summary>

**Wardrobe Hotkeys (A)**
 - Supports Equipment Wardrobe
 - Auto close after use
 - Play sound on equip

**Queue Loadout**
 - Auto equip loadout 1-12 on keybind (opens /loadouts)

**Kuudra Auto GFS**
 - Get TAP/TWAP after build

**CPS Display**
 - Displays cps for left, right, or both

**Pearl Refill**
 - Auto refill pearls after set amount

**Diana Auto Warp**
 - Auto warp to next location after digging burrow
 - ! Requires SkyHanni's Burrow Guess

**Minion Helper**
 - Claim minion keybind and auto close
 - Click item to auto place in fuel

**Loadout Hotkeys (A)**
 - Equip slot 1-12
 - Next and Previous page
 - Auto close after use, per-slot Keep Open
 - Play sound on equip

**Drop Guard**
 - Block dropping items outside GUIs during F7/M7 terminals
 - Hold override keybind to drop anyway

**Chat Rules**
 - Per-message rules, edit with `/ao chatrules`
 - Match by equals, contains, starts/ends with or regex
 - Filter by dungeon class and island
 - Show title, play sound, hide message, or run a command (other mods' too) after a delay
 - Import NoFrills rules with `/ao chatrules import`
 - Pre-defined rules (old Chat Filter) that only hide spam, toggle on/off

**Level Colors**
 - Custom colors for SkyBlock level prefixes in chat, tab, nametags and lore, edit with `/ao levelcolors`
 - Add ranges past 480 or recolor prestiges
 - Bracket color, solid/gradient/per-character digit colors

**Unfocused Tweaks**
 - Lower game sounds while tabbed out (PM sound stays normal)
 - Skip world rendering, FPS limit, no vanilla FPS reduction while tabbed out

**Command Tooltip**
 - Shows the command a chat message runs on hover

**Calendar Date**
 - Real date and time of SkyBlock calendar events
 - Fear Mongerer arrival and Jerry's Workshop opening

**Experiment Solver**
 - Chronomatron and Ultrasequencer: marks next slots, blocks wrong clicks
 - Superpairs: remembers rewards, highlights pairs and powerups

**Corpse Highlight**
 - Highlights Lapis, Tungsten, Umber and Vanguard corpses in Glacite Mineshafts
 - Hides opened ones, optional see-through-walls

**Jellybean Hider**
 - Hides the Magic Jellybean mutation in the Garden
 - Only renders blocks that can still be broken
 - Configurable hide Y level

**Aloe Highlight**
 - Highlights aloe by growth stage in the Garden

**Dungeon Splits**
 - Splits HUD with tick time and boss entry split
 - Current Split HUD, optionally only in boss and per F7/M7 phase
 - Chat message after each split with PB comparison
 - Projected run time from recent runs
 - Time lost to lag

**Waypoint Placer**
 - Right click blocks to add/remove Odin waypoints (Allow Edits on), sneak to set a label
 - Uses Odin's areas: islands, F6/F7 boss phases, Kuudra tiers
 - Color presets with cycle keybind, editor HUD

**Inventory Buttons**
 - Command buttons around your inventory (and chests), place with `/ao buttons`
 - Icon from an item id, player head (`skull:<texture hash>`) or your held item
 - Five styles: Raised, Slot, Dark, Flat, Clear

**Item Customizer**
 - `/ao customize` on held item: name (& codes), glint, model, leather dye
 - Model is an item id or `skull:<texture hash>`
 - Name shortcuts: `**` for ✪, `*1` to `*5` for master stars
 - Only you see it, saved per item UUID

**Highlight Trash**
 - Highlights junk M7 chest books in your inventory in the Dungeon Hub

**Auto Tip**
 - Runs /tipall every 1000 s (adjustable)
 - Optionally hides the reply

**Pane Click Guard**
 - Ignores clicks on blank glass panes, nothing sent to the server
 - Terminals and experiment games are left alone

**Terminal Click Limit**
 - Holds back terminal clicks Hypixel would ignore (over 5 in 500 ms), also when holding Q
 - Needs Odin's Terminal Solver

**Dungeon Map (A)**
 - Map in [Devonian](https://github.com/Synnerz/devonian)'s style, from Odin's map data
 - Hidden rooms (darkened), doors read from the world
 - Names and checkmarks in any installed font (`/ao mapfont`), player heads outlined in their class color, names while holding a leap

**Dupe Class Alert**
 - Title, sound and chat when teammates share a class at run start
 - Optional party chat message

**Shadow Assassin Notify**
 - Title and sound when a Shadow Assassin is about to teleport to you
 - Ignores the F3/M3 boss, which also uses the world border

**Death Bow Swap**
 - M7 Storm as Archer: after shooting Death Bow, swaps to a hotbar slot then queues a loadout
 - Separate delays for each, once per run
 - ! Requires Queue Loadout

**Blood Rush Waypoints**
 - Master mode blood rush: your class's mob boxes per room, numbered in kill order, with your stack
 - Plan picked by the door you came in through, role by class (Archer/Mage) or override
 - Show on All Classes: general clear guide after the blood door opens, or all run
 - Miniboss rooms show every box, chat line tells the door and box count
 - Boxes hide once the room is cleared

</details>

<details>
<summary><b>Noamm</b></summary>

Ported from [NoammAddons](https://github.com/Noamm9/NoammAddons) by Noamm9 (CC0-1.0), rebuilt on Odin. Own "Noamm" category.

**Auction Price Input**
 - Text box instead of the auction price sign
 - Undercut mode subtracts from lowest BIN
 - Enter confirms Create and Confirm menus

**I Hate Doors**
 - Entrance, wither and blood doors render as colored glass
 - Render only, door stays solid

**Hidden Mobs**
 - Reveals invisible Shadow Assassins, Fels and stealthy blood mobs

**Gate Highlight**
 - Highlights your F7 P3 gate until destroyed

**Snappy Tappy**
 - Last pressed of two opposing movement keys wins

**Mono Audio**
 - All game audio through a single channel

**Sound Manager**
 - Per-sound volume 0-200%, open with `/ao sounds`
 - Recent tab shows last played pitch

**Explosive Shot**
 - Explosive Shot damage per enemy

**Leap Counter**
 - HUD of teammates at your F7 P3 spot, title and sound when all are there

**Maxor's Crystals**
 - F7 P1 crystal respawn timer, placement time with PB, unplaced alert

**Item Tooltip**
 - Lowest BIN, bazaar and NPC sell prices, whole stack with unit price
 - Item creation date and age
 - SkyBlock item ID

**Damage Splash**
 - Shortens damage numbers (1.2m), keeps crit colors

**Lava To Water**
 - Lava as see-through water, no lava fog
 - Everywhere, or only Catacombs, Kuudra and Crimson Isle

**Freeze Display**
 - How long the server has been frozen past a threshold

**Relic Spawn Timer**
 - Countdown to the M7 relic spawn after Necron dies, in your class color

**Time Changer**
 - Sky and fog drawn at a fixed time of day: presets, real time, or a custom tick

**No AOTV Paths**
 - No shovel sound (and, on 26.3, no ghost path block) when right-clicking grass or dirt with the Aspect of the Void

**Custom Scoreboard**
 - Restyled scoreboard, optionally hides server ID

**Leap Menu Extras**
 - Tints dead players (red) and teammates inside the F7/M7 core (green), rings the last wither door opener in Odin's Leap Menu
 - Hides teammates you just leaped onto
 - Map Leap: Dungeon Map (A) in the leap menu, click a teammate to leap (optionally after blood door opens)
 - Map Leap Names: teammate names on the leap map, with or without the map's own name setting

**Dragon Extras**
 - M7 dragon aim marker, arrows-hit count per dragon
 - Needs Odin's Wither Dragons

**Camera Tweaks**
 - Custom FOV, Slowness FOV change, Full Bright, no Blindness or Nausea

**Rejoin Timer**
 - How long ago you were kicked from SkyBlock

**Party Finder Extras**
 - Level requirement and missing classes on party heads
 - Member stats (Cata level, secrets, MP, S+ PB) in the tooltip

</details>

<details>
<summary><b>OdinClient</b></summary>

From skies-starred's [OdinClient](https://github.com/skies-starred/OdinClient) (BSD 3-Clause), no longer maintained, updated to Odin 0.3.6 and MC 26.2. Own "OdinClient" category.

**Auto Experiments**
 - First click Delay
 - Auto close Ultrasequencer one early
 - Auto start Ultrasequencer

**Door Highlight**
 - Wither/blood door and key highlight with depth check (replaces Odin's)
 - Line to Door: line to the next closed wither door, then blood door
 - Highlight Other Doors: boxes normal doors in their own color, current room or all

**Auto Clicker**
 - Left/right click at a set CPS while holding a keybind, or Terminator only
 - Item whitelist (empty hand too), manage with `/ao ac`
 - Optional per-item key (`/ao ac add right RMB`)

**Trajectories**
 - Pearl and bow (Terminator too) paths, impact boxes, hit entities

**Highlight (C)**
 - ESP and tracer for F7 withers, dungeon bats and mobs from `/ao highlight`
 - Starred mobs and teammate glow stay with Odin's Highlight

**Spirit Bear (C)**
 - Odin's Spirit Bear HUD plus highlight and tracer (replaces Odin's)

**Livid Solver (C)**
 - Odin's Livid Solver plus ESP and tracer (replaces Odin's)

**Secret Hitboxes**
 - Full block or 1.8 hitboxes for levers, buttons, skulls and chests in dungeons

**Fuck Diorite**
 - Storm pillars as glass, pillar colored, one color or random
 - Render only

**Auto GFS**
 - Refills pearls, jerries, superbooms and leaps from sacks on dungeon start or a timer
 - Gets an Architect's First Draft when you fail a puzzle

**Escrow Fix**
 - Reopens the AH or BZ after escrow closes it

**Ghosts**
 - Shows invisible Dwarven Mines ghosts, hides their powered layer

**Camera Helper**
 - Camera clip and custom third person distance

</details>

<details>
<summary><b>Fixes</b></summary>

**Door Fix**
 - Fixes iron door rotations in F7 P3 section 3

**Mod Hider**
 - Stops servers detecting mods through translation keys in signs and anvils

**Item Count Fix**
 - Item counts on unstackable items in menus

**Disconnect Fix**
 - Stops corrupted entity data (Tarantula slayer) from disconnecting you

**Double Use Fix**
 - Stops Blaze Daggers and fishing rods activating twice on a block

**Cosmetic Disabler**
 - Turns off custom names, sizes and emblems from other mods, each with its own toggle
 - Odin dev names (also saves ~3.6% of render thread) and sizes
 - skies-starred's donor names, emblems and sizes (Detexturify's kontext), if installed

</details>

<details>
<summary><b>Commands</b></summary>

`/ao trackmutation start|stop <jellybean|thunderling|aloe>`
 - Logs crop collections, on stop copies gained collection and NPC profit to clipboard

`/ao best [floor]`
 - Sums your PB splits into a theoretical best run (current floor, else M7)

`/ao mapfont <name>`
 - Dungeon Map (A) font, any installed font (e.g. `ComicSansMS`) or `Minecraft`

`/ao sounds`
 - Opens Sound Manager

`/ao waypoints`
 - Opens Odin's waypoint manager (import/export)
 - `color <RRGGBBAA>` sets the next waypoint's color

`/ao ac add left|right [key]`, `/ao ac remove left|right`, `/ao ac clear left|right|all`, `/ao ac list`
 - Auto Clicker whitelist for the held item

`/ao highlight add <name> [#RRGGBB]`, `/ao highlight remove <name>`, `/ao highlight clear|list`
 - Mobs Highlight (C) draws

`/ao calc <type> ...`
 - `cata <start> <end>`, `skill <start> <end>`: XP between levels
 - `pet <tier> <start> <end>`: pet XP, legendary up to 200
 - `drop <base %> [looting] [magic find] [pet luck]`: drop chance
 - `coin <gems> <coins> [cookie price]`: coins per gem vs booster cookies
 - `usd <coins> <cookie price>`: rough USD value
 - `tax <price>`: auction fees
 - Amounts accept k/m/b (`1.5m`)

</details>

<details>
<summary><b>Planned</b></summary>

**Dungeons Auto GFS**
 - Get 2 TWAP after lightning
 - Get 2 TWAP after terminals
 - Get 6 TWAP after goldor
 - Get 6 TWAP on dragon spawn

**Warp Cooldown**
 - Mineshaft warp cooldown

**Auto Gloomlock**
 - When low mana
 - When low health

</details>
