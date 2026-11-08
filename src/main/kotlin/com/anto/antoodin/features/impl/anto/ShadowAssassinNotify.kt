// Feature idea from SkyHanni (LGPL-2.1). Written from scratch for AntoOdin, no SkyHanni code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket
import net.minecraft.sounds.SoundEvents

object ShadowAssassinNotify : Module(
    name = "Shadow Assassin Notify",
    description = "Alert when a Shadow Assassin is about to teleport to you.",
    category = Skit.ANTO
) {
    private const val TEXT = "§cShadow Assassin Jumping!"
    private const val SHOW_MS = 2000L

    private val playSound by BooleanSetting("Play Sound", true, desc = "Plays SkyHanni's beep (XP orb pickup) with the alert.")

    @Volatile private var shownAt = 0L

    private val hud by HUD(name, "Shows when a Shadow Assassin is about to jump.", false) { example ->
        if (!example && System.currentTimeMillis() - shownAt > SHOW_MS) return@HUD 0 to 0
        textDim(TEXT, 0, 0)
    }

    init {
        // Hypixel sends a world border with a 10 s warning time right before an assassin jumps.
        // Only in dungeons, and not in the F3/M3 boss, which uses the border for its own fight
        onReceive<ClientboundInitializeBorderPacket> {
            if (warningTime != 10000 || !DungeonUtils.inDungeons) return@onReceive
            if (DungeonUtils.inBoss && DungeonUtils.floor?.floorNumber == 3) return@onReceive
            shownAt = System.currentTimeMillis()
            // Packets arrive on the netty thread
            if (playSound) mc.execute { mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 1f)) }
        }
    }
}
