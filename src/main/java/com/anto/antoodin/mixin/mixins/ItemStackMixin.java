package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.fixes.ItemCountFix;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "limitSize", at = @At("HEAD"), cancellable = true)
    private void keepCount(int maxStackSize, CallbackInfo ci) {
        if (ItemCountFix.skipLimit()) ci.cancel();
    }
}
