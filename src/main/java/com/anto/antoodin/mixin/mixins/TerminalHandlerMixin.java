package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.TerminalClickLimit;
import com.odtheking.odin.utils.skyblock.dungeon.terminals.terminalhandler.TerminalHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every Odin terminal click (vanilla, Odin and Custom GUI, drop key included) goes through click: Terminal Click Limit can hold it back. */
@Mixin(value = TerminalHandler.class, remap = false)
public class TerminalHandlerMixin {
    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private void limitClicks(int slotIndex, int button, boolean simulateClick, CallbackInfo ci) {
        if (TerminalClickLimit.shouldBlock((TerminalHandler) (Object) this, slotIndex, button)) ci.cancel();
    }
}
