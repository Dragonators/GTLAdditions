package com.gtladd.gtladditions.api.machine.logic.chemical

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic
import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex
import com.gtladd.gtladditions.mixin.gtceu.api.machine.trait.RecipeLogicAccessor
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine

class ExoticChemicalRecipeLogic(internal val host: ExoticChemicalComplex) : GTLAddMultipleWirelessRecipesLogic(host) {

    internal val units: Array<ExoticChemicalUnitLogic> = Array(13) {
        ExoticChemicalUnitLogic(this, it, ExoticChemicalUnitMachine(host))
    }
    internal var globalPaused: Boolean = false
        private set

    private var unitParts: Array<List<IMultiPart>>? = null

    internal val capacity: Long
        get() = getMultipleThreads().toLong() * host.maxParallel

    internal val allSuspendAfterFinish: Boolean
        get() = units.any { isUnitActive(it) && it.suspendAfterFinish } &&
            units.all { !isUnitActive(it) || it.isSuspend || it.suspendAfterFinish }

    // ========================================
    // UNIT PARTS & BINDINGS
    // ========================================

    internal fun bind(unitParts: Array<List<IMultiPart>>) {
        this.unitParts = unitParts
        setReduction(if (host.structureTier >= 2) 0.8 else 1.0, if (host.structureTier >= 2) 0.6 else 1.0)
        units.forEach { it.setReduction(reductionEUt, reductionDuration) }
        refreshUnits()
    }

    internal fun partsOf(unitId: Int): List<IMultiPart>? = unitParts?.get(unitId)

    internal fun clearUnitParts() {
        unitParts = null
        for (unitLogic in units) {
            unitLogic.unitMachine.clear()
            unitLogic.updateTickSubscription()
        }
    }

    internal fun refreshUnits() {
        val unitParts = unitParts ?: return
        val distinct = (host as IRecipeCapabilityMachine).isDistinct

        for (unitLogic in units) {
            if (isUnitActive(unitLogic)) {
                unitLogic.unitMachine.refresh(unitParts[unitLogic.id], distinct)
            } else {
                unitLogic.unitMachine.clear()
            }
            unitLogic.updateTickSubscription()
        }

        updateStatus()
    }

    internal fun sortOutputs() {
        for (unitLogic in units) {
            if (unitLogic.unitMachine.isOnline) unitLogic.unitMachine.sortMEOutput()
        }
        wakeUnits()
    }

    internal fun unbind() {
        clearUnitParts()
        updateStatus()
    }

    /**
     * Called by `WorkableMultiblockMachine.onUnload()` on chunk unload and block removal.
     *
     * - Forwards [inValid] to all 13 units because they are excluded from `host.traits`.
     * - Calls [unbind] to release hatch listeners stored in `unitMachine.subscriptions` outside `host.traitSubscriptions`.
     */
    override fun inValid() {
        units.forEach(ExoticChemicalUnitLogic::inValid)
        unbind()
        super.inValid()
    }

    // ========================================
    // SCHEDULING & CAPACITY
    // ========================================

    override fun getMultipleThreads(): Int = Ints.saturatedCast(256L + host.getAdditionalThread())

    internal fun isUnitUnlocked(id: Int): Boolean = id == 0 || host.structureTier >= 2

    // Keep the ports of a downgraded unit until it delivers its last batch.
    private fun isUnitActive(unitLogic: ExoticChemicalUnitLogic): Boolean =
        isUnitUnlocked(unitLogic.id) || unitLogic.currentRecipe != null

    private fun wakeUnits() {
        for (unitLogic in units) {
            if (unitLogic.unitMachine.isOnline) unitLogic.updateTickSubscription()
        }
    }

    // ========================================
    // CONFIGURATION & CONTROLS
    // ========================================

