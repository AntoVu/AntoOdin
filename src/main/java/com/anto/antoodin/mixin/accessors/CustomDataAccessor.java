package com.anto.antoodin.mixin.accessors;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// The tag itself, not copyTag()'s copy, for reads every frame. Never modify it.
@Mixin(CustomData.class)
public interface CustomDataAccessor {
    @Accessor("tag")
    CompoundTag getTag();
}
