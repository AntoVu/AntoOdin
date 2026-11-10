package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.CPSDisplay;
import com.anto.antoodin.features.impl.anto.FastLeap;
import com.anto.antoodin.features.impl.anto.PearlRefill;
import com.anto.antoodin.features.impl.anto.RelicUtils;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onLeftClick(CallbackInfoReturnable<Boolean> cir) {
        // Fast Leap swallows the click (no swing, attack or block break) and opens the leap menu instead
        if (FastLeap.INSTANCE.onLeftClick()) {
            cir.setReturnValue(false);
            return;
        }
        CPSDisplay.INSTANCE.onLeftClick();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void onContinueAttack(boolean down, CallbackInfo ci) {
        if (FastLeap.INSTANCE.blocksAttack()) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onRightClick(CallbackInfo ci) {
        if (RelicUtils.shouldBlockUse()) {
            ci.cancel();
            return;
        }
        CPSDisplay.INSTANCE.onRightClick();
        PearlRefill.INSTANCE.onRightClick();
    }
}