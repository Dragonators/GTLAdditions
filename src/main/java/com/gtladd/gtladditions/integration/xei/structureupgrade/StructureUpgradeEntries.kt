package com.gtladd.gtladditions.integration.xei.structureupgrade

import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.machine.feature.StructureTierSpec
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex
import com.gtladd.gtladditions.common.machine.multiblock.controller.HeartOfTheCosmos
import net.minecraft.resources.ResourceLocation

object StructureUpgradeEntries {
    val ID: ResourceLocation = GTLAdditions.id("structure_upgrade")
    val ICON: ResourceLocation = GTLAdditions.id("textures/gui/structure_tier/icon.png")

    fun entries(): List<StructureUpgradeEntry> = buildList {
        addTiers(MultiBlockMachine.EXOTIC_CHEMICAL_COMPLEX, ExoticChemicalComplex.TIER_SPEC, StructureUpgradeTheme.DEFAULT)
        addTiers(MultiBlockMachine.HEART_OF_THE_COSMOS, HeartOfTheCosmos.TIER_SPEC, StructureUpgradeTheme.COSMIC)
    }

    private fun MutableList<StructureUpgradeEntry>.addTiers(
        machine: MachineDefinition,
        spec: StructureTierSpec,
        theme: StructureUpgradeTheme
    ) {
        spec.requirements.forEachIndexed { i, requirement ->
            add(StructureUpgradeEntry(machine, spec.minTier + 1 + i, spec.maxTier, requirement, theme))
        }
    }
}
