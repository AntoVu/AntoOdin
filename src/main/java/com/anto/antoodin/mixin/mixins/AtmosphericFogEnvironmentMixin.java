package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.TimeChanger;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Time Changer: the fog color follows the changed sky. getBaseColor returns int on 26.2 and Vector3fc on 26.3, hence <?>
@Mixin(AtmosphericFogEnvironment.class)
public class AtmosphericFogEnvironmentMixin {
    @Inject(method = "getBaseColor", at = @At("HEAD"))
    private void startTimeChange(CallbackInfoReturnable<?> cir) {
        TimeChanger.extractingSky = TimeChanger.INSTANCE.getEnabled();
    }

    @Inject(method = "getBaseColor", at = @At("RETURN"))
    private void endTimeChange(CallbackInfoReturnable<?> cir) {
        TimeChanger.extractingSky = false;
    }
}
