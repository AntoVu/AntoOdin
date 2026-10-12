// Taken from OdinFabric (Copyright (c) 2023-2025, odtheking, BSD 3-Clause)
// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.core.SectionPos
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/**
 * OdinClient wrote glass into the client world every tick. This swaps the block only for the chunk mesher instead
 * (same hook as I Hate Doors), so collision and other mods' scanners still see diorite.
 *
 * @see com.anto.antoodin.mixin.mixins.RenderSectionRegionMixin
 * @see com.anto.antoodin.mixin.mixins.SodiumLevelSliceMixin
 */
object FuckDiorite : Module(
    name = "Fuck Diorite",
    description = "Renders the pillars in the storm fight as glass.",
    category = Skit.ODINCLIENT
) {
    // Labels match OdinClient's saved option strings
    enum class GlassColor(val color: DyeColor?) {
        NONE(null), WHITE(DyeColor.WHITE), ORANGE(DyeColor.ORANGE), MAGENTA(DyeColor.MAGENTA), LIGHT_BLUE(DyeColor.LIGHT_BLUE),
        YELLOW(DyeColor.YELLOW), LIME(DyeColor.LIME), PINK(DyeColor.PINK), GRAY(DyeColor.GRAY), LIGHT_GRAY(DyeColor.LIGHT_GRAY),
        CYAN(DyeColor.CYAN), PURPLE(DyeColor.PURPLE), BLUE(DyeColor.BLUE), BROWN(DyeColor.BROWN), GREEN(DyeColor.GREEN),
        RED(DyeColor.RED), BLACK(DyeColor.BLACK)
    }

    private val schitzo by BooleanSetting("Schitzo mode", false, desc = "Schtizoing.")
    private val pillarBasedColor by BooleanSetting("Pillar Based", true, desc = "Swaps the diorite in the pillar to a corresponding color.").withDependency { !schitzo }
    private val colorIndex by SelectorSetting("Color", GlassColor.NONE, desc = "Color for the stained glass.").withDependency { !pillarBasedColor && !schitzo }

    private val GLASS = Blocks.GLASS.defaultBlockState()
    private val STAINED = DyeColor.entries.map { Blocks.STAINED_GLASS.pick(it).defaultBlockState() }

    // Pillar bases, each 7x7 wide and 38 tall
    private val pillars = arrayOf(intArrayOf(46, 41), intArrayOf(46, 65), intArrayOf(100, 65), intArrayOf(100, 41))
    private val pillarColors = arrayOf(DyeColor.LIME, DyeColor.YELLOW, DyeColor.PURPLE, DyeColor.RED)
    private const val MIN_Y = 169
    private const val MAX_Y = MIN_Y + 37

    private var lastConfig: Any? = null

    init {
        // Settings have no change listener, so rebuild when one changes
        on<TickEvent.End> {
            val config = Triple(schitzo, pillarBasedColor, colorIndex)
            if (config != lastConfig) {
                lastConfig = config
                rebuild()
            }
        }
    }

    override fun onDisable() {
        super.onDisable()
        lastConfig = null
        rebuild()
    }

    /** Called from chunk build threads, so it only reads fields. */
    @JvmStatic
    fun getRenderState(x: Int, y: Int, z: Int, original: BlockState): BlockState {
        if (!enabled || y !in MIN_Y..MAX_Y) return original
        if (!original.`is`(Blocks.DIORITE) && !original.`is`(Blocks.POLISHED_DIORITE)) return original
        val pillar = pillars.indexOfFirst { x in it[0] - 3..it[0] + 3 && z in it[1] - 3..it[1] + 3 }
        if (pillar == -1 || !DungeonUtils.inDungeons || !DungeonUtils.isFloor(7)) return original

        return when {
            // Random per block, but stable across rebuilds
            schitzo -> STAINED[Math.floorMod((x * 31 + y) * 31 + z, STAINED.size)]
            pillarBasedColor -> STAINED[pillarColors[pillar].ordinal]
            else -> colorIndex.color?.let { STAINED[it.ordinal] } ?: GLASS
        }
    }

    // Sodium only allows rebuilds from the render thread
    private fun rebuild() = mc.execute {
        if (mc.level == null) return@execute
        for ((px, pz) in pillars.map { it[0] to it[1] })
            for (sx in SectionPos.blockToSectionCoord(px - 3)..SectionPos.blockToSectionCoord(px + 3))
                for (sz in SectionPos.blockToSectionCoord(pz - 3)..SectionPos.blockToSectionCoord(pz + 3))
                    for (sy in SectionPos.blockToSectionCoord(MIN_Y)..SectionPos.blockToSectionCoord(MAX_Y))
                        mc.levelExtractor.setSectionDirty(sx, sy, sz)
    }
}
