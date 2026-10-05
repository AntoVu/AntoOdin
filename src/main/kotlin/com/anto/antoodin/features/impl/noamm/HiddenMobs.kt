// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Giant

object HiddenMobs : Module(
    name = "Hidden Mobs",
    description = "Reveals invisible mobs in dungeons.",
    category = Skit.NOAMM
) {
    private val showFels by BooleanSetting("Show Fels", false, desc = "Reveals invisible Fels.")
    private val showShadowAssassins by BooleanSetting("Show Shadow Assassins", true, desc = "Reveals invisible Shadow Assassins.")
    private val showStealthy by BooleanSetting("Show Stealthy", false, desc = "Reveals invisible blood mobs and Giants.")

    // Blood room mob names, from NoammAddons data/watcherMobsNames.json
    private val watcherMobs = setOf(
        "Revoker", "Psycho", "Reaper", "Cannibal", "Mute", "Ooze", "Putrid", "Freak", "Leech", "Tear", "Parasite",
        "Flamer", "Skull", "Mr. Dead", "Vader", "Frost", "Walker", "Wandering Soul", "Bonzo", "Scarf", "Livid",
        "Spirit Bear", "Giant"
    )

    init {
        // Posted after the data is applied, so this undoes the invisible flag every time the server sends it
        on<EntityEvent.SetData> {
            if (!entity.isInvisible || !DungeonUtils.inDungeons) return@on
            val name = entity.displayName?.string?.trim() ?: return@on

            val reveal = when (val e = entity) {
                is EnderMan -> showFels && name == "Dinnerbone"
                is AbstractClientPlayer -> (showShadowAssassins && "Shadow Assassin" in name) || (showStealthy && name in watcherMobs)
                is Giant -> showStealthy && !e.getItemBySlot(EquipmentSlot.FEET).isEmpty
                else -> false
            }
            if (reveal) entity.isInvisible = false
        }
    }
}
