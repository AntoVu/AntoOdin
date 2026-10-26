package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.fixes.ModHider;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {
    @WrapOperation(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;ZLnet/minecraft/network/chat/Component;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    private String hideTranslations(Component line, Operation<String> original) {
        return ModHider.INSTANCE.getEnabled() ? ModHider.getString(line) : original.call(line);
    }
}
