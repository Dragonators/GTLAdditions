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
    private val parallelPool = ExoticChemicalParallelPool(units.size)

    internal var globalPaused: Boolean = false
        private set

    private var unitParts: Array<List<IMultiPart>>? = null

    internal val capacity: Long
        get() = getMultipleThreads().toLong() * host.maxParallel

    internal val allSuspendAfterFinish: Boolean
        get() = units.any { isUnitActive(it) && it.suspendAfterFinish } &&
            units.all { !isUnitActive(it) || it.isSuspend || it.suspendAfterFinish || it.releaseAfterFinish }

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

    // An independent unit keeps its unitMachine at structure tier I until its current batch finishes.
    internal fun isUnitActive(unitLogic: ExoticChemicalUnitLogic): Boolean =
        unitLogic.id == 0 || unitLogic.independent && (host.structureTier >= 2 || unitLogic.currentRecipe != null)

    private fun isIndependent(unitId: Int): Boolean = units[unitId].independent

    internal fun parallelLimitOf(unitId: Int): Long = parallelPool.limitOf(unitId)

    internal fun clearParallelLimit(unitId: Int) = parallelPool.setLimit(unitId, 0L)

    internal fun maxParallelLimit(unitId: Int): Long = parallelPool.maxLimit(unitId, host.structureTier, capacity, ::isIndependent)

    private fun shareOf(unitId: Int): Long = parallelPool.shareOf(unitId, host.structureTier, capacity, ::isIndependent)

    internal fun availableParallel(unitId: Int): Long = parallelPool.available(unitId, host.structureTier, capacity, ::isIndependent) { other ->
        units[other].usedLimit
    }

    internal fun wakeUnit(unitId: Int) = units[unitId].updateTickSubscription()

    internal fun wakeUnits(idleOnly: Boolean = false) {
        for (unitLogic in units) {
            if (unitLogic.unitMachine.isOnline && (!idleOnly || unitLogic.isIdle)) unitLogic.updateTickSubscription()
        }
    }

    // ========================================
    // CONFIGURATION & CONTROLS
    // ========================================

    fun configure(unitId: Int, edit: String, number: Long, check: Boolean): Boolean {
        if (number < 0 || unitId !in units.indices || host.level !is ServerLevel) return false
        val unitLogic = units[unitId]

        when (edit) {
            "independent", "limit" -> {
                if (unitId == 0 || !host.isRecipeLogicAvailable || host.structureTier < 2 || unitLogic.releaseAfterFinish) return false
                val activating = edit == "independent"
                if (activating && unitLogic.independent || !activating && !unitLogic.independent) return false
                val parts = if (activating) unitParts?.get(unitId) ?: return false else null

                val useDefaults = activating && number == 0L
                val limit = when {
                    !useDefaults -> minOf(number, maxParallelLimit(unitId))
                    host.structureTier == 2 -> minOf(host.maxParallel.toLong(), availableParallel(0))
                    else -> host.maxParallel.toLong()
                }
                val previousLimit = parallelLimitOf(unitId)
                val wasLimitAll = unitLogic.limitAll

                if (activating) unitLogic.independent = true
                parallelPool.setLimit(unitId, limit)
                unitLogic.limitAll = !useDefaults && check

                if (parts != null) {
                    unitLogic.unitMachine.refresh(parts, (host as IRecipeCapabilityMachine).isDistinct)
                    wakeUnit(unitId)
                } else if (host.structureTier == 2 && limit < previousLimit) {
                    wakeUnits(idleOnly = true)
                } else if (limit > previousLimit || wasLimitAll && !unitLogic.limitAll) {
                    wakeUnit(unitId)
                }
            }

            "share" -> {
                if (!unitLogic.independent || unitLogic.releaseAfterFinish) return false
                if (unitLogic.currentRecipe == null) {
                    unitLogic.releaseToShared()
                    wakeUnits(idleOnly = true)
                } else {
                    unitLogic.releaseAfterFinish = true
                    unitLogic.isWorkingEnabled = true
                }
            }

            "pause" -> {
                if (!isUnitActive(unitLogic)) return false
                unitLogic.isWorkingEnabled = !check
                unitLogic.setSuspendAfterFinish(false)
                if (!check) wakeUnit(unitId)
            }

            "suspend_after_finish" -> {
                if (!isUnitActive(unitLogic) || unitLogic.releaseAfterFinish) return false
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
        units.filter(::isUnitActive).forEach { it.setSuspendAfterFinish(enabled) }
        configChanged()
    }

    override fun setLock(lock: Boolean) = super.setLock(false)

    override fun setLockRecipe(recipe: GTRecipe?) = super.setLockRecipe(null)

    override fun setWorkingEnabled(allowed: Boolean) {
        val resumeAll = allowed && !globalPaused && isSuspend
        globalPaused = !allowed
        if (resumeAll) {
            units.filter(::isUnitActive).forEach { it.isWorkingEnabled = true }
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

    internal fun onUnitFinished(unitLogic: ExoticChemicalUnitLogic, usedLimit: Long) {
        val released = unitLogic.releaseAfterFinish && unitLogic.currentRecipe == null
        if (released) {
            unitLogic.releaseToShared()
        } else if (!isUnitActive(unitLogic)) {
            unitLogic.unitMachine.clear()
            unitLogic.updateTickSubscription()
        }

        if (host.structureTier in 1..2 && (released || usedLimit > shareOf(unitLogic.id))) {
            wakeUnits(idleOnly = true)
        }
    }

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

            if (unitLogic.releaseAfterFinish) {
                unitLogic.releaseToShared()
            } else if (paused) {
                unitLogic.isWorkingEnabled = false
            }
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
                putLongArray("limits", parallelPool.save())
                put("units", ListTag().apply { units.forEach { add(it.save()) } })
            }
        )
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        if (!tag.contains("exoticChemicalUnits", Tag.TAG_COMPOUND.toInt())) return

        val data = tag.getCompound("exoticChemicalUnits")
        globalPaused = data.getBoolean("globalPaused")
        parallelPool.load(data.getLongArray("limits"))
        for (entry in data.getList("units", Tag.TAG_COMPOUND.toInt())) {
            val saved = entry as CompoundTag
            units.getOrNull(saved.getInt("id"))?.load(saved)
        }
        refreshUnits()
    }
}