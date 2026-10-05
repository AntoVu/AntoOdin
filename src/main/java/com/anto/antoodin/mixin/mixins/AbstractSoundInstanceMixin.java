package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.SoundManager;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSoundInstance.class)
public class AbstractSoundInstanceMixin {
    @Shadow @Final protected Identifier identifier;

    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    private void applySoundVolume(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * SoundManager.getMultiplier(identifier));
    }
}
