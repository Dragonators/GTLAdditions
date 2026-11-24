package com.gtladd.gtladditions.integration.gtlcore.monitor

import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.gui.ExoticChemicalDisplay
import com.gtladd.gtladditions.api.machine.gui.ExoticChemicalUnitState
import com.gtladd.gtladditions.api.machine.logic.chemical.ExoticChemicalRecipeLogic
import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex
import com.gtladd.gtladditions.mixin.gtlcore.integration.MachineMonitorEntriesAccessor
import net.minecraft.SharedConstants
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.DISTORT_RECIPES
import org.gtlcore.gtlcore.utils.NumberUtils

object ExoticChemicalMonitor {

    const val EXOTIC_RECIPE_MODE = "ExoticRecipeMode"
    const val EXOTIC_MULTI_UNIT = "ExoticMultiUnit"
    const val EXOTIC_RUNNING_UNITS = "ExoticRunningUnits"
    const val EXOTIC_UNLOCKED_UNITS = "ExoticUnlockedUnits"
    const val EXOTIC_RATE_PER_SEC = "ExoticRatePerSec"
    const val EXOTIC_MAX_RATE_PER_SEC = "ExoticMaxRatePerSec"
    const val WIRELESS_TICK_INPUTS = WirelessMonitorPower.TAG

    const val MODE_LARGE: Byte = 0
    const val MODE_DISTORT: Byte = 1
    const val MODE_MIXED: Byte = 2
    const val MODE_DUAL_IDLE: Byte = 3

    const val LARGE_TEXT = "gtladditions.exotic_chemical.recipe.large_chemical"
    const val DISTORT_TEXT = "gtladditions.exotic_chemical.recipe.distort"
    const val MIXED_TEXT = "gtladditions.exotic_chemical.monitor.mode.mixed"
    const val DUAL_IDLE_TEXT = "gtladditions.exotic_chemical.monitor.mode.dual_idle"

    /** Arguments: blocked unit count, first reason component. */
    const val BLOCKED_TEXT = "gtladditions.exotic_chemical.monitor.blocked"
    const val UNBOUND_TEXT = "gtladditions.exotic_chemical.monitor.wireless_unbound"
    const val UNAVAILABLE_TEXT = "gtladditions.exotic_chemical.monitor.unit_unavailable"

    /** Arguments: running units, unlocked units. */
    const val RUNNING_TEXT = "gtladditions.exotic_chemical.monitor.running_units"

    const val STATUS = "Status"
    const val ACTIVE = "Active"
    const val WORKING = "Working"
    const val WORKING_ENABLED = "WorkingEnabled"
    const val WORK_REASON = "work_reason"
    const val REASON = "reason"
    const val HAS_RECIPE = "HasRecipe"
    const val CONTENT_VERSION = "ContentVersion"
    const val PROGRESS = "Progress"
    const val MAX_PROGRESS = "MaxProgress"
    const val INPUTS = "Inputs"
    const val OUTPUTS = "Outputs"
    const val AMOUNT = "Amount"
    const val MAX_AMOUNT = "MaxAmount"
    const val NON_CONSUMED = "NonConsumed"
    const val TICK = "Tick"
    const val CHANCE = "Chance"
    const val MAX_CHANCE = "MaxChance"
    const val TIER_CHANCE_BOOST = "TierChanceBoost"
    const val ITEMS = "Items"
    const val FLUIDS = "Fluids"

    @JvmStatic
    fun modeName(mode: Byte): Component = Component.translatable(
        when (mode) {
            MODE_DISTORT -> DISTORT_TEXT
            MODE_MIXED -> MIXED_TEXT
            MODE_DUAL_IDLE -> DUAL_IDLE_TEXT
            else -> LARGE_TEXT
        }
    )

