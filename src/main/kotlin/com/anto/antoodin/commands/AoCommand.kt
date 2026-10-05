package com.anto.antoodin.commands

import com.anto.antoodin.features.MutationTracker
import com.anto.antoodin.features.impl.anto.DungeonSplits
import com.anto.antoodin.features.impl.noamm.ChatFilter
import com.anto.antoodin.features.impl.noamm.SoundManagerScreen
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.handlers.schedule
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
    literal("sounds").runs { schedule(1) { mc.setScreen(SoundManagerScreen(null)) } }

    literal("chathider") {
        literal("add").runs { pattern: GreedyString -> ChatFilter.addPattern(pattern.string) }
        literal("remove").executable {
            param("pattern").suggests { ChatFilter.patterns() }
            runs { pattern: GreedyString -> ChatFilter.removePattern(pattern.string) }
        }
        literal("list").runs { ChatFilter.listPatterns() }
    }
}
