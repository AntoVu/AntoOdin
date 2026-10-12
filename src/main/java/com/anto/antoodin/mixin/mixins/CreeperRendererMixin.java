package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.odinclient.Ghosts;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
// Edits the render state after vanilla fills it, which is when isInvisible and isPowered are set
@Mixin(CreeperRenderer.class)
public abstract class CreeperRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/Creeper;Lnet/minecraft/client/renderer/entity/state/CreeperRenderState;F)V", at = @At("RETURN"))
    private void modifyGhost(Creeper creeper, CreeperRenderState state, float partialTick, CallbackInfo ci) {
        Ghosts.modifyRenderState(creeper, state);
    }
}
