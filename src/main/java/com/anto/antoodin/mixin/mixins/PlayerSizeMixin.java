package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.fixes.CosmeticDisabler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.odtheking.odin.features.impl.render.PlayerSize;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Odin scales its dev players from AvatarRenderer.scale whether or not its Player Size module is on. */
@Mixin(value = PlayerSize.class, remap = false)
public class PlayerSizeMixin {
    @Inject(method = "preRenderCallbackScaleHook", at = @At("HEAD"), cancellable = true)
    private static void skipDevSize(AvatarRenderState state, PoseStack matrix, CallbackInfo ci) {
        if (CosmeticDisabler.skipOdinSize(state)) ci.cancel();
    }
}
