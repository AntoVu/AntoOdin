// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.core.Direction
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * @see com.anto.antoodin.mixin.mixins.ClipContextMixin
 * @see com.anto.antoodin.mixin.mixins.LevelExtractorMixin
 */
object SecretHitboxes : Module(
    name = "Secret Hitboxes",
    description = "Extends the hitboxes of secret blocks to a full block.",
    category = Skit.ODINCLIENT
) {
    private val lever by BooleanSetting("Lever", false, desc = "Extends the lever hitbox.")
    private val oldLeverStyle by BooleanSetting("1.8 lever hitbox", true, desc = "Use the 1.8 lever hitbox.").withDependency { lever }
    private val button by BooleanSetting("Button", false, desc = "Extends the button hitbox.")
    private val flatButtonStyle by BooleanSetting("Flat button hitbox", false, desc = "Use the flat button hitbox.").withDependency { button }
    private val skull by BooleanSetting("Skulls", false, desc = "Extends the skulls hitbox.")
    private val chests by BooleanSetting("Chests", false, desc = "Extends the chest hitbox.")
    private val onlyTrappedChests by BooleanSetting("Only trapped chests", false, desc = "Only extends the trapped chests hitbox.").withDependency { chests }

    private val LEVER_FLOOR_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0)
    private val LEVER_NORTH_SHAPE = Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0)
    private val LEVER_SOUTH_SHAPE = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0)
    private val LEVER_EAST_SHAPE = Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0)
    private val LEVER_WEST_SHAPE = Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0)

    private fun getOldLeverShape(face: AttachFace, direction: Direction): VoxelShape {
        return if (face == AttachFace.FLOOR || face == AttachFace.CEILING) {
            LEVER_FLOOR_SHAPE
        } else when (direction) {
            Direction.EAST -> LEVER_EAST_SHAPE
            Direction.WEST -> LEVER_WEST_SHAPE
            Direction.SOUTH -> LEVER_SOUTH_SHAPE
            Direction.NORTH -> LEVER_NORTH_SHAPE
            else -> LEVER_FLOOR_SHAPE
        }
    }

    private fun getHackButtonShape(face: AttachFace, direction: Direction, powered: Boolean): VoxelShape {
        val f2 = (if (powered) 1 else 2) / 16.0
        return when (face) {
            AttachFace.CEILING -> Shapes.box(0.0, 1.0 - f2, 0.0, 1.0, 1.0, 1.0)
            AttachFace.FLOOR -> Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + f2, 1.0)
            else -> when (direction) {
                Direction.EAST -> Shapes.box(0.0, 0.0, 0.0, f2, 1.0, 1.0)
                Direction.WEST -> Shapes.box(1.0 - f2, 0.0, 0.0, 1.0, 1.0, 1.0)
                Direction.SOUTH -> Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, f2)
                Direction.NORTH -> Shapes.box(0.0, 0.0, 1.0 - f2, 1.0, 1.0, 1.0)
                Direction.UP -> Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0 + f2, 1.0)
                Direction.DOWN -> Shapes.box(0.0, 1.0 - f2, 0.0, 1.0, 1.0, 1.0)
            }
        }
    }

    /** Extended shape for a secret block in dungeons, or null to keep the vanilla one. */
    @JvmStatic
    fun getShape(state: BlockState): VoxelShape? {
        if (!enabled || !DungeonUtils.inDungeons) return null

        return when (val block = state.block) {
            is SkullBlock if skull -> Shapes.block()
            is ChestBlock if chests -> if (!onlyTrappedChests || block is TrappedChestBlock) Shapes.block() else null
            is LeverBlock if lever -> {
                if (oldLeverStyle) getOldLeverShape(
                    state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE),
                    state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING)
                ) else Shapes.block()
            }
            is ButtonBlock if button -> {
                if (flatButtonStyle) getHackButtonShape(
                    state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE),
                    state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING),
                    state.getValue(ButtonBlock.POWERED)
                ) else Shapes.block()
            }
            else -> null
        }
    }
}
