// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.Floor
import com.odtheking.odin.utils.toFixed
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents

object RelicSpawnTimer : Module(
    name = "Relic Spawn Timer",
    description = "Counts down until the M7 relics spawn after Necron dies.",
    category = Skit.NOAMM
) {
    // Server ticks from Necron's death sound to the relic spawn
    private const val SPAWN_TICKS = 61

    @Volatile private var necronStarted = false
    @Volatile private var necronDead = false
    // Packets and server ticks arrive on the netty thread, the HUD reads on the render thread
    @Volatile private var ticksLeft = 0

    val relicsSpawned get() = necronDead && ticksLeft == 0

    private val hud by HUD(name, "Time until the relics spawn, in your class color.", false) { example ->
        val ticks = if (example) 25 else ticksLeft.takeIf { it > 0 } ?: return@HUD 0 to 0
        textDim((ticks / 20.0).toFixed(2), 0, 0, DungeonUtils.currentDungeonPlayer.clazz.color)
    }

    init {
        on<MessageEvent.Chat> {
            if (message == "[BOSS] Necron: ARGH!" && DungeonUtils.inBoss && DungeonUtils.floor?.floorNumber == 7) necronStarted = true
        }

        // Necron's death: the one wither hurt sound at volume 15, pitch 1
        onReceive<ClientboundSoundPacket> {
            if (!necronStarted || necronDead || sound.value() != SoundEvents.WITHER_HURT || volume != 15f || pitch != 1f) return@onReceive
            if (DungeonUtils.floor == Floor.M7) ticksLeft = SPAWN_TICKS
            necronDead = true // set last: relicsSpawned reads these from another thread
        }

        on<TickEvent.Server> { if (ticksLeft > 0) ticksLeft-- }

        on<LevelEvent.Load> {
            necronStarted = false
            necronDead = false
            ticksLeft = 0
        }
    }
}
