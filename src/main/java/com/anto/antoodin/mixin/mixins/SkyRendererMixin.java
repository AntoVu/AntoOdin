package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.TimeChanger;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Time Changer: the sky is sampled at the changed time
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void startTimeChange(CallbackInfo ci) {
        TimeChanger.extractingSky = TimeChanger.INSTANCE.getEnabled();
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void endTimeChange(CallbackInfo ci) {
        TimeChanger.extractingSky = false;
    }
}
