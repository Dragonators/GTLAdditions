package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.common.machine.multiblock.electric.TierCasingMachine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TierCasingMachine.class)
public interface TierCasingMachineAccessor {

    @Accessor(value = "tierType", remap = false)
    String getTierType();

    @Accessor(value = "tier", remap = false)
    int getTier();
}
