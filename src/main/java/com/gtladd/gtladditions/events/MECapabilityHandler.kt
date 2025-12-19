package com.gtladd.gtladditions.events

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gtladd.gtladditions.GTLAdditions.Companion.MOD_ID
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import com.gtladd.gtladditions.common.machine.GTLAddMachines.INFINITY_INPUT_DUAL_HATCH
import com.gtladd.gtladditions.common.machine.hatch.InfinityDualHatchPartMachine
import com.gtladd.gtladditions.integration.ae2.MEStorageCapabilityProvider
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Suppress("unused")
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object MECapabilityHandler {
    @SubscribeEvent
    @JvmStatic
    fun attachMEStorageCapability(event: AttachCapabilitiesEvent<BlockEntity>) {
        val blockEntity = event.getObject()

        if (blockEntity !is MetaMachineBlockEntity) return

        if(blockEntity.definition === INFINITY_INPUT_DUAL_HATCH) {
            event.addCapability(
                id("infinity_input_dual_hatch"),
                MEStorageCapabilityProvider { blockEntity.getMetaMachine() as InfinityDualHatchPartMachine }
            )
        }
    }
}