package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.CameraTweaks;
import com.anto.antoodin.features.impl.odinclient.CameraHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float applyCustomFov(float original) {
        return original * CameraTweaks.fovRatio();
    }

    // Culling uses its own FOV, widen it too or chunks at the screen edges disappear
    @ModifyExpressionValue(method = "createProjectionMatrixForCulling", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float applyCustomCullingFov(float original) {
        return original * CameraTweaks.fovRatio();
    }

    // Camera Helper (based on skies-starred OdinClient): both calls read the CAMERA_DISTANCE attribute
    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double customCameraDistance(LivingEntity instance, Holder<Attribute> attribute, Operation<Double> original) {
        return CameraHelper.distance(original.call(instance, attribute));
    }

    // Skipping the block raycast lets the camera clip through walls
    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void cameraClip(float maxZoom, CallbackInfoReturnable<Float> cir) {
        if (CameraHelper.shouldClip()) cir.setReturnValue(maxZoom);
    }
}
