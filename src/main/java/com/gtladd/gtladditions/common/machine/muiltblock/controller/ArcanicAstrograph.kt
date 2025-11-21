package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.common.machine.multiblock.electric.HarmonyMachine
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

class ArcanicAstrograph(holder: IMachineBlockEntity, vararg args: Any?) : HarmonyMachine(holder, *args),
    IAstralArrayInteractionMachine {

    @field:Persisted
    override var astralArrayCount: Int = 0

    @field:Persisted
    private var parallelAmount: Int = 2048

    override fun addDisplayText(textList: MutableList<Component>) {
        super.addDisplayText(textList)
        if (isFormed) {
            textList.add(
                Component.translatable(
                    "tooltip.gtladditions.astral_array_count",
                    Component.literal(astralArrayCount.toString()).withStyle(ChatFormatting.GOLD)
                )
            )
            textList.add(
                Component.translatable(
                    "tooltip.gtladditions.arcanic_parallel_amount",
                    Component.literal(parallelAmount.toString()).withStyle(ChatFormatting.GOLD)
                )
            )
        }
    }

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    override fun increaseAstralArrayCount(amount: Int): Int {
        astralArrayCount += amount
        parallelAmount = calculateParallelAmount(astralArrayCount)
        return amount
    }

    companion object {

        /**
         * Formula: parallelAmount = 2048 + 2^floor(log_{1.7}(8 * count)) * 128
         * Uses Long to prevent overflow, clamped to Int.MAX_VALUE
         */
        private fun calculateParallelAmount(count: Int): Int {
            // floor(log_{1.7}(8 * count)) = floor(ln(8 * count) / ln(1.7))
            val logBase17 = floor(ln(8.0 * count) / ln(1.7)).toInt()

            // 2^logBase17 * 128
            val additional = 2.0.pow(logBase17) * 128

            val result = 2048L + additional.toLong()
            return Ints.saturatedCast(result)
        }

        fun recipeModifier(machine: MetaMachine, recipe: GTRecipe, params: OCParams, result: OCResult): GTRecipe? {
            HarmonyMachine.recipeModifier(machine, recipe, params, result)
                ?.let {
                    return GTRecipeModifiers.accurateParallel(
                        machine,
                        it,
                        (machine as ArcanicAstrograph).parallelAmount,
                        false
                    ).getFirst()
                }
            return null
        }

        private val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            ArcanicAstrograph::class.java,
            HarmonyMachine.MANAGED_FIELD_HOLDER
        )
    }
}
