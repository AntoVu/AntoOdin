package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.fixes.CosmeticDisabler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// kontext (skies-starred's library, bundled in Detexturify) has no setting for donor sizes. Its player mixin asks
// DonatorSize.fn once per player whether to scale them and caches the answer.
// Skipped when kontext isn't installed; require = 0 so a kontext update that changes fn can't crash the game.
@Pseudo
@Mixin(targets = "foo.starred.kontext.core.misc.DonatorSize", remap = false)
public class KontextDonatorSizeMixin {
    @Inject(method = "fn(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private static void skipDonorSize(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CosmeticDisabler.disableStarredSizes()) cir.setReturnValue(false);
    }
}
