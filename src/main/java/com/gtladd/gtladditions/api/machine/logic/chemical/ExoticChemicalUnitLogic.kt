package com.gtladd.gtladditions.api.machine.logic.chemical

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.LARGE_CHEMICAL_RECIPES
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex.Companion.TSD_COST_COEFFICIENT
import com.gtladd.gtladditions.utils.recipe.RecipeCalculationHelper
import com.lowdragmc.lowdraglib.syncdata.accessor.IManagedAccessor
import it.unimi.dsi.fastutil.longs.LongBooleanPair
import net.minecraft.nbt.CompoundTag
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.DISTORT_RECIPES
import org.gtlcore.gtlcore.utils.datastructure.Int128

internal class ExoticChemicalUnitLogic(
    val coordinator: ExoticChemicalRecipeLogic,
    val id: Int,
    val unitMachine: ExoticChemicalUnitMachine
) : GTLAddMultipleWirelessRecipesLogic(unitMachine) {

    var suspendAfterFinish: Boolean = false
        private set
    var limitAll: Boolean = false
    var customLimit: Long = Long.MAX_VALUE
    private var distortRecipes = false

    val recipeType: GTRecipeType
        get() = if (coordinator.host.structureTier >= 3 && distortRecipes) DISTORT_RECIPES else LARGE_CHEMICAL_RECIPES

    val parallelLimit: Long
        get() = if (coordinator.host.structureTier >= 3) customLimit else minOf(customLimit, coordinator.capacity)

    val currentRecipe: GTRecipe?
        get() = if (duration > 0) lastRecipe else null

    val isRecipeLogicAvailable: Boolean
        get() = unitMachine.isRecipeLogicAvailable()

    init {
        unitMachine.unitLogic = this
    }

    // ========================================
    // LIFE CYCLE
    // ========================================

    override fun updateTickSubscription() {
        if (coordinator.globalPaused) {
            subscription?.unsubscribe()
            subscription = null
        } else {
            super.updateTickSubscription()
        }
    }

    override fun serverTick() {
        if (coordinator.globalPaused) return

        val previous = currentRecipe
        val beforeProgress = progress
        val beforeStatus = status

        super.serverTick()

        val recipeOrStatusChanged = currentRecipe !== previous || status != beforeStatus
        // Unit progress is saved by the coordinator, outside the host's managed field scan.
        if (recipeOrStatusChanged || progress != beforeProgress) coordinator.host.markDirty()
        if (recipeOrStatusChanged) coordinator.updateStatus()
    }

    override fun onRecipeFinish() {
        super.onRecipeFinish()
        if (!coordinator.isUnitUnlocked(id)) {
            unitMachine.clear()
            updateTickSubscription()
        }
    }

    override fun handleRecipeWorking() {
        super.handleRecipeWorking()
        workingStatus = if (isWaiting) RecipeResult.FAIL_NO_ENOUGH_EU_IN else null
    }

    override fun setLock(lock: Boolean) = super.setLock(false)

    override fun setLockRecipe(recipe: GTRecipe?) = super.setLockRecipe(null)

    // ========================================
    // PARALLEL ALLOCATION
    // ========================================

    override fun calculateParallels(): ParallelData? {
        val structureTier = coordinator.host.structureTier
        val recipes = lookupRecipeIterator()

        if (structureTier >= 3 && (id == 0 || !limitAll)) {
            return RecipeCalculationHelper.calculateParallelsWithProcessing(
                recipes,
                unitMachine,
                getParallelLimitForRecipe = { Long.MAX_VALUE },
                getMaxParallelForRecipe = { recipe, limit -> IParallelLogic.getParallel(unitMachine, recipe, limit) }
            )
        }

        val budget = parallelLimit
        return RecipeCalculationHelper.calculateParallelsWithFairAllocation(recipes, budget) { recipe ->
            val budgeted = structureTier != 2 || id != 0 && limitAll || IGTRecipe.of(recipe).euTier >= GTValues.UV
            val upper = if (budgeted) budget else Long.MAX_VALUE
            LongBooleanPair.of(IParallelLogic.getParallel(unitMachine, recipe, upper), budgeted)
        }
    }

    // ========================================
    // RECIPE EXECUTION
    // ========================================

    override fun checkBeforeWorking(): Boolean = coordinator.isUnitUnlocked(id) && super.checkBeforeWorking()

    override fun checkRecipe(recipe: GTRecipe): Boolean {
        if (!super.checkRecipe(recipe)) return false
        if (coordinator.host.coilType.coilTemperature >= recipe.data.getInt("ebf_temp")) return true

        RecipeResult.of(unitMachine, RecipeResult.FAIL_NO_ENOUGH_TEMPERATURE)
        return false
    }

    override fun buildFinalWirelessRecipe(
        parallelData: ParallelData,
        wirelessTrait: IWirelessNetworkEnergyHandler
    ): WirelessGTRecipe? {
        if (!wirelessTrait.isOnline) return null

        val startedParallels = if (recipeType === DISTORT_RECIPES) Int128.ZERO() else null
        val (itemOutputs, fluidOutputs, totalEu) = RecipeCalculationHelper.processParallelDataWireless(
            parallelData,
            unitMachine,
            wirelessTrait.maxAvailableEnergy,
            euMultiplier,
            ::getWirelessRecipeEut,
            onStarted = { _, count -> startedParallels?.add(count) }
        )

        if (!RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs)) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(unitMachine, RecipeResult.FAIL_FIND)
            return null
        }

        if (startedParallels != null) {
            // The unlimited path has already consumed inputs for every entry in ParallelData.
            if (!parallelData.shouldProcess) {
                for (count in parallelData.parallels) {
                    startedParallels.add(count)
                }
            }

            val multiplier = coordinator.host.resolveTSD()
                ?.tryConsumeAndGetOutputMultiplier(startedParallels, TSD_COST_COEFFICIENT, false) ?: 1.0

            if (multiplier > 1.0) {
                val modifier = ContentModifier.multiplier(multiplier)
                itemOutputs.replaceAll { RecipeCalculationHelper.copyFixBoost(it, ItemRecipeCapability.CAP, modifier) }
                fluidOutputs.replaceAll { RecipeCalculationHelper.copyFixBoost(it, FluidRecipeCapability.CAP, modifier) }
            }
        }

        return buildWirelessRecipe(itemOutputs, fluidOutputs, totalEu)
    }

    // ========================================
    // UNIT STATE CONTROLS
    // ========================================

    fun setSuspendAfterFinish(enabled: Boolean) {
        suspendAfterFinish = enabled && currentRecipe != null && !isSuspend
        if (enabled && currentRecipe == null) isWorkingEnabled = false
    }

    fun setDistortRecipes(enabled: Boolean) {
        if (distortRecipes == enabled) return

        distortRecipes = enabled
        unitMachine.clearRecipeCache()
        if (isIdle) updateTickSubscription()
    }

    // ========================================
    // PERSISTENCE
    // ========================================

    fun save(): CompoundTag = CompoundTag().apply {
        putInt("id", this@ExoticChemicalUnitLogic.id)
        putBoolean("distortRecipes", distortRecipes)
        putBoolean("suspendAfterFinish", suspendAfterFinish)
        putBoolean("limitAll", limitAll)
        putLong("customLimit", customLimit)

        /*
         * Units sit outside LDLib's @Persisted scan and host.traits.
         * Replay RecipeLogic's managed fields and custom data (chance_cache) into an isolated per-unit sub-tag.
         */
        val state = IManagedAccessor.readManagedFields(this@ExoticChemicalUnitLogic, CompoundTag())
        saveCustomPersistedData(state, false)
        put("recipeLogic", state)
    }

    fun load(tag: CompoundTag) {
        distortRecipes = tag.getBoolean("distortRecipes")
        suspendAfterFinish = tag.getBoolean("suspendAfterFinish")
        limitAll = tag.getBoolean("limitAll")
        customLimit = tag.getLong("customLimit")

        val state = tag.getCompound("recipeLogic")
        IManagedAccessor.writePersistedFields(state, syncStorage.persistedFields)
        loadCustomPersistedData(state)
    }
}