// Wrong cauldron block: idea from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.BlockInteractEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.itemId
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

/**
 * Right clicks go through Odin's [BlockInteractEvent]; left clicks and the menu guard are hooked in
 * [com.anto.antoodin.mixin.mixins.MultiPlayerGameModeMixin] and [com.anto.antoodin.mixin.mixins.MinecraftMixin].
 */
object RelicUtils : Module(
    name = "Relic Utils",
    description = "Blocks placing an M7 relic at the wrong cauldron and opening the SkyBlock menu right after placing.",
    category = Skit.ANTO
) {
    private val blockWrong by BooleanSetting("Block Wrong Cauldron", true, desc = "Cancels clicks on a cauldron or anvil that isn't your relic's.")
    private val menuGuard by BooleanSetting("Menu Guard", true, desc = "Blocks right clicking the SkyBlock menu for a moment after placing a relic.")
    private val guardSeconds by NumberSetting("Guard Time", 2.0, 0.5..5.0, 0.1, desc = "Seconds the SkyBlock menu stays blocked.", unit = "s")
        .withDependency { menuGuard }

    // Relic item id -> its cauldron (the anvil sits in the same column)
    private val cauldrons = mapOf(
        "GREEN_KING_RELIC" to BlockPos(49, 7, 44),
        "PURPLE_KING_RELIC" to BlockPos(54, 7, 41),
        "BLUE_KING_RELIC" to BlockPos(59, 7, 44),
        "ORANGE_KING_RELIC" to BlockPos(57, 7, 42),
        "RED_KING_RELIC" to BlockPos(51, 7, 42),
    )

    private var placedAt = 0L

    init {
        on<BlockInteractEvent> { if (shouldBlockClick(pos)) cancel() }
    }

    /** True if a click on [pos] is a relic going to the wrong cauldron or anvil. Records a correct placement. */
    @JvmStatic
    fun shouldBlockClick(pos: BlockPos): Boolean {
        if (!enabled) return false
        val target = cauldrons[mc.player?.mainHandItem?.itemId] ?: return false
        val block = mc.level?.getBlockState(pos)?.block ?: return false
        if (block != Blocks.CAULDRON && block != Blocks.ANVIL) return false
        if (pos.x == target.x && pos.z == target.z) {
            placedAt = System.currentTimeMillis()
            return false
        }
        return blockWrong
    }

    /** True if this right click would open the SkyBlock menu too soon after placing a relic. */
    @JvmStatic
    fun shouldBlockUse(): Boolean =
        enabled && menuGuard && mc.player?.mainHandItem?.itemId == "SKYBLOCK_MENU" &&
            System.currentTimeMillis() - placedAt < guardSeconds * 1000
}
