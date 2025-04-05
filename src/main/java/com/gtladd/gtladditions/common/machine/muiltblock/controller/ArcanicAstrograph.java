package com.gtladd.gtladditions.common.machine.muiltblock.controller;

import org.gtlcore.gtlcore.common.machine.multiblock.electric.HarmonyMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.logic.OCParams;
import com.gregtechceu.gtceu.api.recipe.logic.OCResult;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArcanicAstrograph extends HarmonyMachine {

    public ArcanicAstrograph(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    public static @Nullable GTRecipe recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe, @NotNull OCParams params, @NotNull OCResult result) {
        GTRecipe recipe1 = HarmonyMachine.recipeModifier(machine, recipe, params, result);
        if (recipe1 != null) {
            return GTRecipeModifiers.accurateParallel(machine, recipe1, 2048, false).getFirst();
        }
        return null;
    }
}
