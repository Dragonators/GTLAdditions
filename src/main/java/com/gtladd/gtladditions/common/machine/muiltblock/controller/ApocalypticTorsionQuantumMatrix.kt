package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleTypeWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine
import com.gtladd.gtladditions.api.recipe.ChanceParallelLogic
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ObjectFunction
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
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

    class ApocalypticTorsionQuantumMatrixLogic(parallel: ApocalypticTorsionQuantumMatrix?) :
        GTLAddMultipleTypeWirelessRecipesLogic(parallel) {
        init {
            this.setReduction(0.2, 1.0)
        }

        override fun getMachine(): ApocalypticTorsionQuantumMatrix {
            return super.getMachine() as ApocalypticTorsionQuantumMatrix
        }

        override fun getMultipleThreads(): Int {
            return MAX_THREADS.toInt()
        }

       override fun getGTRecipe(): GTRecipe? {
            if (!checkBeforeWorking()) return null

           val recipes: Set<GTRecipe?> = this.lookupRecipeIterator()
           val length = recipes.size
           if (length == 0) return null

           val uuid = getMachine().uuid
           checkNotNull(uuid)
           val maxTotalEu = WirelessEnergyManager.getUserEU(uuid).divide(MAX_EU_RATIO)

           val itemOutputs = ObjectArrayList<Content?>()
           val fluidOutputs = ObjectArrayList<Content?>()

           val euMultiplier = this.euMultiplier
           var totalEu = BigInteger.ZERO
           var remain = this.parallel.maxParallel * multipleThreads.toLong()

           for (match in recipes) {
               if (match == null) continue
               if (remain <= 0) break
               val p = getMaxParallel(match, remain)
               if (p <= 0) continue

               var parallelEUt = BigInteger.valueOf(RecipeHelper.getInputEUt(match))
               var tempRecipe = if (p > 1) run {
                   parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p))
                   match.copy(ContentModifier.multiplier(p.toDouble()), false)
               } else match
               (tempRecipe as IGTRecipe).realParallels = p

               val tempTotalEu = totalEu.add(BigDecimal.valueOf(getTotalEuOfRecipe(tempRecipe) * euMultiplier).multiply(BigDecimal(parallelEUt)).toBigInteger())
               if (tempTotalEu > maxTotalEu) break
               else totalEu = tempTotalEu

               tempRecipe = modifyInputAndOutput(tempRecipe)
               remain -= p
               if (RecipeRunnerHelper.handleRecipeInput(machine, tempRecipe)) {
                   tempRecipe.outputs[ItemRecipeCapability.CAP]?.let { itemOutputs.addAll(it) }
                   tempRecipe.outputs[FluidRecipeCapability.CAP]?.let { fluidOutputs.addAll(it) }
               }
           }

           if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
               if (totalEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN)
               return null
           }

           val minDuration = limited.limitedDuration
           val eut = totalEu.divide(BigInteger.valueOf(minDuration.toLong())).negate()
           return buildWirelessRecipe(itemOutputs, fluidOutputs, minDuration, eut)
        }

        override fun modifyInputAndOutput(recipe: GTRecipe): GTRecipe {
            return modifyChance(recipe)
        }

        override fun getMaxParallel(recipe: GTRecipe, limit: Long): Long {
            return ChanceParallelLogic.getMaxParallel(
                getMachine(),
                modifyChance(recipe),
                limit,
                chanceCaches,
                recipe.type.chanceFunction,
                (recipe as IGTRecipe).euTier,
                getMachine().tier
            )
        }

        // Disable
        override fun buildFinalRecipe(parallelData: ParallelData?): GTRecipe? {
            return null
        }

        // Disable
        override fun calculateParallels(): ParallelData? {
            return null
        }

        companion object {
            private const val MAX_THREADS: Long = 1024
            private const val INPUT_CHANCE_RATIO = 10

            private fun modifyContents(before: Map<RecipeCapability<*>, MutableList<Content>>, isInput: Boolean): Reference2ObjectOpenHashMap<RecipeCapability<*>, MutableList<Content>> {
                val after = Reference2ObjectOpenHashMap<RecipeCapability<*>, MutableList<Content>>()
                for (entry in before) {
                    val cap = entry.key
                    val contentList = after.computeIfAbsent(
                        cap,
                        Reference2ObjectFunction { c: Any -> ObjectArrayList() })
                    for (cont in entry.value) {
                        if (cont.chance >= cont.maxChance) contentList.add(cont)
                        else {
                            val copy = cont.copy(cap, null)
                            copy.chance = if(isInput) cont.chance / INPUT_CHANCE_RATIO else cont.maxChance
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
                (copy as IGTRecipe).realParallels = (recipe as IGTRecipe).realParallels
                copy.ocTier = recipe.ocTier
                return copy
            }
        }
    }
}