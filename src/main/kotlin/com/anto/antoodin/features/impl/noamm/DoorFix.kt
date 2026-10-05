// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.P3Section
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoorHingeSide
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf

object DoorFix : Module(
    name = "Door Fix",
    description = "Fixes the client-side rotation of the iron doors in F7 P3 section 3.",
    category = Skit.NOAMM,
    toggled = true
) {
    private val doorPositions = buildList {
        for (i in 0..6) {
            if (i != 1) add(BlockPos(1, 112 + i * 4, 104))
            if (i < 6) add(BlockPos(1, 113 + i * 4, 86))
            add(BlockPos(1, 112 + i * 4, 68))
        }
    }

    private val doorState = Blocks.IRON_DOOR.defaultBlockState()
        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
        .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)
        .setValue(BlockStateProperties.DOOR_HINGE, DoorHingeSide.LEFT)
        .setValue(BlockStateProperties.OPEN, false)

    init {
        on<TickEvent.End> {
            if (P3Section.current() != 3) return@on
            val level = mc.level ?: return@on
            for (pos in doorPositions) {
                val state = level.getBlockState(pos)
                if (!state.isAir && state != doorState) level.setBlock(pos, doorState, 19)
            }
        }
    }
}
