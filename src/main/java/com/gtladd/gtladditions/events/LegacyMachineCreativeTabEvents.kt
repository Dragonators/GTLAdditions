package com.gtladd.gtladditions.events

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.APOCALYPTIC_TORSION_QUANTUM_MATRIX
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.ATOMIC_TRANSMUTATIOON_CORE
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
                it.key.`is`(APOCALYPTIC_TORSION_QUANTUM_MATRIX.item)
        }.map { it.key }
        hidden.forEach { event.entries.remove(it) }
    }
}