package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.ChatRules;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @Inject(method = "addServerSystemMessage", at = @At("HEAD"), cancellable = true)
    private void filterMessage(Component message, CallbackInfo ci) {
        if (ChatRules.shouldHide(message)) ci.cancel();
    }
}
