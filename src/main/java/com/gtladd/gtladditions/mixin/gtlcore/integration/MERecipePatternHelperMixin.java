package com.gtladd.gtladditions.mixin.gtlcore.integration;

import org.gtlcore.gtlcore.integration.ae2.handler.MERecipePatternHelper;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeRecipesLogic;
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic;
import com.gtladd.gtladditions.common.machine.multiblock.controller.ApocalypticTorsionQuantumMatrix;
import com.gtladd.gtladditions.common.machine.multiblock.controller.ArcanicAstrograph;
import com.gtladd.gtladditions.common.machine.multiblock.controller.ForgeOfTheAntichrist;
import com.gtladd.gtladditions.common.machine.multiblock.controller.TimeSpaceDistorter;
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.RRFModuleMachine;
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.RRFWorkableModuleMachine;
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.RecursiveReverseArray;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(MERecipePatternHelper.class)
public abstract class MERecipePatternHelperMixin {

    @Inject(method = "getActiveRecipeTypes", at = @At("HEAD"), cancellable = true, remap = false)
    private static void gtladditions$excludeControllers(List<IMultiController> controllers,
                                                        CallbackInfoReturnable<Set<GTRecipeType>> cir) {
        for (IMultiController controller : controllers) {
            var machine = controller.self();
            if (machine instanceof TimeSpaceDistorter || machine instanceof RecursiveReverseArray ||
                    machine instanceof RRFModuleMachine || machine instanceof RRFWorkableModuleMachine ||
                    machine instanceof ApocalypticTorsionQuantumMatrix || machine instanceof ForgeOfTheAntichrist) {
                cir.setReturnValue(Set.of());
                return;
            }
        }
    }

    @WrapOperation(method = "getActiveRecipeTypes",
                   at = @At(value = "INVOKE",
                            target = "Lorg/gtlcore/gtlcore/integration/ae2/handler/MERecipePatternHelper;addRecipeType(Ljava/util/Set;Lcom/gregtechceu/gtceu/api/recipe/GTRecipeType;Ljava/util/Set;)V"),
                   remap = false,
                   require = 1)
    private static void gtladditions$expandRecipeTypes(Set<GTRecipeType> types, GTRecipeType type, Set<GTRecipeType> visited,
                                                       Operation<Void> original, @Local IRecipeLogicMachine machine) {
        var logic = machine.getRecipeLogic();
        if (logic instanceof GTLAddMultipleTypeRecipesLogic || logic instanceof GTLAddMultipleTypeWirelessRecipesLogic ||
                machine instanceof ArcanicAstrograph) {
            for (GTRecipeType recipeType : machine.getRecipeTypes()) {
                original.call(types, recipeType, visited);
            }
        } else {
            original.call(types, type, visited);
        }
    }
}