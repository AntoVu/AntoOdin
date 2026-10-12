package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.odinclient.SecretHitboxes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
// The block outline matches the extended secret block shape (moved here from LevelRenderer in 26.2)
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @WrapOperation(method = "extractBlockOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape extendSecretOutline(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, Operation<VoxelShape> original) {
        VoxelShape shape = SecretHitboxes.getShape(state);
        return shape != null ? shape : original.call(state, level, pos, context);
    }
}
