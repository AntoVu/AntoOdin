package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.LavaToWater;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import net.minecraft.client.renderer.fog.environment.WaterFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LavaFogEnvironment.class)
public abstract class LavaFogEnvironmentMixin {
    @Unique private static final WaterFogEnvironment antoodin$waterFog = new WaterFogEnvironment();

    // Pushes the fog out to the render distance and makes it transparent
    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void hideLavaFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!LavaToWater.shouldHideFog()) return;
        fog.color.set(fog.color.x, fog.color.y, fog.color.z, 0f);
        fog.environmentalStart = renderDistance;
        fog.environmentalEnd = renderDistance;
        ci.cancel();
    }

    @Inject(method = "getBaseColor", at = @At("HEAD"), cancellable = true)
    private void waterFogColor(ClientLevel level, Camera camera, int renderDistance, float partialTicks, CallbackInfoReturnable<Integer> cir) {
        if (!LavaToWater.isActive()) return;
        Integer tint = LavaToWater.fogColor();
        cir.setReturnValue(tint != null ? tint : antoodin$waterFog.getBaseColor(level, camera, renderDistance, partialTicks));
    }
}
