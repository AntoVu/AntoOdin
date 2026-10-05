package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.ModHider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
    @WrapOperation(method = "slotChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    private String hideTranslationsOnSlotChanged(Component instance, Operation<String> original) {
        return ModHider.getString(instance);
    }

    @WrapOperation(method = "onNameChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    private String hideTranslationsOnNameChanged(Component instance, Operation<String> original) {
        return ModHider.getString(instance);
    }
}
