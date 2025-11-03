package com.gtladd.gtladditions.common.machine.muiltblock.part

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider.PageGroupingData
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gregtechceu.gtceu.common.data.GTItems
import com.gtladd.gtladditions.common.machine.trait.NetworkEnergyContainer
import com.hepdd.gtmthings.api.capability.IBindable
import com.hepdd.gtmthings.utils.TeamUtil
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import java.util.*

open class WirelessEnergyNetworkTerminalPartMachineBase(holder: IMachineBlockEntity, protected val io: IO) :
    MultiblockPartMachine(holder), IInteractedMachine, IBindable, IMachineLife {
    @field:Persisted
    protected var uuid: UUID? = null
    @field:Persisted
    val energyContainer: NetworkEnergyContainer

    init {
        this.energyContainer = createEnergyContainer()
    }

    override fun onUse(
        state: BlockState?,
        world: Level?,
        pos: BlockPos?,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult?
    ): InteractionResult {
        if (player.getItemInHand(hand).`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            setUUID(player.getUUID())
            if (isRemote) player.sendSystemMessage(
                Component.translatable(
                    "gtmthings.machine.wireless_energy_hatch.tooltip.bind",
                    TeamUtil.GetName(player)
                )
            )
            return InteractionResult.sidedSuccess(isRemote)
        }
        return InteractionResult.PASS
    }

    override fun onLeftClick(
        player: Player,
        world: Level?,
        hand: InteractionHand,
        pos: BlockPos?,
        direction: Direction?
    ): Boolean {
        val `is` = player.getItemInHand(hand)
        if (`is`.isEmpty) return false
        if (`is`.`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            this.uuid = null
            if (isRemote) {
                player.sendSystemMessage(Component.translatable("gtmthings.machine.wireless_energy_hatch.tooltip.unbind"))
            }
            return true
        }
        return false
    }

    override fun onMachinePlaced(player: LivingEntity?, stack: ItemStack?) {
        if (player != null) this.setUUID(player.getUUID())
    }

    override fun getPageGroupingData(): PageGroupingData? {
        return when (this.io) {
            IO.IN -> PageGroupingData("gtceu.multiblock.page_switcher.io.import", 1)
            IO.OUT -> PageGroupingData("gtceu.multiblock.page_switcher.io.export", 2)
            IO.BOTH -> PageGroupingData("gtceu.multiblock.page_switcher.io.both", 3)
            IO.NONE -> null
        }
    }

    protected fun createEnergyContainer(vararg args: Any?): NetworkEnergyContainer {
        return NetworkEnergyContainer(this, io)
    }

    override fun getUUID(): UUID? {
        return uuid
    }

    override fun setUUID(uuid: UUID?) {
        this.uuid = uuid
        for (controller in controllers) {
            if (controller is IRecipeLogicMachine) {
                controller.recipeLogic.updateTickSubscription()
            }
        }
    }

    override fun shouldOpenUI(player: Player?, hand: InteractionHand?, hit: BlockHitResult?): Boolean = false

    override fun canShared(): Boolean = false

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        protected val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            WirelessEnergyNetworkTerminalPartMachineBase::class.java, MultiblockPartMachine.MANAGED_FIELD_HOLDER
        )
    }
}
