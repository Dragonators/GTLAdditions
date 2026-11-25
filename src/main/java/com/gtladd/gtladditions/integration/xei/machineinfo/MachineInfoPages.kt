package com.gtladd.gtladditions.integration.xei.machineinfo

import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.SubspaceCorridorHubIndustrialArray
import com.gtladd.gtladditions.common.machine.multiblock.controller.TimeSpaceDistorter
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.SpacetimeStasisDevice
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.SupratemporalBoostingEngine

object MachineInfoPages {
    fun pages(): List<MachineInfoPage> = listOf(
        MachineInfoPage(
            MultiBlockMachine.SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY,
            SubspaceCorridorHubIndustrialArray.UPKEEP_EUT
        ),
        MachineInfoPage(
            MultiBlockMachine.SUBSPACE_CORRIDOR_NEXUS_INDUSTRIAL_ARRAY,
            SubspaceCorridorHubIndustrialArray.UPKEEP_EUT
        ),
        MachineInfoPage(
            MultiBlockMachine.SPACETIME_STASIS_DEVICE,
            SpacetimeStasisDevice.UPKEEP_EUT,
            listOf(SpacetimeStasisDevice.SPACETIME_PER_SECOND)
        ),
        MachineInfoPage(MultiBlockMachine.SUPRATEMPORAL_BOOSTING_ENGINE, SupratemporalBoostingEngine.UPKEEP_EUT),
        MachineInfoPage(MultiBlockMachine.TIME_SPACE_DISTORTER, TimeSpaceDistorter.UPKEEP_EUT)
    )
}
