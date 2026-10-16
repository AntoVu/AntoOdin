package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.CommandTooltip;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {
    // Appends to the wrapped lines, not the hover text: SkyHanni replaces the text passed to split with its own
    @ModifyExpressionValue(method = "componentHoverEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"))
    private List<FormattedCharSequence> addCommand(List<FormattedCharSequence> original, @Local(argsOnly = true) Style style) {
        return CommandTooltip.withCommand(original, style);
    }
}
