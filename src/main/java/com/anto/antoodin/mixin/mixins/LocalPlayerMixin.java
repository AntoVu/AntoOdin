package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.DropGuard;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void onDrop(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        if (DropGuard.INSTANCE.shouldBlockDrop()) cir.setReturnValue(false);
    }
}
