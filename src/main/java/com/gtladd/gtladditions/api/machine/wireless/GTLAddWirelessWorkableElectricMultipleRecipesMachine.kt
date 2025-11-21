package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.common.data.GTItems
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager
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
import org.gtlcore.gtlcore.utils.NumberUtils
import java.math.BigInteger
import java.util.*

open class GTLAddWirelessWorkableElectricMultipleRecipesMachine(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder, *args), IMachineLife {
    @field:Persisted
    var uuid: UUID? = null
        protected set

    val selfWirelessNetworkTrait = SelfWirelessNetworkHandler()

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleWirelessRecipesLogic(this)
    }

    override fun getRecipeLogic(): GTLAddMultipleWirelessRecipesLogic {
        return super.getRecipeLogic() as GTLAddMultipleWirelessRecipesLogic
    }

    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (player.getItemInHand(hand).`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            this.uuid = player.getUUID()
            if (isRemote) {
                player.sendSystemMessage(
                    Component.translatable(
                        "gtmthings.machine.wireless_energy_hatch.tooltip.bind",
                        TeamUtil.GetName(player)
                    )
                )
            } else if(this.isFormed){
                this.refreshTier()
            }
            return InteractionResult.sidedSuccess(isRemote)
        }
        return super.onUse(state, world, pos, player, hand, hit)
    }

    override fun onLeftClick(
        player: Player,
        world: Level,
        hand: InteractionHand,
        pos: BlockPos,
        direction: Direction
    ): Boolean {
        val itemStack = player.getItemInHand(hand)
        if (itemStack.isEmpty) return false
        if (itemStack.`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            this.uuid = null
            if (isRemote) {
                player.sendSystemMessage(
                    Component.translatable(
                        "gtmthings.machine.wireless_energy_hatch.tooltip.unbind"
                    )
                )
            }
            return true
        }
        return false
    }

    override fun onMachinePlaced(player: LivingEntity?, stack: ItemStack) {
        player?.let { this.uuid = it.getUUID() }
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        this.refreshTier()
    }

    override fun getMaxVoltage(): Long {
        return 0
    }

    override fun getWirelessNetworkEnergyHandler(): IWirelessNetworkEnergyHandler {
        return selfWirelessNetworkTrait
    }

    fun refreshTier() {
        uuid?.let {
            val totalEu = WirelessEnergyManager.getUserEU(it)
            val longEu = NumberUtils.getLongValue(totalEu)
            this.tier = if(longEu == Long.MAX_VALUE) GTValues.MAX_TRUE else NumberUtils.getFakeVoltageTier(longEu)
        }
    }
    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    companion object {
        @JvmStatic
        protected val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            GTLAddWirelessWorkableElectricMultipleRecipesMachine::class.java,
            GTLAddWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )

        @JvmStatic
        protected val SELF_WIRELESS_NETWORK_PROXY_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            GTLAddWirelessWorkableElectricMultipleRecipesMachine::class.java
        )
    }

    inner class SelfWirelessNetworkHandler : MachineTrait(this), IWirelessNetworkEnergyHandler {

        override fun consumeEnergy(energy: Int): Boolean {
            return uuid != null && WirelessEnergyManager.addEUToGlobalEnergyMap(
                uuid,
                energy,
                this@GTLAddWirelessWorkableElectricMultipleRecipesMachine
            )
        }

        override fun consumeEnergy(energy: Long): Boolean {
            return uuid != null && WirelessEnergyManager.addEUToGlobalEnergyMap(
                uuid,
                energy,
                this@GTLAddWirelessWorkableElectricMultipleRecipesMachine
            )
        }

        override fun consumeEnergy(energy: BigInteger): Boolean {
            return uuid != null && WirelessEnergyManager.addEUToGlobalEnergyMap(
                uuid,
                energy,
                this@GTLAddWirelessWorkableElectricMultipleRecipesMachine
            )
        }

        override val maxAvailableEnergy: BigInteger
            get() = if (uuid != null) WirelessEnergyManager.getUserEU(uuid) else BigInteger.ZERO

        override val isOnline: Boolean
            get() = uuid != null && WirelessEnergyManager.getUserEU(uuid).signum() > 0

        override fun getFieldHolder(): ManagedFieldHolder {
            return SELF_WIRELESS_NETWORK_PROXY_FIELD_HOLDER
        }
    }
}