    internal fun recipeMode(host: ExoticChemicalComplex): Byte {
        if (host.structureTier < 3) return MODE_LARGE

        var mode = MODE_DUAL_IDLE
        if (host.isFormed) {
            for (unit in host.getRecipeLogic().units) {
                val recipe = unit.currentRecipe ?: continue
                val batchMode = if (recipe.recipeType === DISTORT_RECIPES) MODE_DISTORT else MODE_LARGE
                if (mode != MODE_DUAL_IDLE && mode != batchMode) return MODE_MIXED
                mode = batchMode
            }
        }

        return mode
    }

    @JvmStatic
    fun sampledArray(logic: ExoticChemicalRecipeLogic): Array<GTRecipe?> = arrayOfNulls(logic.units.size)

    /** Compares each unit's live recipe pointer against [sampled] and updates changed slots in place. */
    @JvmStatic
    fun syncBatches(logic: ExoticChemicalRecipeLogic, sampled: Array<GTRecipe?>): Boolean {
        var changed = false
        val units = logic.units

        for (i in units.indices) {
            val current = units[i].currentRecipe
            if (sampled[i] !== current) {
                sampled[i] = current
                changed = true
            }
        }

        return changed
    }

    /** Writes the live fields and returns the running batch mask used to invalidate row rates. */
    @JvmStatic
    fun writeState(state: CompoundTag, logic: ExoticChemicalRecipeLogic): Int {
        val host = logic.host
        var batches = 0
        var progress = 0
        var duration = 0
        var runningMask = 0
        var running = 0
        var unlockedUnits = 0
        var blocked = 0
        var allPaused = true
        var reason: Component? = null

        for (unit in logic.units) {
            val unlocked = logic.isUnitUnlocked(unit.id)
            val batch = unit.currentRecipe
            if (!unlocked && batch == null) continue

            if (batch != null) {
                batches++
                progress = unit.progress
                duration = unit.duration
            }
            if (!unit.isSuspend) allPaused = false

            // A downgraded unit can still finish a batch. Only unlocked units count towards the GUI header.
            if (unlocked) unlockedUnits++

            when (ExoticChemicalDisplay.operatingState(logic, unit)) {
                ExoticChemicalUnitState.RUN -> {
                    runningMask = runningMask or (1 shl unit.id)
                    if (unlocked) running++
                }

                ExoticChemicalUnitState.WAIT -> {
                    blocked++
                    if (reason == null && unit.isRecipeLogicAvailable) reason = ExoticChemicalDisplay.message(unit)
                }

                else -> Unit
            }
        }

        val suspended = logic.globalPaused || allPaused
        val working = !suspended && runningMask != 0
        val status = when {
            suspended -> "paused"
            working -> "working"
            blocked > 0 -> "waiting"
            else -> "idle"
        }

        state.putString(STATUS, status)
        state.putBoolean(ACTIVE, working || batches > 0)
        state.putBoolean(WORKING, working)
        state.putBoolean(WORKING_ENABLED, !suspended)

        if (blocked > 0) {
            val cause = if (host.uuid == null) {
                Component.translatable(UNBOUND_TEXT)
            } else {
                reason ?: Component.translatable(UNAVAILABLE_TEXT)
            }
            val detail = Component.translatable(BLOCKED_TEXT, blocked, cause)
            state.putString(if (working) WORK_REASON else REASON, Component.Serializer.toJson(detail))
        }

        state.putByte(EXOTIC_RECIPE_MODE, recipeMode(host))
        state.putBoolean(HAS_RECIPE, batches > 0)
        state.putInt(PROGRESS, if (batches == 1) progress else 0)
        state.putInt(MAX_PROGRESS, if (batches == 1) duration else 0)

        if (batches >= 2) {
            state.putBoolean(EXOTIC_MULTI_UNIT, true)
            state.putInt(EXOTIC_RUNNING_UNITS, running)
            state.putInt(EXOTIC_UNLOCKED_UNITS, unlockedUnits)
        }

        val netEut = ExoticChemicalDisplay.netEut(logic)
        if (netEut.signum() != 0) state.putByteArray(WIRELESS_TICK_INPUTS, netEut.negate().toByteArray())

        return runningMask
    }

