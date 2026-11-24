package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.JellybeanHider;
import com.anto.antoodin.features.impl.noamm.LeapMenuExtras;
import com.anto.antoodin.features.impl.odinclient.Highlight;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    //~ if >= 26.3 'camZ, Callback' -> 'camZ, float partialTicks, Callback'
    private void onShouldRender(T entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (JellybeanHider.INSTANCE.shouldHideEntity(entity) || LeapMenuExtras.shouldHideEntity(entity) || Highlight.shouldHideEntity(entity)) cir.setReturnValue(false);
    }

    // Highlight (C) Glow style: the outline color is set here, so Odin's Entity.getTeamColor mixin is left alone
    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean forceGlow(boolean original, T entity) {
        return original || Highlight.glowColor(entity) != null;
    }

    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int glowColor(int original, T entity) {
        Integer color = Highlight.glowColor(entity);
        if (color == null) color = Highlight.teammateColor(entity);
        return color != null ? color : original;
    }
}
