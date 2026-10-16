package com.anto.antoodin.mixin.accessors;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// The pitch the sound was played with; getPitch() also multiplies in a random sample from sounds.json
@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceAccessor {
    @Accessor("pitch")
    float getRequestedPitch();
}
