# AntoOdin
Simple odin addon for more features I wanted
## Features
Wardrobe Hotkeys (A)
 - Supports Equipment Wardrobe
 - Auto close after use
 - Play sound on equip

Queue Loadout
 - Auto equip loadout 1-12 on keybind (opens /loadouts)

Kuudra Auto GFS
 - Get TAP/TWAP after build

CPS Display
 - Displays cps for left, right, or both

Pearl Refill
 - Auto refill pearls after set amount

Diana Auto Warp
 - Auto warp to next location after digging burrow
 - ! Requires SkyHanni's Burrow Guess

Minion Helper
 - Claim minion keybind and auto close
 - Click item to auto place in fuel

Loadout Hotkeys (A)
 - Equip slot 1-12
 - Next and Previous page
 - Auto close after use
 - Play sound on equip

Drop Guard
 - Block dropping items outside of GUIs during F7/M7 terminals
 - Hold override keybind to drop anyway

Chat Rules
 - Per-message rules, edited in a menu with `/ao chatrules`
 - Match by equals, contains, starts with, ends with or regex, optionally case sensitive
 - Filter by dungeon class and island
 - Show a title or a custom title HUD, play a sound, hide the message
 - Run a command (other mods' commands too) after a delay with random variety
 - Import your NoFrills chat rules with `/ao chatrules import`
 - Pre-defined rules (the old Chat Filter categories) that only hide spam: view their patterns and turn them on or off

Level Colors
 - Custom colors for SkyBlock level prefixes in chat, tab, nametags and item lore, edited with `/ao levelcolors`
 - Add your own ranges past 480 (Hypixel's last color), or recolor Hypixel's prestiges
 - Separate bracket color, and solid, gradient or per-character digit colors with hex codes
 - Recolored lines are cached, so the cost stays close to zero

Unfocused Tweaks
 - Lower all game sounds to a set percentage while tabbed out
 - Private message sound still plays at normal volume (configurable sound id and pitch)
 - Skip world rendering, FPS limit and turn off vanilla's FPS reduction while tabbed out

Command Tooltip
 - Shows the command a chat message runs in its hover tooltip

Calendar Date
 - Shows the real date and time of events in the SkyBlock calendar
 - Also when the Fear Mongerer arrives and Jerry's Workshop opens

Experiment Solver
 - Chronomatron and Ultrasequencer: marks the next slots to click and blocks wrong clicks
 - Superpairs: remembers uncovered rewards and highlights pairs and powerups

Corpse Highlight
 - Highlights Lapis, Tungsten, Umber and Vanguard corpses in Glacite Mineshafts
 - Hides corpses you've opened, optional see-through-walls

Jellybean Hider
 - Hides the Magic Jellybean mutation in the Garden
 - Render only the blocks can still be broken
 - Configurable hide Y level

Aloe Highlight
 - Highlights aloe by growth stage in the Garden
 - Colors for Not Ready, Not Optimal, Ready and Harvest Now

Dungeon Splits
 - Splits HUD with tick time and boss entry split
 - Current Split HUD with the live split's tick time, optionally only in boss and per F7/M7 phase
 - Chat message after each split with tick time and PB comparison
 - Projected run time from your recent runs
 - Time lost to lag at the end of the run

Waypoint Placer
 - Right click blocks to add or remove Odin waypoints (Odin's Waypoints module) with Allow Edits on, sneak to set a label
 - Uses Odin's areas: islands, dungeon boss rooms (F6/F7 per phase) and Kuudra tiers
 - Color presets with a cycle keybind, plus an editor HUD

Inventory Buttons
 - Buttons around your inventory (and optionally chests) that run a command when clicked, placed with `/ao buttons`
 - Icons from any item id or a player head skin (`skull:<texture hash>`), or copied from the item in your hand
 - Five button styles: Raised, Slot, Dark, Flat and Clear

Item Customizer
 - Hold an item and use `/ao customize` to change its name (& color codes), enchant glint, model and leather dye color
 - Model can be any item id or a player head skin (`skull:<texture hash>`)
 - Name shortcuts: `**` for ✪, `*1` to `*5` for master stars, `&` color codes, or start from the real name
 - Only you see it, and other mods still read the real item. Saved per item UUID

Highlight Trash
 - Highlights junk M7 chest books (Feather Falling, Infinite Quiver, Combo, Jerry, Bank, No Pain No Gain) in your inventory in the Dungeon Hub

## Noamm
Features ported from [NoammAddons](https://github.com/Noamm9/NoammAddons) by Noamm9 (CC0-1.0), rebuilt on Odin so a
second dungeon/chat/packet library isn't needed. They live in their own "Noamm" category.

Auction Price Input
 - Replaces the auction price sign with a text box
 - Undercut mode subtracts from the lowest BIN
 - Enter confirms the Create and Confirm auction menus

I Hate Doors
 - Renders entrance, wither and blood doors as colored glass
 - Render only, the door is still solid and map mods still see it

Hidden Mobs
 - Reveals invisible Shadow Assassins, Fels and stealthy blood mobs

Gate Highlight
 - Highlights the F7 P3 gate of your section until it is destroyed

Snappy Tappy
 - The most recently pressed of two opposing movement keys wins

Mono Audio
 - Plays all game audio through a single channel

Sound Manager
 - Per-sound volume from 0 to 200%, open with `/ao sounds`
 - Recent tab shows the pitch each sound last played at

Explosive Shot
 - Shows Explosive Shot damage per enemy

Leap Counter
 - HUD of how many teammates reached your F7 P3 spot, with a title and sound when everyone is there

Maxor's Crystals
 - F7 P1 crystal respawn timer, crystal placement time with PB, unplaced crystal alert

Item Tooltip
 - Lowest BIN, bazaar buy/sell and NPC sell prices on item tooltips, priced for the whole stack with the unit price beside it

Damage Splash
 - Shortens damage numbers (1.2m) in NEU's style, keeping Hypixel's crit colors

Lava To Water
 - Renders lava as see-through water and removes lava fog
 - Everywhere, or only in Catacombs, Kuudra and the Crimson Isle

Freeze Display
 - Shows how long the server has been frozen past a threshold

Custom Scoreboard
 - Restyled scoreboard, optionally hiding the server ID

Leap Menu Extras
 - Tints dead players and rings the last wither door opener's face in Odin's Leap Menu
 - Hides teammates you just leaped onto

Dragon Extras
 - M7 dragon aim marker for arrow stacks, arrows-hit count per dragon
 - Needs Odin's Wither Dragons

Camera Tweaks
 - Custom FOV, adjustable Slowness FOV change, Full Bright, no Blindness or Nausea

Rejoin Timer
 - Shows how long ago you were kicked from SkyBlock

Party Finder Extras
 - Level requirement and missing classes on each party's head
 - Member stats (Catacombs level, secrets, MP, S+ PB) and missing classes in the tooltip

## OdinClient
Features from skies-starred's [OdinClient](https://github.com/skies-starred/OdinClient) (BSD 3-Clause), which is no longer
maintained, updated to Odin 0.3.6 and MC 26.2. They live in their own "OdinClient" category.

Auto Experiments
 - First click Delay
 - Auto close Ultrasequencer one early
 - Auto start Ultrasequencer

Door Highlight
 - OdinClient's wither/blood door and key highlight, with a depth check for the key box (replaces Odin's Door Highlight)
 - Line to Door draws a line to the nearest closed wither door, or the blood door once no wither doors are left

Auto Clicker
 - Left and right click at a set CPS while holding a keybind, or Terminator only mode
 - Item whitelist (empty hand included) managed with `/ao ac`, listed one colored item per row with the UUID or SkyBlock ID on hover
 - Optional per-item key (`/ao ac add right RMB`) that replaces the Left/Right Click bind while holding that item

Trajectories
 - Pearl and bow (Terminator spread too) paths, impact boxes and hit entities

Highlight (C)
 - ESP and tracer for the F7 withers, dungeon bats, and mobs added with `/ao highlight`
 - Starred mobs and teammate glow are left to Odin's Highlight

Spirit Bear (C)
 - Odin's Spirit Bear HUD plus a bear highlight and tracer (replaces Odin's Spirit Bear)

Livid Solver (C)
 - Odin's Livid Solver plus ESP and a tracer to the right Livid (replaces Odin's Livid Solver)

Secret Hitboxes
 - Full block or 1.8 style hitboxes for levers, buttons, skulls and chests in dungeons

Fuck Diorite
 - Renders the Storm pillars as glass, pillar colored, one color or random
 - Render only, the client world keeps the diorite

Auto GFS
 - Refills pearls, jerries, superbooms and leaps from sacks on dungeon start or a timer
 - Gets an Architect's First Draft from sacks when you fail a puzzle (from NoammAddons' Architect Draft)

