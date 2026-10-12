package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.odinclient.SecretHitboxes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
// Raycasts (the crosshair target) use the extended secret block shape
@Mixin(ClipContext.class)
public abstract class ClipContextMixin {
    @Inject(method = "getBlockShape", at = @At("RETURN"), cancellable = true)
    private void extendSecretShape(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        VoxelShape shape = SecretHitboxes.getShape(state);
        if (shape != null) cir.setReturnValue(shape);
    }
}
