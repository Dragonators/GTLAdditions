package com.gtladd.gtladditions.api.registry

import com.gregtechceu.gtceu.api.block.IMachineBlock
import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.item.MetaMachineItem
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate
import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import java.util.function.Function

class GTLAddRegistration : GTRegistrate(GTLAdditions.MOD_ID) {
    override fun multiblock(
        name: String,
        metaMachine: Function<IMachineBlockEntity, out MultiblockControllerMachine?>
    ): GTLAddMultiBlockMachineBuilder {
        return GTLAddMultiBlockMachineBuilder.createMulti(
            name,
            metaMachine,
            { properties: BlockBehaviour.Properties?, definition: MultiblockMachineDefinition? ->
                MetaMachineBlock(
                    properties!!,
                    definition!!
                )
            },
            { block: IMachineBlock?, properties: Item.Properties? -> MetaMachineItem(block!!, properties!!) },
            { type: BlockEntityType<*>?, pos: BlockPos?, blockState: BlockState? ->
                MetaMachineBlockEntity.createBlockEntity(
                    type,
                    pos,
                    blockState
                )
            })
    }

    companion object {
        val REGISTRATE: GTLAddRegistration = GTLAddRegistration()
    }
}
