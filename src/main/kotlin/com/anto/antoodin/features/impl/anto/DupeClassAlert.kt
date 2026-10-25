// Feature idea from NoFrills and Devonian (GPL-3.0). Written from scratch for AntoOdin, no NoFrills or Devonian code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.alert
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils

object DupeClassAlert : Module(
    name = "Dupe Class Alert",
    description = "Alerts when two or more players in your dungeon run picked the same class.",
    category = Skit.ANTO
) {
    private val playSound by BooleanSetting("Play Sound", true, desc = "Plays a sound with the alert title.")
    private val partyMessage by BooleanSetting("Party Message", false, desc = "Sends the duplicate classes and players to party chat.")

    // Mort's line comes after classes lock in, when the tab list has every teammate's final class
    private const val RUN_START = "[NPC] Mort: Here, I found this map when I first entered the dungeon."

    init {
        on<MessageEvent.Chat> {
            if (message == RUN_START) schedule(10) { check() }
        }
    }

    private fun check() {
        val dupes = DungeonUtils.dungeonTeammates.filter { it.clazz != DungeonClass.EMPTY }
            .groupBy { it.clazz }.filterValues { it.size > 1 }
        if (dupes.isEmpty()) return

        val text = dupes.entries.joinToString(" | ") { (clazz, players) -> "${className(clazz)}: ${players.joinToString(", ") { it.name }}" }
        alert("§cDupe ${dupes.keys.joinToString(", ") { "§${it.colorCode}${className(it)}" }}", playSound)
        modMessage("§cDupe class! §f$text")
        if (partyMessage) mc.player?.connection?.sendCommand("pc Dupe $text")
    }

    private fun className(clazz: DungeonClass) = clazz.name.lowercase().replaceFirstChar { it.uppercase() }
}
