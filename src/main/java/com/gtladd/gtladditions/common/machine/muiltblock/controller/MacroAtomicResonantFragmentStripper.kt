package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.api.machine.data.ParallelData
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import kotlin.math.max
import kotlin.math.pow

class MacroAtomicResonantFragmentStripper(holder: IMachineBlockEntity) :
    GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder), IAstralArrayInteractionMachine {

    @field:Persisted
    private var astralArrayCount: Int = 0

    @field:Persisted
    private var parallelMultiplier: Int = 1

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    override fun getMaxParallel(): Int {
        return (1536 + max(this.coilType.coilTemperature - 21600, 0) / 1200 * 300) * parallelMultiplier
    }

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return MacroAtomicResonantFragmentStripperLogic(this)
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
        if (maxParallel > 1) {
            textList.add(
                Component.translatable(
                    "gtceu.multiblock.parallel",
                    Component.literal(FormattingUtil.formatNumbers(maxParallel)).withStyle(ChatFormatting.DARK_PURPLE)
                ).withStyle(ChatFormatting.GRAY)
            )
        }
        textList.add(
            Component.translatable(
                "gtladditions.multiblock.threads",
                Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").withStyle(ChatFormatting.GOLD)
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    override fun increaseAstralArrayCount(amount: Int): Int {
        val actualIncrease = minOf(amount, MAX_ASTRAL_ARRAY_COUNT - astralArrayCount)
        if (actualIncrease > 0) {
            astralArrayCount += actualIncrease
            parallelMultiplier = calculateParallelMultiplier(astralArrayCount)
        }
        return actualIncrease
    }

    override fun getAstralArrayCount(): Int {
        return astralArrayCount
    }

    companion object{
        const val MAX_ASTRAL_ARRAY_COUNT = 66

        val FRAGMENT_STRIPPER = Predicate { machine: IRecipeLogicMachine? ->
            return@Predicate if (machine is MacroAtomicResonantFragmentStripper) machine.coilType.coilTemperature >= 21600 else false
        }

        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(
                MacroAtomicResonantFragmentStripper::class.java,
                GTLAddWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
            )

        /**
         * Formula: parallelMultiplier = 2^(6 + 10*((astralArrayCount - 1)/63)^2)
         */
        fun calculateParallelMultiplier(count: Int): Int {
            if (count == 0) return 1
            val normalized = (count - 1) / 63.0
            val exponent = 6 + 10 * normalized * normalized
            return 2.0.pow(exponent).toInt()
        }

        class MacroAtomicResonantFragmentStripperLogic(parallel: MacroAtomicResonantFragmentStripper?) :
            GTLAddMultipleRecipesLogic(parallel, FRAGMENT_STRIPPER){
            init {
                this.setReduction(4.0, 1.0)
            }

            override fun getMachine(): MacroAtomicResonantFragmentStripper {
                return super.getMachine() as MacroAtomicResonantFragmentStripper
            }

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                if (recipes.isEmpty()) return null

                val recipeList = ObjectArrayList<GTRecipe>(recipes.size)
                val parallelsList = LongArrayList(recipes.size)
                val eachParallel = this.parallel.maxParallel.toLong()

                for (recipe in recipes) {
                    recipe ?: continue
                    val parallel = getMaxParallel(recipe, eachParallel)
                    if (parallel > 0) {
                        recipeList.add(recipe)
                        parallelsList.add(parallel)
                    }
                }

                return if (recipeList.isEmpty()) null
                else ParallelData(recipeList, parallelsList.toLongArray())
            }
        }
    }
}
