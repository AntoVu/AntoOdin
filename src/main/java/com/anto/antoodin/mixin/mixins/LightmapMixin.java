package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.CameraTweaks;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Ported from NoammAddons by Noamm9 (CC0-1.0). Vanilla re-renders the lightmap every tick, so toggling applies at once
@Mixin(Lightmap.class)
public abstract class LightmapMixin {
    @Unique private static final Vector3fc WHITE = new Vector3f(1f, 1f, 1f);

    @Inject(method = "render", at = @At("HEAD"))
    private void fullBright(LightmapRenderState state, CallbackInfo ci) {
        if (!CameraTweaks.shouldFullBright()) return;
        state.skyFactor = 1f;
        state.blockFactor = 1f;
        state.brightness = 1f;
        state.nightVisionEffectIntensity = 0f;
        state.darknessEffectScale = 0f;
        state.bossOverlayWorldDarkening = 0f;
        state.blockLightTint = WHITE;
        state.skyLightColor = WHITE;
        state.ambientColor = WHITE;
        state.nightVisionColor = WHITE;
    }
}
