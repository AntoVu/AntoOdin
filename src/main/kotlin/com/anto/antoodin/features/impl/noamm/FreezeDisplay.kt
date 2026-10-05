// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils

object FreezeDisplay : Module(
    name = "Freeze Display",
    description = "Shows how long the server has been frozen once it passes a threshold.",
    category = Skit.NOAMM
) {
    private val color by ColorSetting("Color", Color(245, 73, 39), desc = "Text color.")
    private val threshold by NumberSetting("Threshold", 500, 100, 2000, 100, desc = "Milliseconds without a server tick before the display shows.", unit = "ms")
    private val dungeonsOnly by BooleanSetting("Only in Dungeons", true, desc = "Only shows the display in dungeons.")

    // Set on the netty thread, so client lag spikes don't count as server freezes
    @Volatile private var lastServerTick = System.currentTimeMillis()

    private val hud by HUD(name, "Shows how long the server has been frozen.", false) { example ->
        val frozenFor = System.currentTimeMillis() - lastServerTick
        val show = example || (frozenFor > threshold && !mc.isLocalServer && (!dungeonsOnly || DungeonUtils.inDungeons))
        if (!show) return@HUD 0 to 0
        textDim(if (example) "567ms" else "${frozenFor}ms", 0, 0, color)
    }

    init {
        on<TickEvent.Server> { lastServerTick = System.currentTimeMillis() }
        on<LevelEvent.Load> { lastServerTick = System.currentTimeMillis() }
    }
}
