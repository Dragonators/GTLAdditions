package com.gtladd.gtladditions.api.recipeslogic

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import kotlin.math.max

open class GTLAddMultipleRecipesLogic(protected val parallel: ParallelMachine) :
    RecipeLogic(parallel as IRecipeLogicMachine?), ILockRecipe {
    override fun getMachine(): WorkableElectricMultiblockMachine? {
        return super.getMachine() as WorkableElectricMultiblockMachine?
    }

    override fun findAndHandleRecipe() {
        lastRecipe = null
        val match = this.recipe
        if (match != null && RecipeRunnerHelper.matchRecipeOutput(machine, match)) {
            setupRecipe(match)
        }
    }

    protected val recipe: GTRecipe?
        get() {
            if (!machine.hasProxies()) return null
            val recipes = LookupRecipe()
            if (recipes == null) return null
            val length = recipes.size
            if (length == 0) return null
            var match = recipes[0]
            val recipe = buildEmptyRecipe()
            recipe.outputs.put(
                ItemRecipeCapability.CAP,
                ArrayList<Content?>()
            )
            recipe.outputs.put(
                FluidRecipeCapability.CAP,
                ArrayList<Content?>()
            )
            val maxEUt = getMachine()!!.overclockVoltage
            var totalEu: Long = 0
            val parallel = this.parallel.maxParallel
            for (i in 0..63) {
                if (checkRecipe(match)) {
                    match = recipes[(i + 1) % length]
                    continue
                }
                match = parallelRecipe(match, parallel)
                val input = buildEmptyRecipe()
                input.inputs.putAll(match.inputs)
                input.setId(match.id)
                if (RecipeRunnerHelper.handleRecipeInput(machine, input)) {
                    totalEu += match.duration * RecipeHelper.getInputEUt(match)
                    if (totalEu > maxEUt) break
                    val item =
                        match.outputs[ItemRecipeCapability.CAP]
                    if (item != null) recipe.outputs[ItemRecipeCapability.CAP]!!.addAll(item)
                    val fluid =
                        match.outputs[FluidRecipeCapability.CAP]
                    if (fluid != null) recipe.outputs[FluidRecipeCapability.CAP]!!.addAll(fluid)
                }
                match = recipes[(i + 1) % length]
            }
            if (recipe.outputs[ItemRecipeCapability.CAP] == ArrayList<Any?>() && recipe.outputs[FluidRecipeCapability.CAP] == ArrayList<Any?>()
            ) return null
            val d = totalEu.toDouble() / maxEUt
            val eut = if (d > 20) maxEUt else (maxEUt * d / 20).toLong()
            recipe.tickInputs.put(
                EURecipeCapability.CAP,
                listOf<Content?>(
                    Content(
                        eut,
                        ChanceLogic.getMaxChancedValue(),
                        ChanceLogic.getMaxChancedValue(),
                        0,
                        null,
                        null
                    )
                )
            )
            recipe.duration = max(d, 20.0).toInt()
            return recipe
        }

    protected fun LookupRecipe(): Array<GTRecipe>? {
        val recipeSet = this.machine.recipeType.lookup.findRecipeCollisions(this.machine)
        if (this.isLock) {
            if (this.lockRecipe == null) {
                this.lockRecipe = if (recipeSet != null && !recipeSet.isEmpty()) recipeSet.iterator().next() else null
            } else if (!RecipeRunnerHelper.matchRecipeInput(this.machine, this.lockRecipe)) {
                return null
            }
            return arrayOf(this.lockRecipe)
        }
        return (if (recipeSet != null && !recipeSet.isEmpty()) recipeSet.toTypedArray<GTRecipe?>() else null) as Array<GTRecipe>?
    }

    protected fun buildEmptyRecipe(): GTRecipe {
        return GTRecipeBuilder.ofRaw().buildRawRecipe()
    }

    protected fun parallelRecipe(recipe: GTRecipe, max: Int): GTRecipe {
        var recipe = recipe
        var maxMultipliers = Int.Companion.MAX_VALUE
        for (cap in recipe.inputs.keys) {
            if (cap.doMatchInRecipe()) {
                val currentMultiplier = cap.getMaxParallelRatio(machine, recipe, max)
                if (currentMultiplier < maxMultipliers) maxMultipliers = currentMultiplier
            }
        }
        if (maxMultipliers > 0) recipe = recipe.copy(ContentModifier.multiplier(maxMultipliers.toDouble()), false)
        return recipe
    }

    override fun onRecipeFinish() {
        machine.afterWorking()
        if (lastRecipe != null) {
            lastRecipe!!.postWorking(this.machine)
            RecipeRunnerHelper.handleRecipeOutput(this.machine, lastRecipe)
        }
        val match = this.recipe
        if (match != null && RecipeRunnerHelper.matchRecipeOutput(machine, match)) {
            setupRecipe(match)
        }
        status = Status.IDLE
        progress = 0
        duration = 0
    }

    private fun checkRecipe(recipe: GTRecipe): Boolean {
        val eut = RecipeHelper.getRecipeEUtTier(recipe) > getMachine()!!.getTier()
        val ebf_temp = machine is CoilWorkableElectricMultiblockMachine &&
                (machine as CoilWorkableElectricMultiblockMachine).coilType.coilTemperature < recipe.data.getInt("ebf_temp")
        return eut || ebf_temp
    }
}
