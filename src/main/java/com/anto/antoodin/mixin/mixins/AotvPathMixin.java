package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.NoAotvPaths;
//? if >= 26.3 {
/*import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
*///?} else {
import net.minecraft.world.item.ShovelItem;
//?}
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// No AOTV Paths: ends the shovel's path code early on the client, with the SUCCESS vanilla returns there
//~ if >= 26.3 'ShovelItem.class' -> 'BlockTransformer.class'
@Mixin(ShovelItem.class)
public class AotvPathMixin {
    //? if >= 26.3 {
    /*// Shovels became a block transformer component; skip before the path block is set and the sound played
    @Inject(method = "transformBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/context/UseOnContext;getPlayer()Lnet/minecraft/world/entity/player/Player;"), cancellable = true)
    private void skipAotvPath(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir, @Local(ordinal = 0) BlockState newState) {
        if (newState.is(Blocks.DIRT_PATH) && context.getLevel().isClientSide() && NoAotvPaths.shouldPrevent(context.getItemInHand()))
            cir.setReturnValue(InteractionResult.SUCCESS);
    }
    *///?} else {
    // Only the sound plays on the client; the path block is placed server side
    @Inject(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"), cancellable = true)
    private void skipAotvPath(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide() && NoAotvPaths.shouldPrevent(context.getItemInHand()))
            cir.setReturnValue(InteractionResult.SUCCESS);
    }
    //?}
}
