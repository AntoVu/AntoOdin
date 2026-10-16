package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.UnfocusedTweaks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FramerateLimitTracker.class)
public class FramerateLimitTrackerMixin {
    @ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"))
    private int unfocusedLimit(int original) {
        int limit = UnfocusedTweaks.frameLimit();
        return limit < 0 ? original : limit;
    }
}
