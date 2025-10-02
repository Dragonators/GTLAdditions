package com.gtladd.gtladditions;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.registry.GTLAddRegistration;
import com.gtladd.gtladditions.common.blocks.GTLAddBlocks;
import com.gtladd.gtladditions.common.data.GTLAddSoundEntries;
import com.gtladd.gtladditions.common.items.GTLAddItems;
import com.gtladd.gtladditions.common.material.GTLAddElements;
import com.gtladd.gtladditions.data.recipes.*;
import com.gtladd.gtladditions.data.recipes.newmachinerecipe.*;
import com.gtladd.gtladditions.data.recipes.process.SocProcess;

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
        GTLAddBlocks.init();
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
        ChaosWeave.init(provider);
        AE2.init(provider);
        Assembler.init(provider);
        AssemblyLine.init(provider);
        Distort.init(provider);
        ElectricBlastFurnace.init(provider);
        IntegratedtedOreProcessor.init(provider);
        NewMultiBlockMachineController.init(provider);
        Qft.init(provider);
        SocProcess.init(provider);
        Misc.init(provider);
    }

    @Override
    public void registerSounds() {
        GTLAddSoundEntries.init();
    }

    @Override
    public void registerElements() {
        GTLAddElements.init();
    }
}
