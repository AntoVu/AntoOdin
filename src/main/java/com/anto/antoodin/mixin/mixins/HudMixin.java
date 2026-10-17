package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.ItemCustomizer;
import com.anto.antoodin.features.impl.noamm.CameraTweaks;
import com.anto.antoodin.features.impl.noamm.CustomScoreboard;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {
    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void hideVanillaScoreboard(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (CustomScoreboard.shouldHideVanilla()) ci.cancel();
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void hideNausea(GuiGraphicsExtractor graphics, float strength, CallbackInfo ci) {
        if (CameraTweaks.shouldHideNausea()) ci.cancel();
    }

    // The name above the hotbar when switching items
    @WrapOperation(method = "extractSelectedItemName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private Component customName(ItemStack stack, Operation<Component> original) {
        Component name = ItemCustomizer.INSTANCE.customName(stack);
        return name != null ? name : original.call(stack);
    }
}
