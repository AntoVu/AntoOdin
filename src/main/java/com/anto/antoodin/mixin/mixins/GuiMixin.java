package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.CameraTweaks;
import com.anto.antoodin.features.impl.noamm.CustomScoreboard;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void hideVanillaScoreboard(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (CustomScoreboard.shouldHideVanilla()) ci.cancel();
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void hideNausea(GuiGraphicsExtractor graphics, float strength, CallbackInfo ci) {
        if (CameraTweaks.shouldHideNausea()) ci.cancel();
    }
}