    fun configure(unitId: Int, edit: String, number: Long, check: Boolean): Boolean {
        if (number < 0 || unitId !in units.indices || host.level !is ServerLevel) return false

        val unitLogic = units[unitId]
        when (edit) {
            "limit", "limit_all" -> {
                if (unitId == 0 || !isUnitUnlocked(unitId) || !host.isRecipeLogicAvailable) return false

                if (edit == "limit") {
                    if (number < 1L) return false
                    val clamped = if (host.structureTier == 2) minOf(number, capacity) else number
                    val previous = unitLogic.parallelLimit
                    unitLogic.customLimit = if (host.structureTier == 2 && clamped == capacity) Long.MAX_VALUE else clamped
                    if (unitLogic.parallelLimit > previous && unitLogic.isIdle) unitLogic.updateTickSubscription()
                } else if (unitLogic.limitAll != check) {
                    if (check && host.structureTier >= 3 && unitLogic.customLimit == Long.MAX_VALUE) {
                        unitLogic.customLimit = capacity
                    }
                    unitLogic.limitAll = check
                    if (!check && unitLogic.isIdle) unitLogic.updateTickSubscription()
                }
            }

            "recipe_type" -> {
                if (host.structureTier < 3 || !host.isRecipeLogicAvailable) return false
                unitLogic.setDistortRecipes(check)
            }

            "pause" -> {
                if (!isUnitUnlocked(unitId)) return false
                unitLogic.isWorkingEnabled = !check
                unitLogic.setSuspendAfterFinish(false)
            }

            "suspend_after_finish" -> {
                if (!isUnitUnlocked(unitId)) return false
                unitLogic.setSuspendAfterFinish(check)
            }

            "suspend_after_finish_all" -> {
                setAllSuspendAfterFinish(check)
                return true
            }

            else -> return false
        }

        configChanged()
        return true
    }

    internal fun setAllSuspendAfterFinish(enabled: Boolean) {
        for (unitLogic in units) {
            if (isUnitActive(unitLogic)) unitLogic.setSuspendAfterFinish(enabled)
        }
        configChanged()
    }

    override fun setLock(lock: Boolean) = super.setLock(false)

    override fun setLockRecipe(recipe: GTRecipe?) = super.setLockRecipe(null)

    override fun setWorkingEnabled(allowed: Boolean) {
        val resumeAll = allowed && !globalPaused && isSuspend
        globalPaused = !allowed
        if (resumeAll) {
            for (unitLogic in units) {
                if (isUnitActive(unitLogic)) unitLogic.isWorkingEnabled = true
            }
        }

        wakeUnits()
        configChanged()
    }

    private fun configChanged() {
        updateStatus()
        host.markDirty()
    }

    // ========================================
    // STATUS & RECIPE LIFECYCLE
    // ========================================

    internal fun updateStatus() {
        val next = when {
            globalPaused -> Status.SUSPEND
            units.any { it.isRecipeLogicAvailable && it.isWorking } -> Status.WORKING
            units.all { !isUnitActive(it) || it.isSuspend } -> Status.SUSPEND
            else -> Status.IDLE
        }

        val previous = status
        if (previous == next) return

        /*
         * Write the status field directly instead of calling RecipeLogic.setStatus:
         * setStatus also calls updateTickSubscription, which would wake unrelated idle units
         * whenever one unit changes the aggregated display status.
         */
        host.notifyStatusChanged(previous, next)
        (this as RecipeLogicAccessor).setStatusField(next)
    }

    override fun updateTickSubscription() = wakeUnits()

    override fun findAndHandleRecipe() = wakeUnits()

    override fun onRecipeFinish() = Unit

    override fun resetRecipeLogic() {
        super.resetRecipeLogic()

        for (unitLogic in units) {
            val paused = unitLogic.isSuspend || unitLogic.suspendAfterFinish
            unitLogic.resetRecipeLogic()
            unitLogic.workingStatus = null
            unitLogic.recipeStatus = null
            unitLogic.setSuspendAfterFinish(false)

            if (paused) unitLogic.isWorkingEnabled = false
        }

        updateStatus()
    }

    // ========================================
    // PERSISTENCE
    // ========================================

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        if (forDrop) return
        super.saveCustomPersistedData(tag, false)

        tag.put(
            "exoticChemicalUnits",
            CompoundTag().apply {
                putBoolean("globalPaused", globalPaused)
                put("units", ListTag().apply { units.forEach { add(it.save()) } })
            }
        )
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        if (!tag.contains("exoticChemicalUnits", Tag.TAG_COMPOUND.toInt())) return

        val data = tag.getCompound("exoticChemicalUnits")
        globalPaused = data.getBoolean("globalPaused")
        for (entry in data.getList("units", Tag.TAG_COMPOUND.toInt())) {
            val saved = entry as CompoundTag
            units.getOrNull(saved.getInt("id"))?.load(saved)
        }

        refreshUnits()
    }
}