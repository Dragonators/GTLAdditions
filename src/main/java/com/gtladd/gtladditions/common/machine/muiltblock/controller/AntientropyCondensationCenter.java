package com.gtladd.gtladditions.common.machine.muiltblock.controller;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricMultipleRecipesMachine;
import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricParallelHatchMultipleRecipesMachine;
import net.minecraft.network.chat.Component;
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers;
import org.gtlcore.gtlcore.utils.MachineIO;
import org.gtlcore.gtlcore.utils.Registries;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AntientropyCondensationCenter extends GTLAddWorkableElectricParallelHatchMultipleRecipesMachine {
    private static int ITEM_INPUT;
    public AntientropyCondensationCenter(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }
    public static boolean beforeWorking(IRecipeLogicMachine machine, @NotNull GTRecipe recipe) {
        if (machine instanceof AntientropyCondensationCenter machines){
            machines.setFluidInput();
            if (MachineIO.inputItem(machines, Registries.getItemStack("kubejs:dust_cryotheum", ITEM_INPUT))) return true;
        }
        machine.getRecipeLogic().interruptRecipe();
        return false;
    }
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed()){
            this.setFluidInput();
            textList.add(Component.literal("需要凛冰粉：" + ITEM_INPUT + "个"));
        }
    }
    private void setFluidInput(){
        ITEM_INPUT = 1 << (GTValues.MAX - this.getTier());
    }
}