Escrow Fix
 - Reopens the AH or BZ after escrow closes it

Ghosts
 - Shows invisible Dwarven Mines ghosts and hides their powered layer

Camera Helper
 - Camera clip and custom third person distance

## Fixes
Door Fix
 - Fixes the iron door rotations in F7 P3 section 3

Mod Hider
 - Stops servers from detecting mods through translation keys in signs and anvils

Item Count Fix
 - Shows item counts on unstackable items in menus

Disconnect Fix
 - Stops corrupted entity data (Tarantula slayer) from disconnecting you

Double Use Fix
 - Stops Blaze Daggers and fishing rods from activating twice when clicking a block

## Commands
`/ao trackmutation start|stop <jellybean|thunderling|aloe>`
 - Logs crop collections on start and stop
 - On stop, copies collection gained per crop and NPC crop profit to clipboard

`/ao best [floor]`
 - Adds up your PB splits into a theoretical best run (defaults to current floor, else M7)

`/ao sounds`
 - Opens the Sound Manager

`/ao waypoints`
 - Opens Odin's waypoint manager (import/export are there)
 - `color <RRGGBBAA>` sets the next placed waypoint's color

`/ao ac add left|right [key]`, `/ao ac remove left|right`, `/ao ac clear left|right|all`, `/ao ac list`
 - Manages the Auto Clicker whitelist for the held item

`/ao highlight add <name> [#RRGGBB]`, `/ao highlight remove <name>`, `/ao highlight clear|list`
 - Manages the mobs Highlight (C) draws

`/ao calc <type> ...`
 - `cata <start> <end>`, `skill <start> <end>`: XP needed between levels (decimals allowed)
 - `pet <tier> <start> <end>`: pet XP, legendary up to 200
 - `drop <base %> [looting] [magic find] [pet luck]`: drop chance
 - `coin <gems> <coins> [cookie price]`: coins per gem, compared to booster cookies
 - `usd <coins> <cookie price>`: rough USD value of coins
 - `tax <price>`: auction list and claim fees
 - Amounts accept k/m/b (e.g. `1.5m`)

## Planned
Dungeons Auto GFS
 - Get 2 TWAP after lightning
 - Get 2 TWAP after terminals
 - Get 6 TWAP after goldor
 - Get 6 TWAP on dragon spawn

Warp Cooldown
 - Mineshaft warp cooldown

Ability Timers
 - Shows how long Rag Axe buff lasts
 - Shows how long Reaper buff lasts

Queue Equipment
 - Same as Queue Loadout but for Equipments

Auto Gloomlock
 - When low mana
 - When low health
