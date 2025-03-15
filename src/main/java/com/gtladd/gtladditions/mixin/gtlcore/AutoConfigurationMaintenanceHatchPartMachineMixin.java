package com.gtladd.gtladditions.mixin.gtlcore;

import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.AutoConfigurationMaintenanceHatchPartMachine;

import dev.architectury.patchedmixin.staticmixin.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AutoConfigurationMaintenanceHatchPartMachine.class)
public class AutoConfigurationMaintenanceHatchPartMachineMixin {

    @Shadow(remap = false)
    private float durationMultiplier;

    @Overwrite
    public void incInternalMultiplier(int multiplier) {
        this.durationMultiplier = Math.min(this.durationMultiplier + 0.01F * (float) multiplier, 5.0F);
    }

    @Overwrite
    private void decInternalMultiplier(int multiplier) {
        this.durationMultiplier = Math.max(this.durationMultiplier - 0.01F * (float) multiplier, 0.1F);
    }
}
