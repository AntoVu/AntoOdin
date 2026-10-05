package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.MonoAudio;
import com.anto.antoodin.interfaces.MonoAudioChannel;
import net.minecraft.client.Camera;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Shadow @Final private ChannelAccess channelAccess;

    // The listener moved, so distances to every playing sound changed
    @Inject(method = "updateSource", at = @At("TAIL"))
    private void refreshMonoChannels(Camera camera, CallbackInfo ci) {
        if (!MonoAudio.INSTANCE.getEnabled()) return;
        channelAccess.executeOnChannels(stream ->
            stream.forEach(channel -> ((MonoAudioChannel) channel).antoodin$refreshPosition())
        );
    }
}
