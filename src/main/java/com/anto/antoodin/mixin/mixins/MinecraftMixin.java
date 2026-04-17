package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.CPSDisplay;
import com.anto.antoodin.features.impl.anto.PearlRefill;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"))
    private void onLeftClick(CallbackInfoReturnable<Boolean> cir) {
        CPSDisplay.INSTANCE.onLeftClick();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void onRightClick(CallbackInfo ci) {
        CPSDisplay.INSTANCE.onRightClick();
        PearlRefill.INSTANCE.onRightClick();
    }
}