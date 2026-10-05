// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.LocationChangeEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.core.SectionPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/**
 * Swaps the block the chunk mesher sees, so only rendering changes. The client world (collision, mining,
 * other mods' map scanners) and the server still have the real door block.
 *
 * @see com.anto.antoodin.mixin.mixins.RenderSectionRegionMixin
 * @see com.anto.antoodin.mixin.mixins.SodiumLevelSliceMixin
 */
object IHateDoors : Module(
    name = "I Hate Doors",
    description = "Renders dungeon doors as glass. Only the visuals change, the door is still solid.",
    category = Skit.NOAMM
) {
    private val glassNames = listOf(
        "Clear", "White", "Black", "Cyan", "Light Blue", "Red", "Pink", "Orange", "Magenta",
        "Yellow", "Lime", "Gray", "Light Gray", "Purple", "Blue", "Brown", "Green"
    )
    private val glassBlocks = listOf(
        Blocks.GLASS, Blocks.WHITE_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS,
        Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.RED_STAINED_GLASS, Blocks.PINK_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS,
        Blocks.MAGENTA_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS,
        Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS, Blocks.BROWN_STAINED_GLASS,
        Blocks.GREEN_STAINED_GLASS
    )

    private val glassEntrance by BooleanSetting("Glass Entrance Door", true, desc = "Renders the entrance door as glass.")
    private val entranceGlass by SelectorSetting("Entrance Door Glass", "Clear", glassNames, desc = "Glass used for the entrance door.").withDependency { glassEntrance }
    private val glassWither by BooleanSetting("Glass Wither Door", true, desc = "Renders wither doors as glass.")
    private val witherGlass by SelectorSetting("Wither Door Glass", "Black", glassNames, desc = "Glass used for wither doors.").withDependency { glassWither }
    private val glassBlood by BooleanSetting("Glass Blood Door", true, desc = "Renders the blood door as glass.")
    private val bloodGlass by SelectorSetting("Blood Door Glass", "Red", glassNames, desc = "Glass used for the blood door.").withDependency { glassBlood }

    // Door blocks are at room grid edges: 16 blocks apart starting at -185, 3 wide, y 69-72
    private const val GRID_START = -185
    private const val DOOR_STEP = 16
    private const val DOOR_MIN_Y = 69
    private const val DOOR_MAX_Y = 72

    // Client ticks after entering a dungeon to rebuild again (Sodium skips sections still on their first build)
    private val LATE_REBUILD_TICKS = intArrayOf(20, 100)

    // Read from chunk build threads, so replaced as a whole and never mutated
    @Volatile
    private var renderStates = emptyMap<Block, BlockState>()

    init {
        on<LocationChangeEvent> {
            if (!DungeonUtils.inDungeons) return@on
            rebuildDoors()
            LATE_REBUILD_TICKS.forEach { schedule(it) { markDoorsDirty() } }
        }

        // Settings have no change listener, so rebuild when a door setting changes
        on<TickEvent.End> { updateRenderStates() }
    }

    override fun onEnable() {
        super.onEnable()
        updateRenderStates()
    }

    override fun onDisable() {
        super.onDisable()
        renderStates = emptyMap()
        rebuildDoors()
    }

    private fun updateRenderStates() {
        val next = buildMap {
            if (glassEntrance) put(Blocks.INFESTED_CHISELED_STONE_BRICKS, glassBlocks[entranceGlass].defaultBlockState())
            if (glassWither) put(Blocks.COAL_BLOCK, glassBlocks[witherGlass].defaultBlockState())
            if (glassBlood) put(Blocks.RED_TERRACOTTA, glassBlocks[bloodGlass].defaultBlockState())
        }
        if (next == renderStates) return
        renderStates = next
        rebuildDoors()
    }

    @JvmStatic
    fun getRenderState(x: Int, y: Int, z: Int, original: BlockState): BlockState {
        val glass = renderStates[original.block] ?: return original
        if (y !in DOOR_MIN_Y..DOOR_MAX_Y || !DungeonUtils.inDungeons || DungeonUtils.inBoss) return original

        val gridX = x - GRID_START + 1
        val gridZ = z - GRID_START + 1
        if (gridX !in 0..10 * DOOR_STEP + 2 || gridZ !in 0..10 * DOOR_STEP + 2) return original
        if (gridX % DOOR_STEP > 2 || gridZ % DOOR_STEP > 2) return original
        // Room centers and corners have an even grid sum, doors sit between two rooms
        if ((gridX / DOOR_STEP + gridZ / DOOR_STEP) and 1 == 0) return original

        return glass
    }

    // LocationChangeEvent is posted from the netty thread, and Sodium only allows rebuilds from the render thread
    private fun rebuildDoors() = mc.execute(::markDoorsDirty)

    private fun markDoorsDirty() {
        if (mc.level == null) return
        val minSection = SectionPos.blockToSectionCoord(GRID_START - 1)
        val maxSection = SectionPos.blockToSectionCoord(GRID_START + 10 * DOOR_STEP + 1)
        val ySection = SectionPos.blockToSectionCoord(DOOR_MIN_Y)
        for (x in minSection..maxSection)
            for (z in minSection..maxSection)
                mc.levelRenderer.setSectionDirty(x, ySection, z)
    }
}
