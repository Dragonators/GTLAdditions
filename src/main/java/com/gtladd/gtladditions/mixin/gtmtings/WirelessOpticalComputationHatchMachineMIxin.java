package com.gtladd.gtladditions.mixin.gtmtings;

import com.hepdd.gtmthings.common.block.machine.multiblock.part.computation.WirelessOpticalComputationHatchMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WirelessOpticalComputationHatchMachine.class)
public class WirelessOpticalComputationHatchMachineMIxin {

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public boolean canShared() {
        return true;
    }
}
