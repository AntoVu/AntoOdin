# AntoOdin
Simple odin addon for more features I wanted
## Features
Wardrobe Hotkeys (A)
 - Supports Equipment Wardrobe
 - Auto close after use
 - Play sound on equip

Queue Wardrobe
 - Auto equip wardrobe on keybind

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

Auto Experiments
 - First click Delay
 - Auto close Ultrasequencer one early
 - Auto start Ultrasequencer

Drop Guard
 - Block dropping items outside of GUIs during F7/M7 terminals
 - Hold override keybind to drop anyway

Jellybean Hider
 - Hides the Magic Jellybean mutation in the Garden
 - Render only the blocks can still be broken
 - Configurable hide Y level

Aloe Highlight
 - Highlights aloe by growth stage in the Garden
 - Colors for Not Ready, Not Optimal, Ready and Harvest Now

Dungeon Splits
 - Splits HUD with tick time and boss entry split
 - Chat message after each split with tick time and PB comparison
 - Projected run time from your recent runs
 - Time lost to lag at the end of the run

## Noamm
Features ported from [NoammAddons](https://github.com/Noamm9/NoammAddons) by Noamm9 (CC0-1.0), rebuilt on Odin so a
second dungeon/chat/packet library isn't needed. They live in their own "Noamm" category.

Auction Price Input
 - Replaces the auction price sign with a text box
 - Undercut mode subtracts from the lowest BIN
 - Enter confirms the Create and Confirm auction menus

Architect Draft
 - Gets an Architect's First Draft from sacks when you fail a puzzle

I Hate Doors
 - Renders entrance, wither and blood doors as colored glass
 - Render only, the door is still solid and map mods still see it

Hidden Mobs
 - Reveals invisible Shadow Assassins, Fels and stealthy blood mobs

Gate Highlight
 - Highlights the F7 P3 gate of your section until it is destroyed

Door Fix
 - Fixes the iron door rotations in F7 P3 section 3

Mod Hider
 - Stops servers from detecting mods through translation keys in signs and anvils

Snappy Tappy
 - The most recently pressed of two opposing movement keys wins

Mono Audio
 - Plays all game audio through a single channel

Sound Manager
 - Per-sound volume from 0 to 200%, open with `/ao sounds`

Explosive Shot
 - Shows Explosive Shot damage per enemy

Chat Filter
 - Hides useless chat messages, by category
 - Custom patterns with `/ao chathider`

Leap Counter
 - HUD of how many teammates reached your F7 P3 spot, with a title and sound when everyone is there

Maxor's Crystals
 - F7 P1 crystal respawn timer, crystal placement time with PB, unplaced crystal alert

Item Tooltip
 - Lowest BIN, bazaar buy/sell and NPC sell prices on item tooltips (Shift for the whole stack)

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
 - Custom FOV, adjustable Slowness FOV change, no Blindness or Nausea

Rejoin Timer
 - Shows how long ago you were kicked from SkyBlock

Party Finder Extras
 - Level requirement and missing classes on each party's head
 - Member stats (Catacombs level, secrets, MP, S+ PB) and missing classes in the tooltip

## Commands
`/ao trackmutation start|stop <jellybean|thunderling|aloe>`
 - Logs crop collections on start and stop
 - On stop, copies collection gained per crop and NPC crop profit to clipboard

`/ao best [floor]`
 - Adds up your PB splits into a theoretical best run (defaults to current floor, else M7)

`/ao sounds`
 - Opens the Sound Manager

`/ao chathider add|remove <regex>`, `/ao chathider list`
 - Manages custom Chat Filter patterns (matched against the whole message)

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

Queue Loadout
 - Same as Queue Wardrobe but for Loadouts

Queue Equipment
 - Same as Queue Wardrobe but for Equipments

Auto Gloomlock
 - When low mana
 - When low health
