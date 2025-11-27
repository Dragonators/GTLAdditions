package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdditionalMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineB
import org.gtlcore.gtlcore.config.ConfigHolder
import kotlin.math.min
import kotlin.math.roundToInt

object ThreadMultiplierStrategy {
    private val BLOCK_MULTIPLIER_MAP = mapOf<MultiblockMachineDefinition, Int>(
        MultiBlockMachineA.FISHING_GROUND to 512,
        MultiBlockMachineA.LARGE_GREENHOUSE to 128,
        MultiBlockMachineA.A_MASS_FABRICATOR to 16,
        AdditionalMultiBlockMachine.HUGE_INCUBATOR to 256,
        MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_MIXER to 1,
        AdvancedMultiBlockMachine.SUPRACHRONAL_ASSEMBLY_LINE to 1,
        MultiBlockMachineA.NANO_CORE to 1,
        AdvancedMultiBlockMachine.COMPRESSED_FUSION_REACTOR[GTValues.UEV] to 2,
        MultiBlockMachineA.LARGE_RECYCLER to 2048,
        MultiBlockMachineA.ADVANCED_SPS_CRAFTING to 1,
        MultiBlockMachineA.PETROCHEMICAL_PLANT to 32,
        MultiBlockMachineB.WOOD_DISTILLATION to 128,
        AdvancedMultiBlockMachine.PCB_FACTORY to 2048,
        AdditionalMultiBlockMachine.ADVANCED_RARE_EARTH_CENTRIFUGAL to 4,
        MultiBlockMachineB.GRAVITATION_SHOCKBURST to 512,
        AdditionalMultiBlockMachine.ADVANCED_NEUTRON_ACTIVATOR to 2,
        MultiBlockMachineA.COMPONENT_ASSEMBLY_LINE to 1,
        MultiBlockMachineA.ATOMIC_ENERGY_EXCITATION_PLANT to 1,
        MultiBlockMachineA.SUPER_PARTICLE_COLLIDER to 4,
        MultiBlockMachineA.MATTER_FABRICATOR to 3,
        AdvancedMultiBlockMachine.CREATE_AGGREGATION to 16,
        AdvancedMultiBlockMachine.DOOR_OF_CREATE to 16,
        MultiBlockMachine.DRACONIC_COLLAPSE_CORE to 2,
        MultiBlockMachine.TITAN_CRIP_EARTHBORE to 2,
        MultiBlockMachine.SKELETON_SHIFT_RIFT_ENGINE to 2
    )

    fun getAdditionalMultiplier(definition: MultiblockMachineDefinition?): Int {
        val result = min((1 / ConfigHolder.INSTANCE.durationMultiplier), 4096.0) * BLOCK_MULTIPLIER_MAP.getOrDefault(definition, 2)
        return result.roundToInt()
    }
}
