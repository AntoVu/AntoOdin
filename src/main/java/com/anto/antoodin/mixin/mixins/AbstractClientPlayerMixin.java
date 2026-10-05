package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.CameraTweaks;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    // The movement speed attribute is what makes Slowness narrow the FOV
    @ModifyExpressionValue(method = "getFieldOfViewModifier", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double scaleSlownessFov(double speed) {
        return CameraTweaks.fovMovementSpeed((AbstractClientPlayer) (Object) this, speed);
    }
}
