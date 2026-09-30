package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LocationChangeEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
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
import net.minecraft.world.level.block.StemBlock
import net.minecraft.world.level.block.state.BlockState
import java.util.function.Predicate

object JellybeanHider : Module(
    name = "Jellybean Hider",
    description = "Hides the Magic Jellybean mutation (sugar cane, melon stem and armor stands) in the Garden.",
    category = Skit.ANTO
) {
    private const val MIN_SLIDER_Y = 67
    private val hideAboveY by NumberSetting("Hide Above Y", 76, MIN_SLIDER_Y, 87, 1, desc = "Sugar cane above this Y is hidden. Jellybean armor stands are hidden one block lower.")

    // Fully grown jellybean has a melon stem at this exact Y
    private const val MELON_STEM_Y = 83
    private const val MELON_STEM_AGE = 6

    // Client ticks after entering the Garden to rebuild again
    private val LATE_REBUILD_TICKS = intArrayOf(20, 100)

    private var appliedY = hideAboveY

    init {
        // Chunks are built before Odin knows the area, so rebuild once it does. Sodium drops rebuilds for
        // sections whose first build is still running (the ones nearest the player), so repeat it after a delay
        on<LocationChangeEvent> {
            if (!inGarden()) return@on
            rebuildSections()
            LATE_REBUILD_TICKS.forEach { schedule(it) { markSectionsDirty() } }
        }

        // Settings have no change listener, so rebuild when the slider moves
        on<TickEvent.End> {
            if (hideAboveY == appliedY) return@on
            appliedY = hideAboveY
            markSectionsDirty()
        }
    }

    override fun onEnable() {
        super.onEnable()
        appliedY = hideAboveY
        rebuildSections()
    }

    override fun onDisable() {
        super.onDisable()
        rebuildSections()
    }

    private fun inGarden(): Boolean = LocationUtils.isCurrentArea(Island.Garden)

    // Called from chunk build threads
    fun shouldHideBlock(pos: BlockPos, state: BlockState): Boolean {
        if (!enabled) return false
        val atHiddenY = if (state.`is`(Blocks.MELON_STEM)) pos.y == MELON_STEM_Y else pos.y > hideAboveY
        return atHiddenY && inGarden()
    }

    private fun isWrappedState(state: BlockState): Boolean =
        state.`is`(Blocks.SUGAR_CANE) || (state.`is`(Blocks.MELON_STEM) && state.getValue(StemBlock.AGE) == MELON_STEM_AGE)

    fun shouldHideEntity(entity: Entity): Boolean {
        // The skull sits a block above the stand's position, so stands are hidden one block lower
        if (!enabled || entity !is ArmorStand || entity.y <= hideAboveY - 1 || !inGarden()) return false
        val head = entity.getItemBySlot(EquipmentSlot.HEAD)
        return head.`is`(Items.PLAYER_HEAD) && head.hoverName.string.noControlCodes.contains("magicjellybean", ignoreCase = true)
    }

    // Wrapping the model (instead of mixing into SectionCompiler) also works with Sodium, which renders through FRAPI
    fun registerModelHook() {
        ModelLoadingPlugin.register { context ->
            context.modifyBlockModelAfterBake().register { model, ctx ->
                if (isWrappedState(ctx.state())) HiddenModel(model) else model
            }
        }
    }

    // LocationChangeEvent is posted from the netty thread, and Sodium only allows rebuilds from the render thread
    private fun rebuildSections() = mc.execute(::markSectionsDirty)

    private fun markSectionsDirty() {
        val level = mc.level ?: return
        val player = mc.player ?: return
        val radius = mc.options.renderDistance().get()
        val centerX = SectionPos.blockToSectionCoord(player.blockX)
        val centerZ = SectionPos.blockToSectionCoord(player.blockZ)
        val minY = SectionPos.blockToSectionCoord(MIN_SLIDER_Y + 1)

        for (x in centerX - radius..centerX + radius)
            for (z in centerZ - radius..centerZ + radius)
                for (y in minY..level.maxSectionY)
                    mc.levelRenderer.setSectionDirty(x, y, z)
    }

    private class HiddenModel(wrapped: BlockStateModel) : WrapperBlockStateModel(wrapped) {
        override fun emitQuads(
            emitter: QuadEmitter, level: BlockAndTintGetter, pos: BlockPos, state: BlockState,
            random: RandomSource, cullTest: Predicate<Direction?>
        ) {
            if (shouldHideBlock(pos, state)) return
            super.emitQuads(emitter, level, pos, state, random, cullTest)
        }

        override fun createGeometryKey(level: BlockAndTintGetter, pos: BlockPos, state: BlockState, random: RandomSource): Any? {
            if (shouldHideBlock(pos, state)) return null
            return super.createGeometryKey(level, pos, state, random)
        }
    }
}
