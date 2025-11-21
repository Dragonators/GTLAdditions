package com.gtladd.gtladditions.api.machine.logic

import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe

interface IWirelessRecipeLogic {
    fun handleWirelessTickInput(recipe: IWirelessGTRecipe): Boolean {
        val euTickInputs = recipe.getWirelessEuTickInputs() ?: return true

        val trait = getWirelessMachine().getWirelessNetworkEnergyHandler()
        return trait != null && trait.isOnline && trait.consumeEnergy(euTickInputs)
    }

    fun getWirelessMachine(): IWirelessElectricMultiblockMachine = object : IWirelessElectricMultiblockMachine {}
}