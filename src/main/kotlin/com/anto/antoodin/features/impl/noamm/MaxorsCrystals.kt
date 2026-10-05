// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.PersonalBest
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.world.entity.EntityType

object MaxorsCrystals : Module(
    name = "Maxor's Crystals",
    description = "F7 P1 Energy Crystal respawn timer, placement timer and unplaced crystal alert.",
    category = Skit.NOAMM
) {
    private val placeTimer by BooleanSetting("Place Timer", false, desc = "Says in chat how long you took to place your crystal after picking it up.")

    private val pickupRegex = Regex("^(\\w+) picked up an Energy Crystal!")
    private val spawnRegex = Regex("^\\[BOSS] Maxor: (?:THAT BEAM! IT HURTS! IT HURTS!!|YOU TRICKED ME!)$")
    private const val RESPAWN_TICKS = 34
    private const val CRYSTAL_Y = 224

    private val placePBs = PersonalBest(this, "Crystal PBs")

    // Counted down on the netty thread by server ticks
    @Volatile private var respawnTicks: Int? = null
    private var pickupTime: Long? = null

    private val spawnTimerHud by HUD("Spawn Timer", "Server tick timer until the crystals respawn.") { example ->
        val ticks = if (example) RESPAWN_TICKS else respawnTicks ?: return@HUD 0 to 0
        textDim("§b${"%.2f".format(ticks / 20.0)}", 0, 0)
    }

    private val placeAlertHud by HUD("Place Alert", "Warns you while an Energy Crystal sits unplaced in your hotbar.") { example ->
        val holding = mc.player?.inventory?.getItem(8)?.hoverName?.string.equals("Energy Crystal", ignoreCase = true)
        if (!example && !(holding && DungeonUtils.getF7Phase() == M7Phases.P1)) return@HUD 0 to 0
        textDim("§e§l⚠ §b§lCrystal §e§l⚠", 0, 0)
    }

    init {
        on<MessageEvent.Chat> {
            if (spawnRegex.matches(message)) respawnTicks = RESPAWN_TICKS
            if (pickupRegex.find(message)?.groupValues?.get(1) == mc.user.name) pickupTime = System.currentTimeMillis()
        }

        on<TickEvent.Server> {
            respawnTicks = respawnTicks?.let { if (it > 0) it - 1 else null }
        }

        // Your crystal appears at the pad right next to you when you place it
        on<EntityEvent.Add> {
            val pickedUp = pickupTime ?: return@on
            if (!placeTimer || entity.type != EntityType.END_CRYSTAL || entity.y.toInt() != CRYSTAL_Y) return@on
            val player = mc.player ?: return@on
            val dx = entity.x - player.x
            val dz = entity.z - player.z
            if (dx * dx + dz * dz >= 25) return@on

            pickupTime = null
            placePBs.time("Crystal", (System.currentTimeMillis() - pickedUp) / 1000f, message = "§aCrystal placed in §e")
        }

        on<LevelEvent.Load> {
            respawnTicks = null
            pickupTime = null
        }
    }
}
