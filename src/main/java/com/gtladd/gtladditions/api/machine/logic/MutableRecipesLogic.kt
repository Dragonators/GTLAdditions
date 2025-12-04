package com.gtladd.gtladditions.api.machine.logic

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.longs.LongLongPair
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.function.BiPredicate

open class MutableRecipesLogic<T> : RecipeLogic, ILockRecipe, IWirelessRecipeLogic, IRecipeStatus
        where T : WorkableElectricMultiblockMachine,
              T : IRecipeLogicMachine,
              T : IWirelessElectricMultiblockMachine,
              T : IThreadModifierMachine,
              T : ParallelMachine {

    private var useMultipleRecipes = false
    private val reductionRatio: Double
    protected val recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>?

    constructor(machine: T) : this(machine, null, 1.0)

    constructor(machine: T, reductionRatio: Double) : this(machine, null, reductionRatio)

    constructor(
        machine: T,
        recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>?
    ) : this(machine, recipeCheck, 1.0)

    constructor(
        machine: T,
        recipeCheck: BiPredicate<GTRecipe, IRecipeLogicMachine>?,
        reductionRatio: Double
    ) : super(machine) {
        this.recipeCheck = recipeCheck
        this.reductionRatio = reductionRatio
    }

    @Suppress("UNCHECKED_CAST")
    override fun getMachine(): T {
        return super.getMachine() as T
    }

    override fun findAndHandleRecipe() {
        if (useMultipleRecipes) findAndHandleMultipleRecipe()
        else super.findAndHandleRecipe()
    }

    override fun onRecipeFinish() {
        if (useMultipleRecipes) onMultipleRecipeFinish()
        else super.onRecipeFinish()
    }

    override fun handleRecipeWorking() {
        if (useMultipleRecipes) handleMultipleRecipeWorking()
        else super.handleRecipeWorking()
    }

    // ========================================
    // Multiple Logic
    // ========================================

    protected open fun findAndHandleMultipleRecipe() {
        lastRecipe = null
        recipeStatus = null
        val match = getRecipe()
        if (match != null) {
            if (RecipeRunnerHelper.matchRecipeOutput(machine, match)) {
                setupRecipe(match)
            }
        }
    }

    protected open fun onMultipleRecipeFinish() {
        machine.afterWorking()
        lastRecipe?.let { RecipeRunnerHelper.handleRecipeOutput(this.machine, it) }
        val match = getRecipe()
        if (match != null) {
            if (RecipeRunnerHelper.matchRecipeOutput(machine, match)) {
                setupRecipe(match)
                return
            }
        }
        status = Status.IDLE
        progress = 0
        duration = 0
    }

    protected open fun handleMultipleRecipeWorking() {
        assert(this.lastRecipe != null)

        val success = if (this.lastRecipe is IWirelessGTRecipe) {
            handleWirelessTickInput(this.lastRecipe as IWirelessGTRecipe)
        } else {
            this.handleTickRecipe(this.lastRecipe).isSuccess
        }

        if (success) {
            this.status = Status.WORKING
            if (!this.machine.onWorking()) {
                this.interruptRecipe()
                return
            }
            ++this.progress
            ++this.totalContinuousRunningTime
        } else {
            this.setWaiting(RecipeResult.FAIL_NO_ENOUGH_EU_IN.reason())
        }

        if (this.status == Status.WAITING) {
            this.doDamping()
        }
    }

    protected open fun getRecipe(): GTRecipe? {
        val wirelessTrait = getMachine().getWirelessNetworkEnergyHandler()
        return if (wirelessTrait != null) getWirelessRecipe(wirelessTrait) else getNormalRecipe()
    }

    private fun getNormalRecipe(): GTRecipe? {
        if (!machine.hasProxies()) return null

        val maxEUt = getMachine().overclockVoltage
        if (maxEUt <= 0) return null

        val iterator = lookupRecipeIterator()
        val euMultiplier = euMultiplier
        val itemOutputs = ObjectArrayList<Content>()
        val fluidOutputs = ObjectArrayList<Content>()

        var totalEu = 0.0
        var remain = getMachine().maxParallel.toLong() * getMultipleThreads()
        while (remain > 0 && iterator.hasNext()) {
            val match = iterator.next()
            val pair = calculateParallel(machine, match, remain)
            val p = pair.firstLong()
            if (p <= 0) continue

            var paralleledRecipe = RecipeCalculationHelper.multipleRecipe(match, p)
            paralleledRecipe = IParallelLogic.getRecipeOutputChance(machine, paralleledRecipe)

            if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                remain -= pair.secondLong()
                totalEu += getRecipeEut(match).toDouble() * p * paralleledRecipe.duration * euMultiplier
                RecipeCalculationHelper.collectOutputs(paralleledRecipe, itemOutputs, fluidOutputs)
            }
            if (totalEu / maxEUt > 20 * 500) break
        }

        if (!RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs)) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
            return null
        }

        return RecipeCalculationHelper.buildNormalRecipe(itemOutputs, fluidOutputs, totalEu, maxEUt, 20)
    }

    private fun getWirelessRecipe(wirelessTrait: IWirelessNetworkEnergyHandler): WirelessGTRecipe? {
        if (!wirelessTrait.isOnline) return null

        val iterator = lookupRecipeIterator()
        val maxTotalEu = wirelessTrait.maxAvailableEnergy
        val euMultiplier = euMultiplier
        val itemOutputs = ObjectArrayList<Content>()
        val fluidOutputs = ObjectArrayList<Content>()

        var remain = getMachine().maxParallel.toLong() * getMultipleThreads()
        var totalEu = BigInteger.ZERO

        while (remain > 0 && iterator.hasNext()) {
            val match = iterator.next()
            val pair = calculateParallel(machine, match, remain)
            val p = pair.firstLong()
            if (p <= 0) continue

            var parallelEUt = BigInteger.valueOf(getRecipeEut(match))
            var paralleledRecipe = RecipeCalculationHelper.multipleRecipe(match, p)
            if (p > 1) parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p))

            val tempTotalEu = totalEu.add(
                BigDecimal.valueOf(paralleledRecipe.duration * euMultiplier)
                    .multiply(BigDecimal(parallelEUt)).toBigInteger()
            )
            if (tempTotalEu > maxTotalEu) {
                if (totalEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN)
                break
            }

            paralleledRecipe = IParallelLogic.getRecipeOutputChance(machine, paralleledRecipe)
            if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                remain -= pair.secondLong()
                totalEu = tempTotalEu
                RecipeCalculationHelper.collectOutputs(paralleledRecipe, itemOutputs, fluidOutputs)
            }
        }

        if (!RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs)) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
            return null
        }

        return RecipeCalculationHelper.buildWirelessRecipe(itemOutputs, fluidOutputs, 20, totalEu)
    }

    protected open fun calculateParallel(
        machine: IRecipeLogicMachine,
        match: GTRecipe,
        remain: Long
    ): LongLongPair {
        val p = IParallelLogic.getMaxParallel(machine, match, remain)
        return LongLongPair.of(p, p)
    }

    open fun getMultipleThreads(): Int {
        return if (getMachine().getAdditionalThread() > 0) getMachine().getAdditionalThread() else 1
    }

    protected open val euMultiplier: Double
        get() {
            val maintenanceMachine = (machine as IRecipeCapabilityMachine).maintenanceMachine
            return if (maintenanceMachine != null) maintenanceMachine.durationMultiplier * this.reductionRatio else this.reductionRatio
        }

    protected open fun getRecipeEut(recipe: GTRecipe): Long {
        return RecipeHelper.getInputEUt(recipe)
    }

    protected open fun lookupRecipeIterator(): Iterator<GTRecipe> {
        return if (isLock) {
            when {
                lockRecipe == null -> {
                    lockRecipe = machine.recipeType.lookup.find(machine, this::checkRecipe)
                    lockRecipe?.let { Collections.singleton(it).iterator() } ?: Collections.emptyIterator()
                }
                checkRecipe(lockRecipe) -> Collections.singleton(lockRecipe).iterator()
                else -> Collections.emptyIterator()
            }
        } else {
            machine.recipeType.lookup.getRecipeIterator(machine, this::checkRecipe)
        }
    }

    protected open fun checkRecipe(recipe: GTRecipe): Boolean {
        return RecipeRunnerHelper.matchRecipe(machine, recipe) &&
                IGTRecipe.of(recipe).euTier <= getMachine().tier &&
                recipe.checkConditions(this).isSuccess &&
                (recipeCheck == null || recipeCheck.test(recipe, machine))
    }

    override fun getWirelessMachine(): IWirelessElectricMultiblockMachine {
        return machine as IWirelessElectricMultiblockMachine
    }

    fun setUseMultipleRecipes(useMultipleRecipes: Boolean) {
        this.useMultipleRecipes = useMultipleRecipes
    }

    fun isMultipleRecipeMode(): Boolean = this.useMultipleRecipes
}