    @JvmStatic
    fun contents(logic: ExoticChemicalRecipeLogic, multiUnit: Boolean, runningMask: Int): CompoundTag {
        val inputs = Rows(multiUnit)
        val outputs = Rows(multiUnit)
        val outputRows = ListTag()

        for (unit in logic.units) {
            val recipe = unit.currentRecipe ?: continue
            val runningDuration = if (runningMask and (1 shl unit.id) != 0) unit.duration else 0
            inputs.add(unit.monitorInputs(), runningDuration)

            MachineMonitorEntriesAccessor.invokeAppend(outputRows, recipe.outputs, false)
            MachineMonitorEntriesAccessor.invokeAppend(outputRows, recipe.tickOutputs, true)
            outputs.add(outputRows, runningDuration)
            outputRows.clear()
        }

        return CompoundTag().apply {
            put(INPUTS, inputs.entries())
            put(OUTPUTS, outputs.entries())
        }
    }

    private data class RowKey(
        val items: Tag?,
        val fluids: Tag?,
        val tick: Boolean,
        val nonConsumed: Boolean,
        val chance: Int,
        val maxChance: Int,
        val tierChanceBoost: Int
    )

    private class Rows(private val withRates: Boolean) {
        private val grouped = LinkedHashMap<RowKey, CompoundTag>()

        fun add(rows: ListTag, runningDuration: Int = 0) {
            for (i in rows.indices) {
                val row = rows.getCompound(i)
                val key = RowKey(
                    row.get(ITEMS),
                    row.get(FLUIDS),
                    row.getBoolean(TICK),
                    row.getBoolean(NON_CONSUMED),
                    row.getInt(CHANCE),
                    row.getInt(MAX_CHANCE),
                    row.getInt(TIER_CHANCE_BOOST)
                )

                val amount = row.getLong(AMOUNT)
                val maximum = if (row.contains(MAX_AMOUNT)) row.getLong(MAX_AMOUNT) else amount
                val previous = grouped[key]
                val total = previous ?: row.copy().also { grouped[key] = it }

                if (previous != null) {
                    val prior = total.getLong(AMOUNT)
                    if (total.contains(MAX_AMOUNT) || row.contains(MAX_AMOUNT)) {
                        val priorMax = if (total.contains(MAX_AMOUNT)) total.getLong(MAX_AMOUNT) else prior
                        total.putLong(MAX_AMOUNT, combine(priorMax, maximum, key.nonConsumed))
                    }
                    total.putLong(AMOUNT, combine(prior, amount, key.nonConsumed))
                }

                if (withRates && !key.nonConsumed && !(key.chance > 0 && key.chance < key.maxChance)) {
                    val priorRate = total.getDouble(EXOTIC_RATE_PER_SEC)
                    val priorMax = if (total.contains(EXOTIC_MAX_RATE_PER_SEC)) {
                        total.getDouble(EXOTIC_MAX_RATE_PER_SEC)
                    } else {
                        priorRate
                    }

                    total.putDouble(EXOTIC_RATE_PER_SEC, priorRate + rate(amount, key.tick, runningDuration))
                    if (total.getLong(MAX_AMOUNT) > total.getLong(AMOUNT)) {
                        total.putDouble(EXOTIC_MAX_RATE_PER_SEC, priorMax + rate(maximum, key.tick, runningDuration))
                    } else {
                        total.remove(EXOTIC_MAX_RATE_PER_SEC)
                    }
                }
            }
        }

        private fun combine(a: Long, b: Long, nonConsumed: Boolean): Long =
            if (nonConsumed) maxOf(a, b) else NumberUtils.saturatedAdd(a, b)

        private fun rate(amount: Long, tick: Boolean, runningDuration: Int): Double {
            if (runningDuration == 0) return 0.0

            val perSecond = amount.toDouble() * SharedConstants.TICKS_PER_SECOND
            return if (tick) perSecond else perSecond / runningDuration
        }

        fun entries(): ListTag = ListTag().apply { addAll(grouped.values) }
    }
}