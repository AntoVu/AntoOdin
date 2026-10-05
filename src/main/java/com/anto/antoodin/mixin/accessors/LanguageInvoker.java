package com.anto.antoodin.mixin.accessors;

import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Language.class)
public interface LanguageInvoker {
    @Invoker
    static Language invokeLoadDefault() {
        throw new UnsupportedOperationException();
    }
}
