package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.noamm.TimeChanger;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.timeline.AttributeTrackSampler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// Time Changer: swaps the overworld clock's ticks while the sky or fog is sampled
@Mixin(AttributeTrackSampler.class)
public class AttributeTrackSamplerMixin {
    @Shadow @Final private Holder<WorldClock> clock;
    @Shadow @Final private ClockManager clockManager;

    @ModifyArg(method = "applyTimeBased", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/KeyframeTrackSampler;sample(J)Ljava/lang/Object;"), index = 0)
    private long changeTime(long ticks) {
        return TimeChanger.extractingSky && clockManager instanceof ClientClockManager && clock.is(WorldClocks.OVERWORLD) ? TimeChanger.getTime() : ticks;
    }
}
