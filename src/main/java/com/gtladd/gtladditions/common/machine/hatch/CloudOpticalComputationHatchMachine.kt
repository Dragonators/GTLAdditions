package com.gtladd.gtladditions.common.machine.hatch

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gtladd.gtladditions.api.machine.trait.CloudOpticalComputationContainer
import com.gtladd.gtladditions.utils.CloudNetworkManager
import com.gtladd.gtladditions.utils.CloudTeamUtil
import com.hepdd.gtmthings.api.capability.IBindable
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import java.util.UUID

class CloudOpticalComputationHatchMachine(
    holder: IMachineBlockEntity,
    private val transmitter: Boolean
) : MultiblockPartMachine(holder),
    IMachineLife,
    IDataStickInteractable,
    IBindable {

    val computationContainer = CloudOpticalComputationContainer(
        this,
        if (transmitter) IO.OUT else IO.IN,
        transmitter
    )

    @Persisted
    @DescSynced
    var teamId: UUID? = null
        private set

    fun isTransmitter(): Boolean = transmitter

    override fun shouldOpenUI(player: Player?, hand: InteractionHand?, hit: BlockHitResult?): Boolean = false

    override fun onDataStickRightClick(player: Player, stack: ItemStack): InteractionResult =
        CloudTeamUtil.bindFromDataStick(this, player)

    override fun onDataStickLeftClick(player: Player, stack: ItemStack): Boolean =
        CloudTeamUtil.unbindFromDataStick(this, player)

    override fun onMachineRemoved() {
        if (!isRemote) CloudNetworkManager.unregisterComputationHatch(this)
    }

    override fun onMachinePlaced(player: LivingEntity?, stack: ItemStack) {
        CloudTeamUtil.bindOnPlacement(this, player)
        if (!isRemote) CloudNetworkManager.registerComputationHatch(this)
    }

    override fun onLoad() {
        super.onLoad()
        if (!isRemote) CloudNetworkManager.registerComputationHatch(this)
    }

    override fun onUnload() {
        if (!isRemote) CloudNetworkManager.unregisterComputationHatch(this)
        super.onUnload()
    }

    override fun addedToController(controller: IMultiController) {
        super.addedToController(controller)
        if (!isRemote) CloudNetworkManager.invalidateComputationTopology()
    }

    override fun removedFromController(controller: IMultiController) {
        super.removedFromController(controller)
        if (!isRemote) CloudNetworkManager.invalidateComputationTopology()
    }

    override fun getUUID(): UUID? = teamId

    override fun setUUID(uuid: UUID?) {
        if (teamId == uuid) return
        teamId = uuid
        CloudNetworkManager.invalidateComputationTopology()
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {

        @JvmField
        val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            CloudOpticalComputationHatchMachine::class.java,
            MultiblockPartMachine.MANAGED_FIELD_HOLDER
        )
    }
}