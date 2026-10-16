package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.UnfocusedTweaks;
import com.anto.antoodin.features.impl.noamm.CameraTweaks;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    // Drives the Nausea screen wobble
    @ModifyVariable(method = "renderLevel", at = @At("STORE"), name = "nauseaIntensity")
    private float hideNausea(float nauseaIntensity) {
        return CameraTweaks.shouldHideNausea() ? 0f : nauseaIntensity;
    }

    // Both world extraction and drawing are gated on this, the HUD isn't
    @ModifyExpressionValue(method = {"extract", "render"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isGameLoadFinished()Z"))
    private boolean skipWorld(boolean original) {
        return original && !UnfocusedTweaks.skipWorld();
    }
}
