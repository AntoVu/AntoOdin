package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.LeapMenuExtras;
import com.odtheking.odin.features.impl.dungeon.LeapMenu;
import com.odtheking.odin.utils.skyblock.dungeon.DungeonPlayer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Odin leaps to the clicked screen quadrant here; with Map Leap shown, the click picks a map marker instead. */
@Mixin(value = LeapMenu.class, remap = false)
public class LeapMenuMixin {
    @Inject(method = "mouseTrigger", at = @At("HEAD"), cancellable = true)
    private void skipQuadrantLeap(AbstractContainerScreen<?> screen, DungeonPlayer player, int quadrant, CallbackInfo ci) {
        if (LeapMenuExtras.INSTANCE.mapLeapShown()) ci.cancel();
    }
}
