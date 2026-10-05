package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.PartyFinderExtras;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TAIL so the text draws over the slot's item (Odin's own slot event is at HEAD)
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void drawSlotOverlay(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        PartyFinderExtras.drawSlot(graphics, slot);
    }
}
