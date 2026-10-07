// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundResetScorePacket
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

/**
 * Keep SkyHanni's Custom Scoreboard off, or there will be two.
 *
 * @see com.anto.antoodin.mixin.mixins.GuiMixin
 */
object CustomScoreboard : Module(
    name = "Custom Scoreboard",
    description = "Draws a restyled scoreboard instead of the vanilla one.",
    category = Skit.NOAMM
) {
    private val hideServerId by BooleanSetting("Hide Server ID", false, desc = "Removes the server ID (e.g. m151AM) from the date line.")
    private val accentColor by ColorSetting("Accent Color", Color(0, 134, 255), desc = "Color of the bar along the top.")

    private const val PADDING = 8
    private const val MAX_LINES = 15
    private val background = Color(15, 15, 15, 190 / 255f)
    private val border = Color(255, 255, 255, 20 / 255f)

    // Set from the netty thread when the scoreboard changes, rebuilt on the render thread
    @Volatile private var dirty = true
    private var title: Component = Component.empty()
    private var lines = emptyList<Component>()

    private val hud by HUD(name, "The restyled scoreboard.", false) { example ->
        if (example) {
            draw(this, Component.literal("§e§lSKYBLOCK"), listOf("§710/04/26 §8m151AM", "", "§fPurse: §61,234,567").map(Component::literal))
        } else {
            if (dirty) rebuild()
            if (lines.isEmpty()) 0 to 0 else draw(this, title, lines)
        }
    }

    init {
        onReceive<ClientboundSetScorePacket> { dirty = true }
        onReceive<ClientboundResetScorePacket> { dirty = true }
        onReceive<ClientboundSetObjectivePacket> { dirty = true }
        onReceive<ClientboundSetDisplayObjectivePacket> { dirty = true }
        onReceive<ClientboundSetPlayerTeamPacket> { dirty = true }
    }

    @JvmStatic
    fun shouldHideVanilla(): Boolean = enabled

    private fun rebuild() {
        dirty = false
        val scoreboard = mc.level?.scoreboard
        val objective = scoreboard?.getDisplayObjective(DisplaySlot.SIDEBAR)
        if (objective == null) {
            lines = emptyList()
            return
        }

        title = objective.displayName
        lines = scoreboard.listPlayerScores(objective).sortedByDescending { it.value() }.take(MAX_LINES).mapIndexed { index, score ->
            val name = score.ownerName().string
            val line = PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(name), Component.literal(name))
            // The first SkyBlock line is "<date> <server id>"
            if (index == 0 && hideServerId && LocationUtils.isInSkyblock) Component.literal("§7" + line.string.substringBefore(' ')) else line
        }
    }

    private fun draw(graphics: net.minecraft.client.gui.GuiGraphicsExtractor, title: Component, lines: List<Component>): Pair<Int, Int> {
        val font = mc.font
        val lineHeight = font.lineHeight + 2
        val width = maxOf(font.width(title), lines.maxOfOrNull { font.width(it) } ?: 0) + PADDING * 2
        val height = font.lineHeight + 4 + lines.size * lineHeight + PADDING * 2

        graphics.fill(-1, -1, width + 1, height + 1, border.rgba)
        graphics.fill(0, 0, width, height, background.rgba)
        graphics.fill(0, 0, width, 2, accentColor.rgba)

        graphics.centeredText(font, title, width / 2, PADDING, -1)
        lines.forEachIndexed { i, line -> graphics.text(font, line, PADDING, PADDING + font.lineHeight + 4 + i * lineHeight, -1) }
        return width to height
    }
}
