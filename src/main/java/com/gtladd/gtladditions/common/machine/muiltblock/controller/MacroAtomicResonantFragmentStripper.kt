package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.api.machine.data.ParallelData
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import java.math.BigDecimal
import java.math.BigInteger
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

            override fun getGTRecipe(): GTRecipe? {
                if (!checkBeforeWorking()) return null
                val wirelessTrait = getMachine().wirelessNetworkEnergyHandler
                return if (wirelessTrait != null) buildFinalWirelessRecipe(
                    null,
                    wirelessTrait
                ) else buildFinalNormalRecipe(null)
            }

            override fun buildFinalNormalRecipe(parallelData: ParallelData?): GTRecipe? {
                val recipes: Set<GTRecipe?> = this.lookupRecipeIterator()
                val length = recipes.size
                if (length == 0) return null

                val maxEUt = getMachine().overclockVoltage
                val euMultiplier = this.euMultiplier
                val itemOutputs = ObjectArrayList<Content?>()
                val fluidOutputs = ObjectArrayList<Content?>()
                val output = GTRecipeBuilder.ofRaw().buildRawRecipe()
                output.outputs.put(ItemRecipeCapability.CAP, itemOutputs)
                output.outputs.put(FluidRecipeCapability.CAP, fluidOutputs)

                var totalEu = 0.0
                val eachParallel = this.parallel.maxParallel.toLong()

                for (match in recipes) {
                    if (match == null) continue
                    val p = getMaxParallel(match, eachParallel)
                    if (p <= 0) continue

                    var modifiedMatch = if (p > 1) match.copy(ContentModifier.multiplier(p.toDouble()), false) else match
                    IGTRecipe.of(modifiedMatch).realParallels = p
                    modifiedMatch = modifyInputAndOutput(modifiedMatch)
                    if (RecipeRunnerHelper.handleRecipeInput(machine, modifiedMatch)) {
                        totalEu += getTotalEuOfRecipe(match) * euMultiplier
                        modifiedMatch.outputs[ItemRecipeCapability.CAP]?.let { itemOutputs.addAll(it) }
                        modifiedMatch.outputs[FluidRecipeCapability.CAP]?.let { fluidOutputs.addAll(it) }
                    }
                    if (totalEu / maxEUt > 20 * 500) break
                }
                if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
                    if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(
                        this.machine,
                        RecipeResult.FAIL_FIND
                    )
                    return null
                }
                val d = totalEu / maxEUt
                val eut = if (d > 20) maxEUt else (maxEUt * d / 20).toLong()
                output.tickInputs.put(
                    EURecipeCapability.CAP,
                    listOf(Content(eut, 10000, 10000, 0, null, null))
                )
                output.duration = max(d, 20.0).toInt()
                IGTRecipe.of(output).setHasTick(true)
                return output
            }

            override fun buildFinalWirelessRecipe(
                parallelData: ParallelData?,
                wirelessTrait: IWirelessNetworkEnergyHandler
            ): WirelessGTRecipe? {
                if (!wirelessTrait.isOnline()) return null

                val recipes: Set<GTRecipe?> = this.lookupRecipeIterator()
                val length = recipes.size
                if (length == 0) return null

                val maxTotalEu = wirelessTrait.maxAvailableEnergy
                val euMultiplier = this.euMultiplier
                val itemOutputs = ObjectArrayList<Content?>()
                val fluidOutputs = ObjectArrayList<Content?>()

                var totalEu = BigInteger.ZERO
                val eachParallel = this.parallel.maxParallel.toLong()

                for (match in recipes) {
                    if (match == null) continue
                    val p = getMaxParallel(match, eachParallel)
                    if (p <= 0) continue

                    var parallelEUt = BigInteger.valueOf(RecipeHelper.getInputEUt(match))
                    var modifiedMatch = if (p > 1) run {
                        parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p))
                        match.copy(ContentModifier.multiplier(p.toDouble()), false)
                    } else match
                    IGTRecipe.of(modifiedMatch).realParallels = p

                    val tempTotalEu = totalEu.add(BigDecimal.valueOf(modifiedMatch.duration * euMultiplier).multiply(BigDecimal(parallelEUt)).toBigInteger())
                    if (tempTotalEu > maxTotalEu) {
                        if (totalEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN)
                        break
                    }

                    modifiedMatch = modifyInputAndOutput(modifiedMatch)
                    if (RecipeRunnerHelper.handleRecipeInput(machine, modifiedMatch)) {
                        totalEu = tempTotalEu
                        modifiedMatch.outputs[ItemRecipeCapability.CAP]?.let { itemOutputs.addAll(it) }
                        modifiedMatch.outputs[FluidRecipeCapability.CAP]?.let { fluidOutputs.addAll(it) }
                    }
                }

                if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
                    if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(
                        this.machine,
                        RecipeResult.FAIL_FIND
                    )
                    return null
                }

                val minDuration = limited.limitedDuration
                val eut = totalEu.divide(BigInteger.valueOf(minDuration.toLong())).negate()
                return buildWirelessRecipe(itemOutputs, fluidOutputs, minDuration, eut)
            }

            override fun calculateParallels(): ParallelData? {
                return null
            }
        }
    }
}
