package com.gtladd.gtladditions.mixin.gtceu.integration;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.integration.ae2.machine.MEStockingBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEStockingHatchPartMachine;

import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = { MEStockingBusPartMachine.class, MEStockingHatchPartMachine.class }, remap = false)
public abstract class ExoticChemicalStockingMixin {

    @WrapOperation(method = "testConfiguredInOtherPart",
                   at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/machine/feature/multiblock/IMultiController;getParts()Ljava/util/List;"))
    private List<IMultiPart> gtladditions$stockingPeers(IMultiController controller, Operation<List<IMultiPart>> original) {
        if (controller instanceof ExoticChemicalComplex complex) return complex.stockingPeers((IMultiPart) this);
        return original.call(controller);
    }
}