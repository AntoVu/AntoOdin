// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.P3Section
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawWireFrameBox
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB

object GateHighlight : Module(
    name = "Gate Highlight",
    description = "Highlights the F7 P3 gate of your section until it is destroyed.",
    category = Skit.NOAMM
) {
    enum class Mode { OUTLINE, FILL, FILLED_OUTLINE }

    private val mode by SelectorSetting("Mode", Mode.FILLED_OUTLINE, desc = "How the gate is drawn.")
    private val fillColor by ColorSetting("Fill Color", Color(0, 134, 255, 50 / 255f), true, desc = "Fill color of the gate.").withDependency { mode != Mode.OUTLINE }
    private val outlineColor by ColorSetting("Outline Color", Color(0, 134, 255), true, desc = "Outline color of the gate.").withDependency { mode != Mode.FILL }
    private val lineWidth by NumberSetting("Line Width", 2f, 1.0..10.0, 0.1f, desc = "Outline thickness.").withDependency { mode != Mode.FILL }
    private val phase by BooleanSetting("Phase", false, desc = "Draws the gate through walls.")

    private class Gate(val pos: BlockPos, val box: AABB)

    private val gates = mapOf(
        1 to Gate(BlockPos(103, 134, 123), AABB(95.0, 114.0, 122.0, 106.0, 134.0, 124.0)),
        2 to Gate(BlockPos(17, 134, 135), AABB(18.0, 114.0, 127.0, 19.0, 134.0, 138.0)),
        3 to Gate(BlockPos(5, 134, 49), AABB(14.0, 114.0, 51.0, 1.0, 134.0, 49.0))
    )

    init {
        on<RenderExtractEvent> {
            val gate = gates[P3Section.current()] ?: return@on
            val block = mc.level?.getBlockState(gate.pos)?.block ?: return@on
            if (block != Blocks.CRACKED_STONE_BRICKS && block != Blocks.INFESTED_STONE_BRICKS) return@on

            if (mode != Mode.OUTLINE) drawFilledBox(gate.box, fillColor, !phase)
            if (mode != Mode.FILL) drawWireFrameBox(gate.box, outlineColor, lineWidth, !phase)
        }
    }
}
