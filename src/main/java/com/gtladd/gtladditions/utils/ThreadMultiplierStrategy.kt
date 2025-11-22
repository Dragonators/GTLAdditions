package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.gtlcore.gtlcore.common.data.machines.AdditionalMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineB
import org.gtlcore.gtlcore.config.ConfigHolder
import kotlin.math.min
import kotlin.math.roundToInt

object ThreadMultiplierStrategy {
    private val BLOCK_MULTIPLIER_MAP = Object2IntOpenHashMap<MultiblockMachineDefinition>()

    init {
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.FISHING_GROUND, 512)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.LARGE_GREENHOUSE, 128)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.A_MASS_FABRICATOR, 16)
        BLOCK_MULTIPLIER_MAP.put(AdditionalMultiBlockMachine.HUGE_INCUBATOR, 256)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.DIMENSIONALLY_TRANSCENDENT_MIXER, 1)
        BLOCK_MULTIPLIER_MAP.put(AdvancedMultiBlockMachine.SUPRACHRONAL_ASSEMBLY_LINE, 1)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.NANO_CORE, 1)
        BLOCK_MULTIPLIER_MAP.put(AdvancedMultiBlockMachine.COMPRESSED_FUSION_REACTOR[GTValues.UEV], 2)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.LARGE_RECYCLER, 2048)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.ADVANCED_SPS_CRAFTING, 1)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.PETROCHEMICAL_PLANT, 32)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineB.WOOD_DISTILLATION, 128)
        BLOCK_MULTIPLIER_MAP.put(AdvancedMultiBlockMachine.PCB_FACTORY, 2048)
        BLOCK_MULTIPLIER_MAP.put(AdditionalMultiBlockMachine.ADVANCED_RARE_EARTH_CENTRIFUGAL, 4)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineB.GRAVITATION_SHOCKBURST, 512)
        BLOCK_MULTIPLIER_MAP.put(AdditionalMultiBlockMachine.ADVANCED_NEUTRON_ACTIVATOR, 2)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.COMPONENT_ASSEMBLY_LINE, 1)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.ATOMIC_ENERGY_EXCITATION_PLANT, 1)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.SUPER_PARTICLE_COLLIDER, 4)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachineA.MATTER_FABRICATOR, 3)
        BLOCK_MULTIPLIER_MAP.put(AdvancedMultiBlockMachine.CREATE_AGGREGATION, 1)
        BLOCK_MULTIPLIER_MAP.put(AdvancedMultiBlockMachine.DOOR_OF_CREATE, 1)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachine.DRACONIC_COLLAPSE_CORE, 2)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachine.TITAN_CRIP_EARTHBORE, 2)
        BLOCK_MULTIPLIER_MAP.put(MultiBlockMachine.SKELETON_SHIFT_RIFT_ENGINE, 2)
    }

    fun getAdditionalMultiplier(definition: MultiblockMachineDefinition?): Int {
        val result = min((1 / ConfigHolder.INSTANCE.durationMultiplier), 4096.0) * BLOCK_MULTIPLIER_MAP.getOrDefault(definition, 2)
        return result.roundToInt()
    }
}
