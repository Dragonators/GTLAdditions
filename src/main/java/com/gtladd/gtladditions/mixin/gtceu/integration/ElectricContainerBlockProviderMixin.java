package com.gtladd.gtladditions.mixin.gtceu.integration;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.integration.jade.provider.ElectricContainerBlockProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import com.gtladd.gtladditions.common.machine.trait.NetworkEnergyContainer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ElectricContainerBlockProvider.class)
public abstract class ElectricContainerBlockProviderMixin {

    /**
     * @author Dragons
     * @reason except wireless terminal
     */
    @Nullable
    @Overwrite(remap = false)
    protected IEnergyContainer getCapability(Level level, BlockPos pos, @Nullable Direction side) {
        var container = GTCapabilityHelper.getEnergyContainer(level, pos, side);
        return container instanceof NetworkEnergyContainer ? null : container;
    }
}
