package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.WirelessOpticalDataHatchMachine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WirelessOpticalDataHatchMachine.class)
public class WirelessOpticalDataHatchMachineMixin {

    /**
     * @author .
     * @reason .
     */
    @Overwrite(remap = false)
    public boolean canShared() {
        return true;
    }
}
