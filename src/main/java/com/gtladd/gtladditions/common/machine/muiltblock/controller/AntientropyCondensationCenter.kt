package com.gtladd.gtladditions.common.machine.muiltblock.controller;

import org.gtlcore.gtlcore.utils.MachineIO;
import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.network.chat.Component;

import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricParallelHatchMultipleRecipesMachine;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AntientropyCondensationCenter extends GTLAddWorkableElectricParallelHatchMultipleRecipesMachine {

    private int ITEM_INPUT;

    public AntientropyCondensationCenter(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    public static boolean beforeWorking(IRecipeLogicMachine machine, @NotNull GTRecipe recipe) {
        if (machine instanceof AntientropyCondensationCenter machines) {
            machines.setItemInput();
            return MachineIO.inputItem(machines, Registries.getItemStack("kubejs:dust_cryotheum", machines.ITEM_INPUT));
        }
        return false;
    }

    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed()) {
            this.setItemInput();
            textList.add(Component.literal("需要凛冰粉：" + ITEM_INPUT + "个"));
        }
    }

    private void setItemInput() {
        ITEM_INPUT = 1 << (GTValues.MAX - this.getTier());
    }
}
