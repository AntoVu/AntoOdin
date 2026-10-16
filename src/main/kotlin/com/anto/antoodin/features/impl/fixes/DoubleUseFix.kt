// Feature idea from NoFrills (GPL-3.0). Written from scratch for AntoOdin, no NoFrills code is used.
package com.anto.antoodin.features.impl.fixes

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.loreString
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.phys.HitResult

/**
 * Right-clicking a block that does nothing sends both a use-on-block and a use-item packet, and Hypixel runs the
 * ability for each. Only one is sent: rods keep the item use (their cast), Blaze daggers keep the block use.
 *
 * @see com.anto.antoodin.mixin.mixins.MultiPlayerGameModeMixin
 */
object DoubleUseFix : Module(
    name = "Double Use Fix",
    description = "Stops Blaze Daggers and fishing rods from activating twice when clicking a block.",
    category = Skit.FIXES,
    toggled = true
) {
    // A rod can't use blocks (levers, chests) while this is on, same as NoFrills
    @JvmStatic
    fun skipBlockUse(player: Player, hand: InteractionHand): Boolean =
        active() && player.getItemInHand(hand).`is`(Items.FISHING_ROD)

    @JvmStatic
    fun skipItemUse(player: Player, hand: InteractionHand): Boolean =
        active() && mc.hitResult?.type == HitResult.Type.BLOCK &&
            player.getItemInHand(hand).loreString.any { "Attunement" in it }

    private fun active() = enabled && LocationUtils.isInSkyblock
}
