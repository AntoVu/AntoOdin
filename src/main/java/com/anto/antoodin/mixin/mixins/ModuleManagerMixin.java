package com.anto.antoodin.mixin.mixins;

import com.odtheking.odin.config.ModuleConfig;
import com.odtheking.odin.features.Module;
import com.odtheking.odin.features.ModuleManager;
import com.odtheking.odin.features.impl.skyblock.WardrobeKeybinds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

@Mixin(value = ModuleManager.class, remap = false)
public class ModuleManagerMixin {
    @ModifyVariable(method = "registerModules", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Module[] filterModules(Module[] modules, ModuleConfig config) {
        return Arrays.stream(modules)
                .filter(module -> !(module instanceof WardrobeKeybinds))
                .toArray(Module[]::new);
    }
}