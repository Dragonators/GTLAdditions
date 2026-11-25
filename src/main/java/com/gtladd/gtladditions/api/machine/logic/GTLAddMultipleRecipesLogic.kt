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
import com.gtladd.gtladditions.integration.gtlcore.monitor.BatchMonitorInputs
import com.gtladd.gtladditions.utils.recipe.RecipeCalculationHelper
import it.unimi.dsi.fastutil.longs.LongBooleanPair
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.nbt.ListTag
import org.gtlcore.gtlcore.api.machine.ISuspendableMachine
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic
import org.gtlcore.gtlcore.integration.monitor.MachineMonitorEntries
import java.math.BigInteger
import java.util.*
import java.util.function.BiPredicate
import java.util.function.Predicate

open class GTLAddMultipleRecipesLogic : MultipleRecipesLogic {
    private var batchMonitorInputs: BatchMonitorInputs? = null
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
    ) : this(parallel as IGTLAddMultiRecipeMachine, recipeCheck, beforeWorking)

    protected constructor(
        parallel: IGTLAddMultiRecipeMachine,
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
        pruneMonitorInputs()
    }

    override fun onRecipeFinish() {
        machine.afterWorking()
        lastRecipe?.let { handleRecipeOutput(this.machine, it) }

        val suspendableMachine = machine as? ISuspendableMachine
        if (suspendableMachine?.`gtlcore$isSuspendAfterFinish`() == true) {
            status = Status.SUSPEND
            suspendableMachine.`gtlcore$setSuspendAfterFinish`(false)
        } else {
            getGTRecipe()?.takeIf { matchRecipeOutput(machine, it) }?.let {
                setupRecipe(it)
                if (isWorking && progress == 0 && duration > 0) return
            }
            status = Status.IDLE
        }
        progress = 0
        duration = 0
        pruneMonitorInputs()
    }

    override fun getMachine(): GTLAddWorkableElectricMultipleRecipesMachine = super.getMachine() as GTLAddWorkableElectricMultipleRecipesMachine

    open fun getMultipleThreads(): Int = Ints.saturatedCast(MAX_THREADS + getMachine().getAdditionalThread())

    protected open fun getGTRecipe(): GTRecipe? {
        if (!checkBeforeWorking()) return null

        val parallelData = calculateParallels() ?: return null

        val wirelessTrait = getMachine().getWirelessNetworkEnergyHandler()
        val recipe = if (wirelessTrait != null) {
            buildFinalWirelessRecipe(parallelData, wirelessTrait)
        } else {
            buildFinalNormalRecipe(parallelData)
        }

        return recipe?.let { bindMonitorInputs(it, parallelData) }
    }

    protected fun bindMonitorInputs(recipe: GTRecipe, parallelData: ParallelData): GTRecipe {
        batchMonitorInputs = parallelData.monitorInputs.bind(recipe)
        return recipe
    }

    protected fun pruneMonitorInputs() {
        val pending = batchMonitorInputs ?: return
        val recipe = lastRecipe
        if (duration <= 0 || isIdle || recipe == null || !pending.matches(recipe)) batchMonitorInputs = null
    }

    override fun monitorInputs(): ListTag {
        pruneMonitorInputs()
        return batchMonitorInputs?.materializeCopy() ?: lastRecipe?.let(MachineMonitorEntries::inputs) ?: ListTag()
    }

    override fun setupRecipe(recipe: GTRecipe) {
        super.setupRecipe(recipe)
        pruneMonitorInputs()
    }

    override fun interruptRecipe() {
        batchMonitorInputs = null
        super.interruptRecipe()
    }

    override fun resetRecipeLogic() {
        batchMonitorInputs = null
        super.resetRecipeLogic()
    }

    override fun inValid() {
        batchMonitorInputs = null
        super.inValid()
    }

    protected open fun calculateParallels(): ParallelData? {
        val recipes = lookupRecipeIterator()
        val totalParallel = parallel.maxParallel.toLong() * getMultipleThreads()

        return RecipeCalculationHelper.calculateParallelsWithFairAllocation(
            recipes,
            totalParallel
        ) { recipe -> LongBooleanPair.of(getMaxParallel(recipe, totalParallel), true) }
    }

    protected open fun buildFinalNormalRecipe(parallelData: ParallelData): GTRecipe? {
        val maxEUt = getMachine().overclockVoltage
        val (itemOutputs, fluidOutputs, totalEu) = RecipeCalculationHelper.processParallelDataNormal(
            parallelData,
            machine,
            maxEUt,
            euMultiplier,
            { getTotalEuOfRecipe(it) }
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
            parallelData,
            machine,
            wirelessTrait.maxAvailableEnergy,
            euMultiplier,
            ::getWirelessRecipeEut,
            isEnergyConsumer()
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
    ): WirelessGTRecipe = RecipeCalculationHelper.buildWirelessRecipe(
        itemOutputs,
        fluidOutputs,
        limited.getLimitedDuration(),
        totalEu
    )

    protected open fun getWirelessRecipeEut(recipe: GTRecipe): Long = RecipeHelper.getInputEUt(recipe)

    protected open fun isEnergyConsumer(): Boolean = true

    protected open fun lookupRecipeIterator(): Set<GTRecipe> = if (isLock) {
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

    protected open fun checkRecipe(recipe: GTRecipe): Boolean = matchRecipe(machine, recipe) &&
        IGTRecipe.of(recipe).euTier <= getMachine().tier &&
        recipe.checkConditions(machine.recipeLogic).isSuccess &&
        (recipeCheck == null || recipeCheck.test(recipe, machine))

    protected open fun checkBeforeWorking(): Boolean {
        if (!machine.hasProxies()) return false
        if (getMachine().overclockVoltage <= 0) return false
        if (getMachine().getWirelessNetworkEnergyHandler()?.isOnline == false) return false
        return beforeWorking == null || beforeWorking.test(machine)
    }

    protected open fun getMaxParallel(recipe: GTRecipe, limit: Long): Long = IParallelLogic.getMaxParallel(this.machine, recipe, limit)

    companion object {
        private const val MAX_THREADS = 128L
    }
}
