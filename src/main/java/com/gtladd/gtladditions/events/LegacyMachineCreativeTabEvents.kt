package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.APOCALYPTIC_TORSION_QUANTUM_MATRIX
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.ATOMIC_TRANSMUTATIOON_CORE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.HEART_OF_THE_UNIVERSE
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object LegacyMachineCreativeTabEvents {
    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun hideLegacyMachines(event: BuildCreativeModeTabContentsEvent) {
        val hidden = event.entries.filter {
            it.key.`is`(ATOMIC_TRANSMUTATIOON_CORE.item) ||
                it.key.`is`(APOCALYPTIC_TORSION_QUANTUM_MATRIX.item) ||
                it.key.`is`(DIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT.item) ||
                it.key.`is`(SUBSPACE_CORRIDOR_HUB_INDUSTRIAL_ARRAY.item) ||
                it.key.`is`(HEART_OF_THE_UNIVERSE.item)
        }.map { it.key }
        hidden.forEach { event.entries.remove(it) }
    }
}
