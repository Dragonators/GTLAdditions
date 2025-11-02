package com.gtladd.gtladditions.integration.xei;

import com.lowdragmc.lowdraglib.LDLib;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;

import static com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.HELIOFLARE_POWER_FORGE;
import static org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA.ADVANCED_MULTI_SMELTER;
import static org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_STEAM_OVEN;

@EmiEntrypoint
public class GTEMIPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        if (LDLib.isReiLoaded() || LDLib.isJeiLoaded()) return;

        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(HELIOFLARE_POWER_FORGE.asStack()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(ADVANCED_MULTI_SMELTER.asStack()));
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(DIMENSIONALLY_TRANSCENDENT_STEAM_OVEN.asStack()));
    }
}
