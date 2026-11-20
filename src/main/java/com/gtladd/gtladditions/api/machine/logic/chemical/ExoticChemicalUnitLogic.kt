package com.gtladd.gtladditions.api.machine.logic.chemical

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.recipe.RecipeCalculationHelper
import com.lowdragmc.lowdraglib.syncdata.accessor.IManagedAccessor
import it.unimi.dsi.fastutil.longs.LongBooleanPair
import net.minecraft.nbt.CompoundTag
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeResult

internal class ExoticChemicalUnitLogic(
    val coordinator: ExoticChemicalRecipeLogic,
    val id: Int,
    val unitMachine: ExoticChemicalUnitMachine
) : GTLAddMultipleWirelessRecipesLogic(unitMachine) {

    var independent: Boolean = false
    var releaseAfterFinish: Boolean = false
    var suspendAfterFinish: Boolean = false
        private set
    var limitAll: Boolean = false

    val currentRecipe: GTRecipe?
        get() = if (duration > 0) lastRecipe else null

    val usedLimit: Long
        get() = currentRecipe?.data?.getLong("usedLimit") ?: 0L

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
        val finishedLimit = usedLimit
        super.onRecipeFinish()
        coordinator.onUnitFinished(this, finishedLimit)
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
        val limitedAtTierThree = structureTier >= 3 && id != 0 && limitAll
        val recipes = lookupRecipeIterator()

        if (structureTier >= 3 && !limitedAtTierThree) {
            return RecipeCalculationHelper.calculateParallelsWithProcessing(
                recipes,
                unitMachine,
                getParallelLimitForRecipe = { Long.MAX_VALUE },
                getMaxParallelForRecipe = { recipe, limit -> IParallelLogic.getParallel(unitMachine, recipe, limit) }
            )
        }

        val budget = if (structureTier <= 2) coordinator.availableParallel(id) else coordinator.parallelLimitOf(id)
        return RecipeCalculationHelper.calculateParallelsWithFairAllocation(recipes, budget) { recipe ->
            val budgeted = usesLimit(recipe) || limitedAtTierThree
            val upper = if (budgeted) budget else Long.MAX_VALUE
            LongBooleanPair.of(IParallelLogic.getParallel(unitMachine, recipe, upper), budgeted)
        }
    }

    private fun usesLimit(recipe: GTRecipe): Boolean = when (coordinator.host.structureTier) {
        1 -> true
        2 -> id != 0 && limitAll || IGTRecipe.of(recipe).euTier >= GTValues.UV
        else -> false
    }

    // ========================================
    // RECIPE EXECUTION
    // ========================================

    override fun checkBeforeWorking(): Boolean = (id == 0 || coordinator.host.structureTier >= 2) && super.checkBeforeWorking()

    override fun buildFinalWirelessRecipe(
        parallelData: ParallelData,
        wirelessTrait: IWirelessNetworkEnergyHandler
    ): WirelessGTRecipe? {
        if (!wirelessTrait.isOnline) return null

        var usedLimit = 0L
        val (itemOutputs, fluidOutputs, totalEu) = RecipeCalculationHelper.processParallelDataWireless(
            parallelData,
            unitMachine,
            wirelessTrait.maxAvailableEnergy,
            euMultiplier,
            ::getWirelessRecipeEut,
            onStarted = { origin, count ->
                if (usesLimit(origin)) usedLimit += count
            }
        )

        if (!RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs)) {
            if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(unitMachine, RecipeResult.FAIL_FIND)
            return null
        }

        return buildWirelessRecipe(itemOutputs, fluidOutputs, totalEu).also {
            it.data.putLong("usedLimit", usedLimit)
        }
    }

    // ========================================
    // UNIT STATE CONTROLS
    // ========================================

    fun setSuspendAfterFinish(enabled: Boolean) {
        suspendAfterFinish = enabled && currentRecipe != null && !isSuspend
        if (enabled && currentRecipe == null) isWorkingEnabled = false
    }

    fun releaseToShared() {
        independent = false
        releaseAfterFinish = false
        suspendAfterFinish = false
        coordinator.clearParallelLimit(id)
        limitAll = false

        unitMachine.clear()
        resetRecipeLogic()
        recipeStatus = null
        workingStatus = null
    }

    // ========================================
    // PERSISTENCE
    // ========================================

    fun save(): CompoundTag = CompoundTag().apply {
        putInt("id", this@ExoticChemicalUnitLogic.id)
        putBoolean("independent", independent)
        putBoolean("releaseAfterFinish", releaseAfterFinish)
        putBoolean("suspendAfterFinish", suspendAfterFinish)
        putBoolean("limitAll", limitAll)

        /*
         * Units sit outside LDLib's @Persisted scan and host.traits.
         * Replay RecipeLogic's managed fields and custom data (chance_cache) into an isolated per-unit sub-tag.
         */
        val state = IManagedAccessor.readManagedFields(this@ExoticChemicalUnitLogic, CompoundTag())
        saveCustomPersistedData(state, false)
        put("recipeLogic", state)
    }

    fun load(tag: CompoundTag) {
        independent = tag.getBoolean("independent")
        releaseAfterFinish = tag.getBoolean("releaseAfterFinish")
        suspendAfterFinish = tag.getBoolean("suspendAfterFinish")
        limitAll = tag.getBoolean("limitAll")

        val state = tag.getCompound("recipeLogic")
        IManagedAccessor.writePersistedFields(state, syncStorage.persistedFields)
        loadCustomPersistedData(state)

        if (releaseAfterFinish && currentRecipe == null) releaseToShared()
    }
}