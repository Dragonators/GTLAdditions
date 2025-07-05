package com.gtladd.gtladditions.api.machine;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;

import com.gtladd.gtladditions.api.recipeslogic.GTLAddMultipleRecipesLogic;
import org.jetbrains.annotations.NotNull;

public class GTLAddWorkableElectricMultipleRecipesMachine extends WorkableElectricMultiblockMachine implements ParallelMachine {

    public GTLAddWorkableElectricMultipleRecipesMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    public @NotNull RecipeLogic createRecipeLogic(@NotNull Object... args) {
        return new GTLAddMultipleRecipesLogic(this);
    }

    public @NotNull GTLAddMultipleRecipesLogic getRecipeLogic() {
        return (GTLAddMultipleRecipesLogic) super.getRecipeLogic();
    }

    @Override
    public int getMaxParallel() {
        return Integer.MAX_VALUE;
    }
}
