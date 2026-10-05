package com.anto.antoodin.mixin.mixins;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {
    @Inject(method = "play", at = @At("HEAD"))
    private void recordSound(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        com.anto.antoodin.features.impl.noamm.SoundManager.recordPlayedSound(instance);
    }
}
