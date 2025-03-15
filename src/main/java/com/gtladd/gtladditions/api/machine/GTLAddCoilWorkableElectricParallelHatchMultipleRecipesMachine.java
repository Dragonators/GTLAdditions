package com.gtladd.gtladditions.api.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gtladd.gtladditions.api.recipeslogic.GTLAddMultipleRecipesLogic;
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers;
import org.jetbrains.annotations.NotNull;

public class GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine extends CoilWorkableElectricMultiblockMachine implements ParallelMachine {
    public GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine(IMachineBlockEntity holder) {
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
        return GTLRecipeModifiers.getHatchParallel(this);
    }
}
