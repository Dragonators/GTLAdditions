package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.api.machine.data.ParallelData
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine
import com.gtladd.gtladditions.api.recipe.ChanceParallelLogic
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.gtladd.gtladditions.utils.CommonUtils
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers
import java.math.BigDecimal
import java.math.BigInteger

class ApocalypticTorsionQuantumMatrix(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine(
        holder,
        GTLAddRecipesTypes.QUANTUM_OSCILLATION,
        *args
    ) {

    override fun getMaxParallel(): Int {
        return GTLRecipeModifiers.getHatchParallel(this)
    }

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return ApocalypticTorsionQuantumMatrixLogic(this)
    }

    override fun getRecipeLogic(): ApocalypticTorsionQuantumMatrixLogic {
        return super.getRecipeLogic() as ApocalypticTorsionQuantumMatrixLogic
    }

    companion object {
        class ApocalypticTorsionQuantumMatrixLogic(parallel: ApocalypticTorsionQuantumMatrix?) :
            GTLAddMultipleTypeWirelessRecipesLogic(parallel) {
            init {
                this.setReduction(0.2, 1.0)
            }

            override fun getMachine(): ApocalypticTorsionQuantumMatrix {
                return super.getMachine() as ApocalypticTorsionQuantumMatrix
            }

            override fun getMultipleThreads(): Int {
                return Ints.saturatedCast(MAX_THREADS + getMachine().additionalThread)
            }

            override fun getGTRecipe(): GTRecipe? {
                if (!checkBeforeWorking()) return null

                val recipes: Set<GTRecipe?> = this.lookupRecipeIterator()
                val length = recipes.size
                if (length == 0) return null

                val maxTotalEu = getMachine().wirelessNetworkEnergyHandler.maxAvailableEnergy
                val euMultiplier = this.euMultiplier
                val itemOutputs = ObjectArrayList<Content?>()
                val fluidOutputs = ObjectArrayList<Content?>()

                var totalEu = BigInteger.ZERO
                var remain = this.parallel.maxParallel * multipleThreads.toLong()

                for (match in recipes) {
                    if (match == null) continue
                    if (remain <= 0) break
                    var modifiedMatch = modifyChance(match)
                    val p = getMaxParallel(modifiedMatch, remain)
                    if (p <= 0) continue

                    var parallelEUt = BigInteger.valueOf(RecipeHelper.getInputEUt(match))
                    modifiedMatch = if (p > 1) run {
                        parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p))
                        CommonUtils.copyFixRecipe(modifiedMatch, ContentModifier.multiplier(p.toDouble()), INPUT_CHANCE_RATIO)
                    } else modifiedMatch
                    IGTRecipe.of(modifiedMatch).realParallels = p

                    val tempTotalEu = totalEu.add(BigDecimal.valueOf(modifiedMatch.duration * euMultiplier).multiply(BigDecimal(parallelEUt)).toBigInteger())
                    if (tempTotalEu > maxTotalEu) {
                        if (totalEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN)
                        break
                    }

                    if (RecipeRunnerHelper.handleRecipeInput(machine, modifiedMatch)) {
                        remain -= p
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

            override fun getMaxParallel(recipe: GTRecipe, limit: Long): Long {
                return ChanceParallelLogic.getMaxParallel(
                    getMachine(),
                    recipe,
                    limit,
                    chanceCaches,
                    recipe.type.chanceFunction,
                    IGTRecipe.of(recipe).euTier,
                    getMachine().tier
                )
            }

            // Disable
            override fun buildFinalNormalRecipe(parallelData: ParallelData?): GTRecipe? {
                return null
            }

            // Disable
            override fun calculateParallels(): ParallelData? {
                return null
            }

            companion object {
                private const val MAX_THREADS: Long = 1536
                private const val INPUT_CHANCE_RATIO = 10

                private fun modifyContents(before: Map<RecipeCapability<*>, MutableList<Content>>, isInput: Boolean): Reference2ReferenceOpenHashMap<RecipeCapability<*>, MutableList<Content>> {
                    val after = Reference2ReferenceOpenHashMap<RecipeCapability<*>, MutableList<Content>>()
                    for (entry in before) {
                        val cap = entry.key
                        val contentList = after.computeIfAbsent(cap) { ObjectArrayList() }
                        for (cont in entry.value) {
                            if (cont.chance >= cont.maxChance) contentList.add(cont)
                            else if(cont.chance != 0) {
                                val copy = cont.copy(cap, null)
                                if (isInput) copy.maxChance = cont.maxChance * INPUT_CHANCE_RATIO
                                else copy.chance = cont.maxChance
                                contentList.add(copy)
                            }
                        }
                        if (contentList.isEmpty()) after.remove(cap)
                    }
                    return after
                }

                private fun modifyChance(recipe: GTRecipe): GTRecipe {
                    val copy = GTRecipe(
                        recipe.recipeType,
                        recipe.id,
                        modifyContents(recipe.inputs, true),
                        modifyContents(recipe.outputs, false),
                        recipe.tickInputs,
                        recipe.tickOutputs,
                        recipe.inputChanceLogics,
                        recipe.outputChanceLogics,
                        recipe.tickInputChanceLogics,
                        recipe.tickOutputChanceLogics,
                        recipe.conditions,
                        recipe.ingredientActions,
                        recipe.data,
                        recipe.duration,
                        recipe.isFuel
                    )
                    IGTRecipe.of(copy).realParallels = IGTRecipe.of(recipe).realParallels
                    copy.ocTier = recipe.ocTier
                    return copy
                }
            }
        }
    }
}