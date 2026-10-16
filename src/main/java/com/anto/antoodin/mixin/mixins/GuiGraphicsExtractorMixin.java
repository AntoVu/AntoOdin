package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.CommandTooltip;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {
    @ModifyExpressionValue(method = "componentHoverEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/HoverEvent$ShowText;value()Lnet/minecraft/network/chat/Component;"))
    private Component addCommand(Component original, @Local(argsOnly = true) Style style) {
        return CommandTooltip.withCommand(original, style);
    }
}
