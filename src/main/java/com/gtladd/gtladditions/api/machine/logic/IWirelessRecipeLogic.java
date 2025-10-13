package com.gtladd.gtladditions.api.machine.logic;

import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine;
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public interface IWirelessRecipeLogic {

    default boolean handleWirelessTickInput(IWirelessGTRecipe recipe) {
        final BigInteger euTickInputs = recipe.getWirelessEuTickInputs();
        if (euTickInputs == null) return true;

        final var trait = getWirelessMachine().getWirelessNetworkEnergyHandler();
        return trait != null && trait.isOnline() && trait.consumeEnergy(euTickInputs);
    }

    @NotNull
    default IWirelessElectricMultiblockMachine getWirelessMachine() {
        return new IWirelessElectricMultiblockMachine() {};
    }
}
