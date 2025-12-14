package com.gtladd.gtladditions.api.machine.logic

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipeMachine
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic
import java.math.BigInteger
import java.util.*
import java.util.function.BiPredicate
import java.util.function.Predicate

open class GTLAddMultipleRecipesLogic : MultipleRecipesLogic {
    protected val limited: IGTLAddMultiRecipeMachine
    protected val recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>?
    protected val beforeWorking: Predicate<IRecipeLogicMachine>?

    constructor(parallel: GTLAddWorkableElectricMultipleRecipesMachine) : this(parallel, null, null)

    constructor(
        parallel: GTLAddWorkableElectricMultipleRecipesMachine,
        recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>?
    ) : this(parallel, recipeCheck, null)

    constructor(
        parallel: GTLAddWorkableElectricMultipleRecipesMachine,
        beforeWorking: Predicate<IRecipeLogicMachine>?
    ) : this(parallel, null, beforeWorking)

    constructor(
        parallel: GTLAddWorkableElectricMultipleRecipesMachine,
        recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>?,
        beforeWorking: Predicate<IRecipeLogicMachine>?
    ) : super(parallel) {
        this.limited = parallel
        this.recipeCheck = recipeCheck
        this.beforeWorking = beforeWorking
    }

    override fun findAndHandleRecipe() {
        lastRecipe = null
        recipeStatus = null
        val match = getGTRecipe()
        if (match != null && matchRecipeOutput(machine, match)) {
            setupRecipe(match)
        }
    }

    override fun onRecipeFinish() {
        machine.afterWorking()
        lastRecipe?.let { handleRecipeOutput(this.machine, it) }
        val match = getGTRecipe()
        if (match != null && matchRecipeOutput(machine, match)) {
            setupRecipe(match)
            return
        }
        status = Status.IDLE
        progress = 0
        duration = 0
    }

    override fun getMachine(): GTLAddWorkableElectricMultipleRecipesMachine {
        return super.getMachine() as GTLAddWorkableElectricMultipleRecipesMachine
    }

    open fun getMultipleThreads(): Int {
        return Ints.saturatedCast(MAX_THREADS + getMachine().getAdditionalThread())
    }

    protected open fun getGTRecipe(): GTRecipe? {
        if (!checkBeforeWorking()) return null

        val parallelData = calculateParallels() ?: return null

        val wirelessTrait = getMachine().getWirelessNetworkEnergyHandler()
        return if (wirelessTrait != null)
            buildFinalWirelessRecipe(parallelData, wirelessTrait)
        else
            buildFinalNormalRecipe(parallelData)
    }

    protected open fun calculateParallels(): ParallelData? {
        val recipes = lookupRecipeIterator()
        val totalParallel = parallel.maxParallel.toLong() * getMultipleThreads()

        return RecipeCalculationHelper.calculateParallelsWithFairAllocation(
            recipes, totalParallel
        ) { recipe -> getMaxParallel(recipe, totalParallel) }
    }

    protected open fun buildFinalNormalRecipe(parallelData: ParallelData): GTRecipe? {
        val maxEUt = getMachine().overclockVoltage
        val (itemOutputs, fluidOutputs, totalEu) = RecipeCalculationHelper.processParallelDataNormal(
            parallelData, machine, maxEUt, euMultiplier, { getTotalEuOfRecipe(it) }
        )

        if (!RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs)) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
            return null
        }

        return RecipeCalculationHelper.buildNormalRecipe(itemOutputs, fluidOutputs, totalEu, maxEUt, limited.getLimitedDuration())
    }

    protected open fun buildFinalWirelessRecipe(
        parallelData: ParallelData,
        wirelessTrait: IWirelessNetworkEnergyHandler
    ): WirelessGTRecipe? {
        if (!wirelessTrait.isOnline) return null

        val (itemOutputs, fluidOutputs, totalEu) = RecipeCalculationHelper.processParallelDataWireless(
            parallelData, machine, wirelessTrait.maxAvailableEnergy, euMultiplier, ::getWirelessRecipeEut, isEnergyConsumer()
        )

        if (isEnergyConsumer() && !RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs)) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
            return null
        }

        return buildWirelessRecipe(itemOutputs, fluidOutputs, totalEu)
    }

    protected open fun buildWirelessRecipe(
        itemOutputs: ObjectArrayList<Content>,
        fluidOutputs: ObjectArrayList<Content>,
        totalEu: BigInteger
    ): WirelessGTRecipe {
        return RecipeCalculationHelper.buildWirelessRecipe(
            itemOutputs,
            fluidOutputs,
            limited.getLimitedDuration(),
            totalEu
        )
    }

    protected open fun getWirelessRecipeEut(recipe: GTRecipe): Long = RecipeHelper.getInputEUt(recipe)

    protected open fun isEnergyConsumer(): Boolean = true

    protected open fun lookupRecipeIterator(): Set<GTRecipe> {
        return if (isLock) {
            when {
                lockRecipe == null -> {
                    lockRecipe = machine.recipeType.lookup.find(machine, this::checkRecipe)
                    lockRecipe?.let { Collections.singleton(it) } ?: emptySet()
                }
                checkRecipe(lockRecipe) -> Collections.singleton(lockRecipe)
                else -> emptySet()
            }
        } else {
            machine.recipeType.lookup.getRecipeIterator(machine, this::checkRecipe).asSequence()
                .toCollection(ObjectOpenHashSet())
        }
    }

    protected open fun checkRecipe(recipe: GTRecipe): Boolean {
        return matchRecipe(machine, recipe) &&
                IGTRecipe.of(recipe).euTier <= getMachine().tier &&
                recipe.checkConditions(machine.recipeLogic).isSuccess &&
                (recipeCheck == null || recipeCheck.test(recipe, machine))
    }

    protected open fun checkBeforeWorking(): Boolean {
        if (!machine.hasProxies()) return false
        if (getMachine().overclockVoltage <= 0) return false
        return beforeWorking == null || beforeWorking.test(machine)
    }

    protected open fun getMaxParallel(recipe: GTRecipe, limit: Long): Long {
        return IParallelLogic.getMaxParallel(this.machine, recipe, limit)
    }

    companion object {
        private const val MAX_THREADS = 128L
    }
}