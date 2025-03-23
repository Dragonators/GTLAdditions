package com.gtladd.gtladditions;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.registry.GTLAddRegistration;
import com.gtladd.gtladditions.common.items.GTLAddItems;
import com.gtladd.gtladditions.common.machine.GTLAddMachine;
import com.gtladd.gtladditions.data.recipes.*;
import com.gtladd.gtladditions.data.recipes.newmachinerecipe.*;
import com.gtladd.gtladditions.data.recipes.process.socprocess;

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
        AntientropyCondensation.init(provider);
        ChaoticAlchemy.init(provider);
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
        socprocess.init(provider);
        Misc.init(provider);
    }
}
