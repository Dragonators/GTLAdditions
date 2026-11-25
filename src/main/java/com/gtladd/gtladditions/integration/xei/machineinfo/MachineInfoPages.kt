package com.gtladd.gtladditions.integration.xei.machineinfo

import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.LightHunterSpaceStation
import com.gtladd.gtladditions.common.machine.multiblock.controller.TimeSpaceDistorter
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.SpacetimeStasisDevice
import com.gtladd.gtladditions.common.machine.multiblock.controller.rrf.SupratemporalBoostingEngine

object MachineInfoPages {
    fun pages(): List<MachineInfoPage> = listOf(
        MachineInfoPage(MultiBlockMachine.LIGHT_HUNTER_SPACE_STATION, LightHunterSpaceStation.UPKEEP_EUT),
        MachineInfoPage(
            MultiBlockMachine.SPACETIME_STASIS_DEVICE,
            SpacetimeStasisDevice.UPKEEP_EUT,
            listOf(SpacetimeStasisDevice.SPACETIME_PER_SECOND)
        ),
        MachineInfoPage(MultiBlockMachine.SUPRATEMPORAL_BOOSTING_ENGINE, SupratemporalBoostingEngine.UPKEEP_EUT),
        MachineInfoPage(MultiBlockMachine.TIME_SPACE_DISTORTER, TimeSpaceDistorter.UPKEEP_EUT)
    )
}
