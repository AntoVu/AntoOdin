package com.anto.antoodin.commands

import com.anto.antoodin.features.MutationTracker
import com.anto.antoodin.features.impl.anto.DungeonSplits
import com.anto.antoodin.features.impl.anto.SkyblockWaypointPacks
import com.anto.antoodin.features.impl.anto.SkyblockWaypoints
import com.anto.antoodin.features.impl.anto.SkyblockWaypointsScreen
import com.anto.antoodin.features.impl.noamm.ChatFilter
import com.anto.antoodin.features.impl.noamm.SoundManagerScreen
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.nodes.LiteralNode
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.config.DungeonWaypointConfig
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.network.WebUtils
import com.odtheking.odin.utils.setClipboardContent
import com.odtheking.odin.utils.skyblock.dungeon.Floor
import kotlinx.coroutines.launch

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
    literal("sounds").runs { schedule(1) { mc.setScreen(SoundManagerScreen(null)) } }

    literal("chathider") {
        literal("add").runs { pattern: GreedyString -> ChatFilter.addPattern(pattern.string) }
        literal("remove").executable {
            param("pattern").suggests { ChatFilter.patterns() }
            runs { pattern: GreedyString -> ChatFilter.removePattern(pattern.string) }
        }
        literal("list").runs { ChatFilter.listPatterns() }
    }

    // Separate literals, not literal("waypoints", "wp"): Commodore makes aliases brigadier redirects, which skip the bare `runs`
    literal("waypoints") { waypointCommands() }
    literal("wp") { waypointCommands() }
}

// Mirrors Odin's /dwp
private fun LiteralNode.waypointCommands() {
    runs { schedule(1) { mc.setScreen(SkyblockWaypointsScreen(null)) } }

    literal("fill").runs {
        SkyblockWaypoints.filled = !SkyblockWaypoints.filled
        modMessage("Fill status changed to: ${SkyblockWaypoints.filled}")
    }

    literal("size").runs { sizeX: Double, sizeY: Double, sizeZ: Double ->
        if (listOf(sizeX, sizeY, sizeZ).any { it !in 0.1..5.0 }) return@runs modMessage("§cSize must be between 0.1 and 5.0!")
        SkyblockWaypoints.sizeX = sizeX
        SkyblockWaypoints.sizeY = sizeY
        SkyblockWaypoints.sizeZ = sizeZ
        modMessage("Size changed to: $sizeX, $sizeY, $sizeZ")
    }

    literal("useblocksize").runs {
        SkyblockWaypoints.useBlockSize = !SkyblockWaypoints.useBlockSize
        modMessage("Use block size status changed to: ${SkyblockWaypoints.useBlockSize}")
    }

    literal("depth").runs {
        SkyblockWaypoints.depthCheck = !SkyblockWaypoints.depthCheck
        modMessage("Next waypoint will be added with depth check: ${SkyblockWaypoints.depthCheck}")
    }

    literal("color").runs { hex: String ->
        if (!hex.matches(Regex("[0-9A-Fa-f]{8}"))) return@runs modMessage("Color hex not properly formatted! Use format RRGGBBAA")
        SkyblockWaypoints.color = Color(hex)
        modMessage("Color changed to: $hex")
    }

    literal("export").runs {
        val pack = SkyblockWaypointPacks.packs[SkyblockWaypoints.editPackId]?.filterValues { it.isNotEmpty() }?.toMutableMap()
        val encoded = pack?.let(DungeonWaypointConfig::encodeWaypoints) ?: return@runs modMessage("§cFailed to export waypoints.")
        setClipboardContent(encoded)
        modMessage("Copied pack '${SkyblockWaypoints.editPackId}' to clipboard.")
    }

    literal("import").runs { input: GreedyString? ->
        val text = input?.string?.trim()
        if (text?.startsWith("https://") != true) return@runs SkyblockWaypointPacks.import(text ?: mc.keyboardHandler.clipboard)
        scope.launch {
            val fetched = WebUtils.fetchString(text).getOrNull() ?: return@launch modMessage("§cFailed to fetch $text")
            mc.execute { SkyblockWaypointPacks.import(fetched) }
        }
    }
}
