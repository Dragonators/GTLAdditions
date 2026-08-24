package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.common.machine.multiblock.electric.ComputationProviderMachine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ComputationProviderMachine.class)
public interface ComputationProviderMachineAccessor {

    @Accessor(value = "canProvideCWUt", remap = false)
    boolean canProvideCWUt();
}