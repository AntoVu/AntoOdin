// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.anto.CleanMap;
import com.odtheking.odin.features.impl.dungeon.map.WorldScan;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Odin's room scan runs (or is queued until the dungeon is known) per chunk; scan the doors of unseen rooms too. */
@Mixin(value = WorldScan.class, remap = false)
public class WorldScanMixin {
    @Inject(method = "scanChunk", at = @At("TAIL"))
    private void scanDoor(LevelChunk chunk, CallbackInfo ci) {
        CleanMap.INSTANCE.scanDoor(chunk);
    }
}
