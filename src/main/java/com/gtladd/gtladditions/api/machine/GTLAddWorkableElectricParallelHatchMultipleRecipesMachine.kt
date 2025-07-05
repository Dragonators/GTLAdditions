package com.gtladd.gtladditions.api.machine;

import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

public class GTLAddWorkableElectricParallelHatchMultipleRecipesMachine extends GTLAddWorkableElectricMultipleRecipesMachine {

    public GTLAddWorkableElectricParallelHatchMultipleRecipesMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    public int getMaxParallel() {
        return GTLRecipeModifiers.getHatchParallel(this);
    }
}
