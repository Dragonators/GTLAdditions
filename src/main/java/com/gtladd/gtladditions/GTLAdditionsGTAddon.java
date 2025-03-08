package com.gtladd.gtladditions;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import com.gtladd.gtladditions.data.Recipes.Process.process;
import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.Common.Items.GTLAddItems;
import com.gtladd.gtladditions.Common.Machine.GTLAddMachine;
import com.gtladd.gtladditions.api.Registry.GTLAddRegistration;
import com.gtladd.gtladditions.data.Recipes.*;
import com.gtladd.gtladditions.data.Recipes.NewMachineRecipe.*;

import java.util.function.Consumer;

@GTAddon
public class GTLAdditionsGTAddon implements IGTAddon {

    public GTLAdditionsGTAddon() {}

    @Override
    public GTRegistrate getRegistrate() {
        return GTLAddRegistration.REGISTRATE;
    }

    @Override
    public void initializeAddon() {
        GTLAddItems.init();
        GTLAddMachine.init();
    }

    @Override
    public String addonModId() {
        return GTLAdditions.MOD_ID;
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        PhotonMatrixEtch.init(provider);
        EMResonanceConversionField.init(provider);
        TitansCripEarthbore.init(provider);
        BiologicalSimulation.init(provider);
        VoidfluxReaction.init(provider);
        StellarLgnition.init(provider);
        AE2.init(provider);
        Assembler.init(provider);
        Distort.init(provider);
        IntegratedtedOreProcessor.init(provider);
        Mixer.init(provider);
        NewMultiBlockMachineController.init(provider);
        Qft.init(provider);
        process.init(provider);
    }
}
