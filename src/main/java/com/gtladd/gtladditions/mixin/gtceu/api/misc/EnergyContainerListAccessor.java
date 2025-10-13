package com.gtladd.gtladditions.mixin.gtceu.api.misc;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(EnergyContainerList.class)
public interface EnergyContainerListAccessor {

    @Accessor(value = "energyContainerList", remap = false)
    List<? extends IEnergyContainer> getEnergyContainerList();
}
