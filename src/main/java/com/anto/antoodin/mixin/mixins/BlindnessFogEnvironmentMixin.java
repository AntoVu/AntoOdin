package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.CameraTweaks;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlindnessFogEnvironment.class)
public abstract class BlindnessFogEnvironmentMixin {
    // With no effect to look for, the environment never applies
    @Inject(method = "getMobEffect", at = @At("HEAD"), cancellable = true)
    private void hideBlindness(CallbackInfoReturnable<Holder<MobEffect>> cir) {
        if (CameraTweaks.shouldHideBlindness()) cir.setReturnValue(null);
    }
}
