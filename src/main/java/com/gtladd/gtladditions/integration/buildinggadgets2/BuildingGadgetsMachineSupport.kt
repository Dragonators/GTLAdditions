package com.gtladd.gtladditions.integration.buildinggadgets2

import com.gregtechceu.gtceu.api.block.MetaMachineBlock
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gtladd.gtladditions.common.machine.hatch.CloudOpticalComputationHatchMachine
import com.gtladd.gtladditions.common.machine.trait.StructureModifyTrait
import com.gtladd.gtladditions.utils.cloud.CloudNetworkManager
import com.hepdd.gtmthings.api.capability.IBindable
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID
import kotlin.concurrent.withLock

object BuildingGadgetsMachineSupport {
    const val PLACER_KEY = "gtladditions:gadget_placer"

    @JvmStatic
    fun newMachineData(state: BlockState, player: Player, restoring: Boolean): CompoundTag? {
        if (restoring || state.block !is MetaMachineBlock) return null
        return CompoundTag().apply {
            putUUID("uuid", player.uuid)
            putUUID(PLACER_KEY, player.uuid)
        }
    }

    @JvmStatic
    fun loadMachineData(blockEntity: BlockEntity, load: Runnable) {
        val controller = (blockEntity as? MetaMachineBlockEntity)?.metaMachine as? MultiblockControllerMachine
            ?: return load.run()
        val structureModify = controller.traits.firstOrNull { it is StructureModifyTrait } as? StructureModifyTrait
            ?: return load.run()
        controller.patternLock.withLock {
            try {
                load.run()
            } finally {
                // Gadgets loads NBT after onLoad; initialize the loaded structure before asynchronous checks resume.
                structureModify.onMachineLoad()
            }
        }
    }

    @JvmStatic
    fun finishPlacement(level: ServerLevel, pos: BlockPos, data: CompoundTag) {
        val holder = level.getBlockEntity(pos) as? MetaMachineBlockEntity ?: return
        val machine = holder.metaMachine
        if (data.hasUUID(PLACER_KEY)) {
            val owner: UUID = data.getUUID(PLACER_KEY)
            (machine as? IBindable)?.uuid = owner
            (machine as? IMachineLife)?.onMachinePlaced(
                level.getPlayerByUUID(owner),
                machine.definition.asStack()
            )
        }
        // Gadgets loads its saved data after clearRemoved has already run the machine's onLoad.
        (machine as? IRecipeLogicMachine)?.recipeLogic?.updateTickSubscription()
        if (machine is CloudOpticalComputationHatchMachine) {
            CloudNetworkManager.invalidateComputationTopology()
        }
    }

    @JvmStatic
    fun stopCutMachine(level: ServerLevel, pos: BlockPos) {
        val holder = level.getBlockEntity(pos) as? MetaMachineBlockEntity ?: return
        (holder.metaMachine as? IRecipeLogicMachine)?.recipeLogic?.resetRecipeLogic()
    }
}