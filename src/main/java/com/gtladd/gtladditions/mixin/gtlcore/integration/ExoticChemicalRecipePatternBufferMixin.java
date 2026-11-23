package com.gtladd.gtladditions.mixin.gtlcore.integration;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MERecipePatternBufferPartMachine;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Set;

@Mixin(value = MERecipePatternBufferPartMachine.class, remap = false)
public abstract class ExoticChemicalRecipePatternBufferMixin {

    @WrapOperation(method = "refreshPatterns",
                   at = @At(value = "INVOKE",
                            target = "Lorg/gtlcore/gtlcore/integration/ae2/handler/MERecipePatternHelper;getActiveRecipeTypes(Ljava/util/List;)Ljava/util/Set;"))
    private Set<GTRecipeType> gtladditions$unitRecipeType(List<IMultiController> controllers, Operation<Set<GTRecipeType>> original) {
        for (IMultiController controller : controllers) {
            if (controller instanceof ExoticChemicalComplex complex && complex.isFormed()) {
                GTRecipeType type = complex.getUnitRecipeType((IMultiPart) this);
                return type == null ? Set.of() : Set.of(type);
            }
        }

        return original.call(controllers);
    }
}