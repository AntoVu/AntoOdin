package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.DropGuard;
import com.anto.antoodin.features.impl.fixes.DoubleUseFix;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// PASS without sending: startUseItem then goes on to the other packet, so only one of the two is sent
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void skipBlockUse(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (DoubleUseFix.skipBlockUse(player, hand)) cir.setReturnValue(InteractionResult.PASS);
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void skipItemUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (DoubleUseFix.skipItemUse(player, hand)) cir.setReturnValue(InteractionResult.PASS);
    }

    // The drop key's only path: removes the held item locally and sends the drop packet. GUI drops are container clicks
    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void blockDrop(LocalPlayer player, boolean fullStack, CallbackInfo ci) {
        if (DropGuard.INSTANCE.shouldBlockDrop()) ci.cancel();
    }
}
