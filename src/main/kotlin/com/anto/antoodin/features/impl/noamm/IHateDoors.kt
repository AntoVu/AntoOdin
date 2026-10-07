// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.LocationChangeEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.core.SectionPos
import net.minecraft.world.item.DyeColor
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
    // Labels match the old string options, so saved choices load
    enum class Glass(val color: DyeColor?) {
        CLEAR(null), WHITE(DyeColor.WHITE), BLACK(DyeColor.BLACK), CYAN(DyeColor.CYAN), LIGHT_BLUE(DyeColor.LIGHT_BLUE),
        RED(DyeColor.RED), PINK(DyeColor.PINK), ORANGE(DyeColor.ORANGE), MAGENTA(DyeColor.MAGENTA), YELLOW(DyeColor.YELLOW),
        LIME(DyeColor.LIME), GRAY(DyeColor.GRAY), LIGHT_GRAY(DyeColor.LIGHT_GRAY), PURPLE(DyeColor.PURPLE), BLUE(DyeColor.BLUE),
        BROWN(DyeColor.BROWN), GREEN(DyeColor.GREEN);

        val state: BlockState get() = (color?.let(Blocks.STAINED_GLASS::pick) ?: Blocks.GLASS).defaultBlockState()
    }

    private val glassEntrance by BooleanSetting("Glass Entrance Door", true, desc = "Renders the entrance door as glass.")
    private val entranceGlass by SelectorSetting("Entrance Door Glass", Glass.CLEAR, desc = "Glass used for the entrance door.").withDependency { glassEntrance }
    private val glassWither by BooleanSetting("Glass Wither Door", true, desc = "Renders wither doors as glass.")
    private val witherGlass by SelectorSetting("Wither Door Glass", Glass.BLACK, desc = "Glass used for wither doors.").withDependency { glassWither }
    private val glassBlood by BooleanSetting("Glass Blood Door", true, desc = "Renders the blood door as glass.")
    private val bloodGlass by SelectorSetting("Blood Door Glass", Glass.RED, desc = "Glass used for the blood door.").withDependency { glassBlood }

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
            if (glassEntrance) put(Blocks.INFESTED_CHISELED_STONE_BRICKS, entranceGlass.state)
            if (glassWither) put(Blocks.COAL_BLOCK, witherGlass.state)
            if (glassBlood) put(Blocks.DYED_TERRACOTTA.pick(DyeColor.RED), bloodGlass.state)
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
                mc.levelExtractor.setSectionDirty(x, ySection, z)
    }
}
