package com.anto.antoodin.commands

import com.anto.antoodin.features.Calculators
import com.anto.antoodin.features.MutationTracker
import com.anto.antoodin.features.impl.anto.DungeonSplits
import com.anto.antoodin.features.impl.anto.WaypointPlacer
import com.anto.antoodin.features.impl.noamm.ChatFilter
import com.anto.antoodin.features.impl.noamm.SoundManagerScreen
import com.anto.antoodin.features.impl.odinclient.AutoClicker
import com.anto.antoodin.features.impl.odinclient.Highlight
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.nodes.LiteralNode
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.features.impl.render.waypoints.WaypointScreen
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.skyblock.dungeon.Floor

val aoCommand = Commodore("ao") {
    literal("trackmutation") {
        literal("start").executable {
            param("type").suggests { MutationTracker.MUTATIONS.keys }
            runs { type: String -> MutationTracker.start(type.lowercase()) }
        }
        literal("stop").executable {
            param("type").suggests { MutationTracker.MUTATIONS.keys }
            runs { type: String -> MutationTracker.stop(type.lowercase()) }
        }
    }

    literal("best").executable {
        param("floor").suggests { Floor.entries.map { it.name } }
        runs { floor: String? -> DungeonSplits.printBest(floor) }
    }

    // Opened next tick, after chat closes, so the chat screen doesn't replace it
    literal("sounds").runs { schedule(1) { mc.gui.setScreen(SoundManagerScreen(null)) } }

    literal("chathider") {
        literal("add").runs { pattern: GreedyString -> ChatFilter.addPattern(pattern.string) }
        literal("remove").executable {
            param("pattern").suggests { ChatFilter.patterns() }
            runs { pattern: GreedyString -> ChatFilter.removePattern(pattern.string) }
        }
        literal("list").runs { ChatFilter.listPatterns() }
    }

    literal("ac") {
        literal("add") { for (side in listOf("left", "right")) literal(side).runs { AutoClicker.add(side) } }
        literal("remove") { for (side in listOf("left", "right")) literal(side).runs { AutoClicker.remove(side) } }
        literal("clear") { for (side in listOf("left", "right", "all")) literal(side).runs { AutoClicker.clear(side) } }
        literal("list").runs { AutoClicker.list() }
    }

    literal("highlight") {
        literal("add").runs { arg: GreedyString -> Highlight.add(arg.string) }
        literal("remove").executable {
            param("name").suggests { Highlight.names() }
            runs { name: GreedyString -> Highlight.remove(name.string) }
        }
        literal("clear").runs { Highlight.clear() }
        literal("list").runs { Highlight.list() }
    }

    // Nullable greedy args, so Commodore never rejects the input and Calculators prints its own usage
    literal("calc") {
        runs { Calculators.run("", emptyList()) }
        for (type in Calculators.TYPES) literal(type).runs { args: GreedyString? ->
            Calculators.run(type, args?.string?.split(' ')?.filter { it.isNotBlank() }.orEmpty())
        }
    }

    // Separate literals, not literal("waypoints", "wp"): Commodore makes aliases brigadier redirects, which skip the bare `runs`
    literal("waypoints") { waypointCommands() }
    literal("wp") { waypointCommands() }
}

// Odin's waypoint manager, plus the next placed waypoint's color
private fun LiteralNode.waypointCommands() {
    runs { schedule(1) { mc.setScreenAndShow(WaypointScreen(null)) } }

    literal("color").runs { hex: String ->
        if (!hex.matches(Regex("[0-9A-Fa-f]{8}"))) return@runs modMessage("Color hex not properly formatted! Use format RRGGBBAA")
        WaypointPlacer.color = Color(hex)
        modMessage("Color changed to: $hex")
    }
}
