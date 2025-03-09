package com.gtladd.gtladditions.api.machine;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;

import com.gtladd.gtladditions.api.recipeslogic.GTLAddMultipleRecipesLogic;
import org.jetbrains.annotations.NotNull;

public class GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine extends CoilWorkableElectricMultiblockMachine implements ParallelMachine {

    public GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    public @NotNull RecipeLogic createRecipeLogic(@NotNull Object... args) {
        return new GTLAddMultipleRecipesLogic(this);
    }

    public @NotNull GTLAddMultipleRecipesLogic getRecipeLogic() {
        return (GTLAddMultipleRecipesLogic) super.getRecipeLogic();
    }

    @Override
    public int getMaxParallel() {
        return Math.min(Integer.MAX_VALUE, (int) Math.pow(2.0, (double) this.getCoilType().getCoilTemperature() / 900.0));
    }
}
