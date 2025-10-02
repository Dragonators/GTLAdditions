package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager
import com.hepdd.gtmthings.utils.TeamUtil
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues
import org.gtlcore.gtlcore.utils.NumberUtils
import java.util.*

open class GTLAddWirelessWorkableElectricMultipleRecipesMachine(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder, *args), IMachineLife {
    @field:Persisted
    var uuid: UUID? = null
        protected set

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleWirelessRecipesLogic(this)
    }

    override fun getRecipeLogic(): GTLAddMultipleWirelessRecipesLogic {
        return super.getRecipeLogic() as GTLAddMultipleWirelessRecipesLogic
    }

    override fun onUse(
        state: BlockState?,
        world: Level?,
        pos: BlockPos?,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult?
    ): InteractionResult? {
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

    override fun addEnergyDisplay(textList: MutableList<Component?>) {
        uuid?.let {
            val totalEu = WirelessEnergyManager.getUserEU(uuid).divide(GTLAddMultipleWirelessRecipesLogic.MAX_EU_RATIO)
            val longEu = NumberUtils.getLongValue(totalEu)
            val energyTier = if(longEu == Long.MAX_VALUE) GTValues.MAX_TRUE else NumberUtils.getFakeVoltageTier(longEu)

            // Max energy per tick
            textList.add(Component.translatable("gtceu.multiblock.max_energy_per_tick",
                FormattingUtil.formatNumbers(totalEu),
                Component.literal(NewGTValues.VNF[energyTier]))
                .withStyle(ChatFormatting.GRAY)
                .withStyle { it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.translatable("gtceu.multiblock.max_energy_per_tick_hover")
                        .withStyle(ChatFormatting.GRAY))) })

            // Max recipe tier
            textList.add(Component.translatable("gtceu.multiblock.max_recipe_tier",
                Component.literal(GTValues.VNF[energyTier.coerceAtMost(14)]))
                .withStyle(ChatFormatting.GRAY)
                .withStyle { it.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.translatable("gtceu.multiblock.max_recipe_tier_hover")
                        .withStyle(ChatFormatting.GRAY))) })
        }
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
    }
}
