package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.ItemCustomizer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Every item render (GUI, hand, dropped, frames) builds its render state here, so swapping the stack restyles it everywhere
@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {
    @ModifyVariable(method = "appendItemLayers", at = @At("HEAD"), argsOnly = true)
    private ItemStack customizeItem(ItemStack stack) {
        return ItemCustomizer.INSTANCE.displayStack(stack);
    }
}
