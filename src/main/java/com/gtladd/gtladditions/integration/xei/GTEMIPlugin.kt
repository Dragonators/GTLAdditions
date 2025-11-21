package com.gtladd.gtladditions.integration.xei

import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.HELIOFLARE_POWER_FORGE
import com.lowdragmc.lowdraglib.LDLib
import dev.emi.emi.api.EmiEntrypoint
import dev.emi.emi.api.EmiPlugin
import dev.emi.emi.api.EmiRegistry
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories
import dev.emi.emi.api.stack.EmiStack
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA

@Suppress("unused")
@EmiEntrypoint
class GTEMIPlugin : EmiPlugin {
    override fun register(registry: EmiRegistry) {
        if (LDLib.isReiLoaded() || LDLib.isJeiLoaded()) return

        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(HELIOFLARE_POWER_FORGE.asStack()))
        registry.addWorkstation(
            VanillaEmiRecipeCategories.SMELTING,
            EmiStack.of(MultiBlockMachineA.ADVANCED_MULTI_SMELTER.asStack())
        )
        registry.addWorkstation(
            VanillaEmiRecipeCategories.SMELTING,
            EmiStack.of(MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_STEAM_OVEN.asStack())
        )
    }
}
