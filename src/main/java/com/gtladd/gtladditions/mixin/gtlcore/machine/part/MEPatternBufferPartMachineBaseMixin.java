package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachineBase;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MEPatternBufferPartMachineBase.class)
public abstract class MEPatternBufferPartMachineBaseMixin {

    @WrapOperation(method = "onUse",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/nbt/CompoundTag;putIntArray(Ljava/lang/String;[I)V",
                            remap = true),
                   remap = false,
                   require = 1)
    private void gtladditions$clearSuperBinding(CompoundTag tag, String key, int[] value, Operation<Void> original) {
        if ("pos".equals(key) && tag.contains("superPos", Tag.TAG_INT_ARRAY)) tag.remove("superPos");
        original.call(tag, key, value);
    }
}