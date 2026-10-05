package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.CameraTweaks;
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
}
