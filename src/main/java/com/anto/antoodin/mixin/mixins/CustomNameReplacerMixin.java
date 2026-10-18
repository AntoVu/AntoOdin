package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.fixes.NameReplacerOff;
import com.odtheking.odin.features.impl.render.CustomNameReplacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Odin's FontMixin skips all name replacement when this returns false. */
@Mixin(value = CustomNameReplacer.class, remap = false)
public class CustomNameReplacerMixin {
    @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true)
    private static void disable(CallbackInfoReturnable<Boolean> cir) {
        if (NameReplacerOff.shouldDisable()) cir.setReturnValue(false);
    }
}
