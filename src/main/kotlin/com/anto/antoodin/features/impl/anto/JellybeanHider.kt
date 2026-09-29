package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.events.LocationChangeEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.SectionPos
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import java.util.function.Predicate

object JellybeanHider : Module(
    name = "Jellybean Hider",
    description = "Hides the Magic Jellybean mutation (sugar cane above Y 76 and its armor stands) in the Garden.",
    category = Skit.ANTO
) {
    private const val MIN_HIDDEN_Y = 76

    init {
        // Chunks are built before Odin knows the area, so rebuild once it does
        on<LocationChangeEvent> {
            if (inGarden()) rebuildSections()
        }
    }

    override fun onEnable() {
        super.onEnable()
        rebuildSections()
    }

    override fun onDisable() {
        super.onDisable()
        rebuildSections()
    }

    private fun inGarden(): Boolean = LocationUtils.isCurrentArea(Island.Garden)

    // Called from chunk build threads
    fun shouldHideBlock(pos: BlockPos): Boolean = enabled && pos.y > MIN_HIDDEN_Y && inGarden()

    fun shouldHideEntity(entity: Entity): Boolean {
        if (!enabled || entity !is ArmorStand || !inGarden()) return false
        val head = entity.getItemBySlot(EquipmentSlot.HEAD)
        return head.`is`(Items.PLAYER_HEAD) && head.hoverName.string.noControlCodes.contains("magicjellybean", ignoreCase = true)
    }

    // Wrapping the model (instead of mixing into SectionCompiler) also works with Sodium, which renders through FRAPI
    fun registerModelHook() {
        ModelLoadingPlugin.register { context ->
            context.modifyBlockModelAfterBake().register { model, ctx ->
                if (ctx.state().`is`(Blocks.SUGAR_CANE)) HiddenSugarCaneModel(model) else model
            }
        }
    }

    private fun rebuildSections() {
        val level = mc.level ?: return
        val player = mc.player ?: return
        val radius = mc.options.renderDistance().get()
        val centerX = SectionPos.blockToSectionCoord(player.blockX)
        val centerZ = SectionPos.blockToSectionCoord(player.blockZ)
        val minY = SectionPos.blockToSectionCoord(MIN_HIDDEN_Y + 1)

        for (x in centerX - radius..centerX + radius)
            for (z in centerZ - radius..centerZ + radius)
                for (y in minY..level.maxSectionY)
                    mc.levelRenderer.setSectionDirty(x, y, z)
    }

    private class HiddenSugarCaneModel(wrapped: BlockStateModel) : WrapperBlockStateModel(wrapped) {
        override fun emitQuads(
            emitter: QuadEmitter, level: BlockAndTintGetter, pos: BlockPos, state: BlockState,
            random: RandomSource, cullTest: Predicate<Direction?>
        ) {
            if (shouldHideBlock(pos)) return
            super.emitQuads(emitter, level, pos, state, random, cullTest)
        }

        override fun createGeometryKey(level: BlockAndTintGetter, pos: BlockPos, state: BlockState, random: RandomSource): Any? {
            if (shouldHideBlock(pos)) return null
            return super.createGeometryKey(level, pos, state, random)
        }
    }
}
