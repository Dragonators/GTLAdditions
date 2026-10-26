package com.gtladd.gtladditions.mixin.gtceu.integration;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.integration.jade.provider.ElectricContainerBlockProvider;

import com.gtladd.gtladditions.common.machine.trait.NetworkEnergyContainer;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ElectricContainerBlockProvider.class)
public abstract class ElectricContainerBlockProviderMixin {

    @ModifyReturnValue(method = "getCapability(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Lcom/gregtechceu/gtceu/api/capability/IEnergyContainer;",
                       at = @At("RETURN"),
                       remap = false,
                       require = 1)
    private IEnergyContainer gtladditions$hideWirelessContainer(IEnergyContainer container) {
        return container instanceof NetworkEnergyContainer ? null : container;
    }
}