package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.CleanMap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.io.InputStream;

/** Serves `antoodin:font/map.ttf` from the installed system font picked in Clean Map. */
@Mixin(TrueTypeGlyphProviderDefinition.class)
public class TrueTypeGlyphProviderDefinitionMixin {
    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/ResourceManager;open(Lnet/minecraft/resources/Identifier;)Ljava/io/InputStream;"))
    private InputStream openSystemFont(ResourceManager manager, Identifier location, Operation<InputStream> original) throws IOException {
        InputStream font = CleanMap.INSTANCE.openFont(location);
        return font != null ? font : original.call(manager, location);
    }
}
