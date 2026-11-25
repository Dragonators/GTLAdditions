package com.gtladd.gtladditions.mixin.gtceu.api.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorkableMultiblockMachine.class)
public interface WorkableMultiblockMachineAccessor {

    @Accessor(value = "activeBlocks", remap = false)
    void setActiveBlocks(LongSet activeBlocks);
}
