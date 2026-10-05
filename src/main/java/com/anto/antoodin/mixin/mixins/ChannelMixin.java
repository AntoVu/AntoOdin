package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.MonoAudio;
import com.anto.antoodin.interfaces.MonoAudioChannel;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public class ChannelMixin implements MonoAudioChannel {
    @Shadow @Final private int source;
    @Unique private Vec3 antoodin$lastPosition = Vec3.ZERO;
    @Unique private boolean antoodin$relative;

    @Inject(method = "setSelfPosition", at = @At("HEAD"), cancellable = true)
    private void forceMonoPosition(Vec3 pos, CallbackInfo ci) {
        if (!MonoAudio.INSTANCE.getEnabled()) return;
        antoodin$lastPosition = pos;
        antoodin$refreshPosition();
        ci.cancel();
    }

    @Inject(method = "setRelative", at = @At("HEAD"), cancellable = true)
    private void forceRelative(boolean relative, CallbackInfo ci) {
        if (!MonoAudio.INSTANCE.getEnabled()) return;
        antoodin$relative = relative;
        AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        antoodin$refreshPosition();
        ci.cancel();
    }

    @Override
    public void antoodin$refreshPosition() {
        if (!MonoAudio.INSTANCE.getEnabled()) return;
        MonoAudio.applyCenteredPosition(
            source,
            antoodin$relative ? antoodin$lastPosition.length() : MonoAudio.distanceToListener(antoodin$lastPosition)
        );
    }
}
