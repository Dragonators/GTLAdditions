package com.gtladd.gtladditions.integration.xei

import com.gregtechceu.gtceu.integration.emi.recipe.GTRecipeTypeEmiCategory
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.APOCALYPTIC_TORSION_QUANTUM_MATRIX
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.ATOMIC_TRANSMUTATIOON_CORE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.HEART_OF_THE_UNIVERSE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.HELIOFLARE_POWER_FORGE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.SUBATOMIC_TRANSMUTATIOON_CORE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.EM_RESONANCE_CONVERSION_FIELD
import com.gtladd.gtladditions.integration.stargate.StargateContent
import com.gtladd.gtladditions.integration.xei.machineinfo.MachineInfoEmiCategory
import com.gtladd.gtladditions.integration.xei.structureupgrade.StructureUpgradeEmiCategory
import com.lowdragmc.lowdraglib.LDLib
import dev.emi.emi.api.EmiEntrypoint
import dev.emi.emi.api.EmiPlugin
import dev.emi.emi.api.EmiRegistry
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories
import dev.emi.emi.api.stack.EmiStack
import net.minecraft.world.item.Item
import net.minecraft.world.level.material.Fluid
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA

@Suppress("unused")
@EmiEntrypoint
class GTEMIPlugin : EmiPlugin {
    override fun register(registry: EmiRegistry) {
        registry.addGenericDragDropHandler(TerminalSearchBoxEmiHandler)
        registry.removeEmiStacks { stack ->
            when (val key = stack.key) {
                is Item -> StargateContent.isDisabledItem(key) ||
                    key == ATOMIC_TRANSMUTATIOON_CORE.item ||
                    key == APOCALYPTIC_TORSION_QUANTUM_MATRIX.item ||
                    key == DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT.item ||
                    key == SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY.item ||
                    key == HEART_OF_THE_UNIVERSE.item
                is Fluid -> StargateContent.isDisabledFluid(key)
                else -> false
            }
        }

        // Conversion runs on a dummy recipe type; expose its actual block conversion recipes here.
        registry.addWorkstation(
            GTRecipeTypeEmiCategory.CATEGORIES.apply(EM_RESONANCE_CONVERSION_FIELD),
            EmiStack.of(SUBATOMIC_TRANSMUTATIOON_CORE.asStack())
        )

        MachineInfoEmiCategory.register(registry)
        StructureUpgradeEmiCategory.register(registry)

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
