package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.IHateDoors;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

// Sodium's chunk meshing reads blocks from its own LevelSlice copy. getBlockState(BlockPos) delegates here.
// Skipped when Sodium isn't installed.
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public abstract class SodiumLevelSliceMixin {
    @ModifyReturnValue(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"))
    private BlockState replaceDoorRenderState(BlockState original, int x, int y, int z) {
        return IHateDoors.getRenderState(x, y, z, original);
    }
}
