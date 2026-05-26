package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricParallelHatchMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.Constants.INT_MAX_BIG
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.gtlcore.gtlcore.utils.MachineIO
import org.gtlcore.gtlcore.utils.Registries
import java.math.BigInteger
import kotlin.math.ln
import kotlin.math.max

class AntientropyCondensationCenter(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricParallelHatchMultipleRecipesMachine(holder, *args),
    IMachineModifyDrops {

    @Persisted
    @DescSynced
    private var relativisticHeatCapacitorInstalled = false

    @Persisted
    @DescSynced
    private var lastCryotheumDustCost = 0

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = AntientropyCondensationCenterLogic(this)

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!isFormed()) return
        textList.addAll(
            arrayOf(
                "gtceu.multiblock.antientropy_condensation_center.dust_cryotheum".toComponent(
                    getDisplayedCryotheumDustCost()
                ),
                "gtladditions.machine.antientropy_condensation_center.heat_capacitor".toComponent(
                    (if (hasRelativisticHeatCapacitor()) "✓" else "x").literal
                        .withStyle(if (hasRelativisticHeatCapacitor()) ChatFormatting.GREEN else ChatFormatting.RED)
                )
            )
        )
    }

    override fun onUse(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (stack.`is`(GTLAddItems.RELATIVISTIC_HEAT_CAPACITOR.asItem())) {
            if (world.isClientSide) return InteractionResult.SUCCESS

            val serverPlayer = player as? ServerPlayer
            when {
                relativisticHeatCapacitorInstalled -> {
                    serverPlayer?.sendSystemMessage("gtladditions.message.antientropy_condensation_center.heat_capacitor_already_installed".toComponent)
                }
                stack.count < HEAT_CAPACITOR_STACK_SIZE -> {
                    serverPlayer?.sendSystemMessage("gtladditions.message.antientropy_condensation_center.heat_capacitor_need_stack".toComponent)
                }
                else -> {
                    if (!player.isCreative) stack.shrink(HEAT_CAPACITOR_STACK_SIZE)
                    relativisticHeatCapacitorInstalled = true
                    serverPlayer?.sendSystemMessage("gtladditions.message.antientropy_condensation_center.heat_capacitor_installed".toComponent)
                }
            }

            return InteractionResult.SUCCESS
        }

        return super.onUse(state, world, pos, player, hand, hit)
    }

    override fun onDrops(list: MutableList<ItemStack>) {
        if (relativisticHeatCapacitorInstalled) {
            list.add(ItemStack(GTLAddItems.RELATIVISTIC_HEAT_CAPACITOR.asItem(), HEAT_CAPACITOR_STACK_SIZE))
        }
    }

    fun hasRelativisticHeatCapacitor(): Boolean = relativisticHeatCapacitorInstalled

    fun getDisplayedCryotheumDustCost(): Int = lastCryotheumDustCost

    private fun consumeCryotheumDust(parallelData: ParallelData): Boolean {
        val cost = calculateCryotheumDustCost(totalParallels(parallelData))
        lastCryotheumDustCost = cost
        if (cost <= 0) return true
        return MachineIO.inputItem(this, Registries.getItemStack("kubejs:dust_cryotheum", cost))
    }

    private fun calculateCryotheumDustCost(totalParallel: BigInteger): Int {
        if (totalParallel.signum() <= 0) return 0

        if (relativisticHeatCapacitorInstalled) {
            return if (totalParallel <= INT_MAX_BIG) 0 else 1
        }

        val parallel = totalParallel.toDouble()
        val count = 5.0 * (parallel / 524288.0 + 51.0 * ln(parallel)) / max(1, tier - 9)
        return when {
            count.isNaN() || count <= 0.0 -> 0
            count.isInfinite() || count >= Int.MAX_VALUE -> Int.MAX_VALUE
            else -> count.toInt()
        }
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private const val HEAT_CAPACITOR_STACK_SIZE = 64

        private fun totalParallels(parallelData: ParallelData): BigInteger = parallelData.parallels.fold(BigInteger.ZERO) { total, parallel ->
            total.add(BigInteger.valueOf(parallel.coerceAtLeast(0L)))
        }

        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            AntientropyCondensationCenter::class.java,
            GTLAddWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )

        private class AntientropyCondensationCenterLogic(machine: AntientropyCondensationCenter) : GTLAddMultipleRecipesLogic(machine) {

            override fun getMachine(): AntientropyCondensationCenter = super.getMachine() as AntientropyCondensationCenter

            override fun getEuMultiplier(): Double =
                super.getEuMultiplier() * if (getMachine().hasRelativisticHeatCapacitor()) 0.35 else 1.0

            override fun calculateParallels(): ParallelData? {
                val parallelData = super.calculateParallels() ?: return null
                return if (getMachine().consumeCryotheumDust(parallelData)) parallelData else null
            }
        }
    }
}