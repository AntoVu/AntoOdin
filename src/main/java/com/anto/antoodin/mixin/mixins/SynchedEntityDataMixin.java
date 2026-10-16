package com.anto.antoodin.mixin.mixins;

import com.anto.antoodin.features.impl.fixes.DisconnectFix;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SynchedEntityData.class)
public class SynchedEntityDataMixin {
    // Per field, so one bad field doesn't drop the rest of the packet
    @WrapOperation(method = "assignValues", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData;assignValue(Lnet/minecraft/network/syncher/SynchedEntityData$DataItem;Lnet/minecraft/network/syncher/SynchedEntityData$DataValue;)V"))
    private void skipBadValue(SynchedEntityData instance, SynchedEntityData.DataItem<?> item, SynchedEntityData.DataValue<?> value, Operation<Void> original) {
        if (!DisconnectFix.INSTANCE.getEnabled()) {
            original.call(instance, item, value);
            return;
        }
        try {
            original.call(instance, item, value);
        } catch (IllegalStateException | ClassCastException exception) {
            DisconnectFix.onBadData(exception);
        }
    }
}
