// Feature idea from SkyHanni (LGPL-2.1). Written from scratch for AntoOdin, no SkyHanni code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.alert
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket

object ShadowAssassinNotify : Module(
    name = "Shadow Assassin Notify",
    description = "Title when a Shadow Assassin is about to teleport to you.",
    category = Skit.ANTO
) {
    private val playSound by BooleanSetting("Play Sound", true, desc = "Plays a sound with the title.")

    init {
        // Hypixel sends a world border with a 10 s warning time right before an assassin jumps.
        // Only in dungeons, and not in the F3/M3 boss, which uses the border for its own fight
        onReceive<ClientboundInitializeBorderPacket> {
            if (warningTime != 10000 || !DungeonUtils.inDungeons) return@onReceive
            if (DungeonUtils.inBoss && DungeonUtils.floor?.floorNumber == 3) return@onReceive
            // Packets arrive on the netty thread
            mc.execute { alert("§cShadow Assassin Jumping!", playSound) }
        }
    }
}
