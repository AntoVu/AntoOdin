package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.LavaToWater;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// ModifyReturnValue, not a cancellable inject: Sodium's own RETURN hook on get() must still run to animate the sprites
@Mixin(FluidStateModelSet.class)
public abstract class FluidStateModelSetMixin {
    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private FluidModel replaceLava(FluidModel original, FluidState state) {
        return LavaToWater.replaceModel((FluidStateModelSet) (Object) this, state, original);
    }
}
