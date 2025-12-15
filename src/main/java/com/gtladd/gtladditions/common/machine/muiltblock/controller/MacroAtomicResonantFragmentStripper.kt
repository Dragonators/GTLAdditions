package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.google.common.base.Predicate
import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

class MacroAtomicResonantFragmentStripper(holder: IMachineBlockEntity) :
    GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder), IAstralArrayInteractionMachine {

    @field:Persisted
    override var astralArrayCount: Int = 0

    @field:Persisted
    private var parallelAmount: Long = 1

    fun getRealParallel(): Long = parallelAmount

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    override fun needConfirmMEStock(): Boolean = true

    override fun getMaxParallel(): Int = Ints.saturatedCast(parallelAmount)

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return MacroAtomicResonantFragmentStripperLogic(this)
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        parallelAmount = calculateParallelAmount(astralArrayCount, this.coilType.coilTemperature)
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (isFormed) {
            textList.add(
                Component.translatable(
                    "tooltip.gtladditions.astral_array_count",
                    Component.literal(astralArrayCount.toString()).withStyle(ChatFormatting.GOLD)
                )
            )
        }
    }

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        if (parallelAmount > 1) {
            textList.add(
                Component.translatable(
                    "gtceu.multiblock.parallel",
                    Component.literal(FormattingUtil.formatNumbers(parallelAmount)).withStyle(ChatFormatting.DARK_PURPLE)
                ).withStyle(ChatFormatting.GRAY)
            )
        }
        textList.add(
            Component.translatable(
                "gtladditions.multiblock.threads",
                Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel")
                    .withStyle(ChatFormatting.GOLD)
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    override fun increaseAstralArrayCount(amount: Int): Int {
        val actualIncrease = minOf(amount, MAX_ASTRAL_ARRAY_COUNT - astralArrayCount)
        if (actualIncrease > 0) {
            astralArrayCount += actualIncrease
            parallelAmount = calculateParallelAmount(astralArrayCount, this.coilType.coilTemperature)
        }
        return actualIncrease
    }

    companion object {
        const val MAX_ASTRAL_ARRAY_COUNT = 256

        val FRAGMENT_STRIPPER = Predicate { machine: IRecipeLogicMachine ->
            return@Predicate if (machine is MacroAtomicResonantFragmentStripper) machine.coilType.coilTemperature >= 21600 else false
        }

        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(
                MacroAtomicResonantFragmentStripper::class.java,
                GTLAddWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
            )

        /**
         * Formula: parallelMultiplier = 2^(6 + 10*((astralArrayCount - 1)/184)^2)
         */
        fun calculateParallelAmount(count: Int, temperature: Int): Long {
            val base = (1536L + max(temperature - 21600, 0) / 1200 * 300)
            if (count == 0) return base
            val normalized = (count - 1) / 184.0
            val exponent = 6 + 10 * normalized * normalized
            return (base * 2.0.pow(exponent)).roundToLong()
        }

        class MacroAtomicResonantFragmentStripperLogic(parallel: MacroAtomicResonantFragmentStripper) :
            GTLAddMultipleRecipesLogic(parallel, FRAGMENT_STRIPPER) {
            init {
                this.setReduction(4.0, 1.0)
            }

            override fun getMachine(): MacroAtomicResonantFragmentStripper {
                return super.getMachine() as MacroAtomicResonantFragmentStripper
            }

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes, machine,
                    getParallelLimitForRecipe = { getMachine().parallelAmount },
                    getMaxParallelForRecipe = ::getMaxParallel
                )
            }
        }
    }
}
