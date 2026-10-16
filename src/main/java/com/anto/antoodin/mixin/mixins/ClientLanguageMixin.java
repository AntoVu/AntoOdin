package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.LevelColors;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Every Component turns into draw-ready text here: chat, tab, nametags, tooltips, scoreboard
@Mixin(ClientLanguage.class)
public class ClientLanguageMixin {
    @ModifyReturnValue(method = "getVisualOrder(Lnet/minecraft/network/chat/FormattedText;)Lnet/minecraft/util/FormattedCharSequence;", at = @At("RETURN"))
    private FormattedCharSequence recolorLevels(FormattedCharSequence original, FormattedText text) {
        return LevelColors.recolor(original, text);
    }
}
