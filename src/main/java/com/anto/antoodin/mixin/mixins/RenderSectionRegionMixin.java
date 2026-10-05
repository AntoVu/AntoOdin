package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.IHateDoors;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Vanilla chunk meshing (without Sodium)
@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionMixin {
    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState replaceDoorRenderState(BlockState original, BlockPos pos) {
        return IHateDoors.getRenderState(pos.getX(), pos.getY(), pos.getZ(), original);
    }
}
