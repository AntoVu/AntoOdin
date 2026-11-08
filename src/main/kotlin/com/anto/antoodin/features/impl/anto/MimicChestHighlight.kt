// Based on OdinLegacy's Mimic Box (Copyright (c) 2023-2025, odtheking, BSD 3-Clause)
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.BoxStyle
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.phys.AABB

/**
 * The mimic hides in a trapped chest; like OdinLegacy, every trapped chest during clear counts as it. Gated like
 * Odin's Mimic module: F6/F7 only (where mimics spawn), during clear, until the mimic is killed.
 */
object MimicChestHighlight : Module(
    name = "Mimic Chest Highlight",
    description = "Highlights the mimic chest (a trapped chest) in F6/F7.",
    category = Skit.ANTO
) {
    private val color by ColorSetting("Color", Color(255, 85, 85, 0.5f), true, desc = "The color of the box.")
    private val style by SelectorSetting("Style", BoxStyle.FILLED_OUTLINE, desc = "Style of the box.")
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to see the chest through walls.")

    private var chests = emptyList<BlockPos>()
    private var ticks = 0

    init {
        // Block entities have no add event, so rescan the loaded dungeon chunks once a second
        on<TickEvent.End> {
            if (ticks++ % 20 != 0) return@on
            chests = if (active()) scan() else emptyList()
        }

        on<LevelEvent.Load> { chests = emptyList() }

        on<RenderExtractEvent> {
            if (chests.isEmpty() || !active()) return@on
            val level = mc.level ?: return@on
            for (pos in chests) {
                // Opening it turns the chest into the mimic, so skip it as soon as the block is gone
                if (!level.getBlockState(pos).`is`(Blocks.TRAPPED_CHEST)) continue
                drawStyledBox(AABB(pos.x + 1 / 16.0, pos.y.toDouble(), pos.z + 1 / 16.0, pos.x + 15 / 16.0, pos.y + 14 / 16.0, pos.z + 15 / 16.0), color, style, depthCheck)
            }
        }
    }

    private fun active() = DungeonUtils.inClear && DungeonUtils.isFloor(6, 7) && !DungeonUtils.mimicKilled

    private fun scan(): List<BlockPos> {
        val level = mc.level ?: return emptyList()
        val player = mc.player ?: return emptyList()
        val cx = player.blockX shr 4
        val cz = player.blockZ shr 4
        val found = ArrayList<BlockPos>()
        // A dungeon is at most 6x6 rooms of 32 blocks, so 12 chunks out covers it from anywhere inside
        for (x in cx - 12..cx + 12) for (z in cz - 12..cz + 12) {
            val chunk = level.chunkSource.getChunk(x, z, ChunkStatus.FULL, false) ?: continue
            chunk.blockEntities.values.forEach { if (it is TrappedChestBlockEntity) found += it.blockPos }
        }
        return found
    }
}
