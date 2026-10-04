package com.anto.antoodin.commands

import com.anto.antoodin.features.MutationTracker
import com.anto.antoodin.features.impl.anto.DungeonSplits
import com.github.stivais.commodore.Commodore
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
}
