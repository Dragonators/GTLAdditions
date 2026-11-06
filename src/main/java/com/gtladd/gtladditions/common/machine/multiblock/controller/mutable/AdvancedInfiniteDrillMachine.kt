package com.gtladd.gtladditions.common.machine.multiblock.controller.mutable

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.common.machine.hatch.OreProcessorHatch
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.material.Fluid
import org.gtlcore.gtlcore.api.machine.ISuspendableMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.common.machine.multiblock.part.TemperatureSensorPartMachine
import org.gtlcore.gtlcore.common.machine.trait.AdvancedInfiniteDrillLogic
import org.gtlcore.gtlcore.utils.NumberUtils
import java.math.BigInteger

class AdvancedInfiniteDrillMachine(holder: IMachineBlockEntity) :
    org.gtlcore.gtlcore.common.machine.multiblock.electric.AdvancedInfiniteDrillMachine(holder),
    IThreadModifierMachine {
    private var threadPartMachine: IThreadModifierPart? = null
    private var analysisHatch: OreProcessorHatch? = null
    private var temperatureSensors: List<TemperatureSensorPartMachine> = emptyList()

    override fun getRecipeLogic(): AdvancedInfiniteDrillMachineLogic = super.getRecipeLogic() as AdvancedInfiniteDrillMachineLogic

    override fun createRecipeLogic(vararg args: Any?): RecipeLogic =
        AdvancedInfiniteDrillMachineLogic(this, DEFAULT_RANGE)

    private fun syncRange() {
        val target = when (analysisHatch?.thirdTier) {
            0 -> 6
            1 -> 9
            2 -> 11
            3 -> 13
            else -> DEFAULT_RANGE
        }

        val logic = getRecipeLogic()

        if (logic.range != target) {
            logic.range = target
            logic.veinFluids.clear()
            logic.markLastRecipeDirty()
            logic.updateTickSubscription()
        }
    }

    override fun onStructureFormed() {
        super.onStructureFormed()

        analysisHatch = parts.firstOrNull { it is OreProcessorHatch } as? OreProcessorHatch
        temperatureSensors = parts.filterIsInstance<TemperatureSensorPartMachine>()

        syncRange()
    }

    override fun heatUpdate() {
        syncRange()

        if (analysisHatch?.matchAll == true && maxHeat >= 0) {
            currentHeat = maxHeat
            process = 0
            temperatureSensors.forEach { it.update(currentHeat) }
        } else {
            super.heatUpdate()
        }
    }

    private fun productionPlan(): Pair<Long, Long>? {
        val fluids = getRecipeLogic().veinFluids
        if (fluids.isEmpty()) return null

        val hatch = analysisHatch
        // Core folds input amperage into the EnergyContainerList voltage.
        val availablePower = energyContainer.inputVoltage

        var baseEUt = GTValues.V[9]
        var largestFluid = 0L

        for (amount in fluids.values) {
            baseEUt += amount
            largestFluid = maxOf(largestFluid, amount)
        }

        return calculateProduction(
            baseEUt,
            rate.toLong(),
            threadPartMachine?.getThreadCount() ?: 1,
            availablePower,
            largestFluid,
            hatch?.firstTier,
            hatch?.secondTier,
            hatch?.matchAll == true
        )
    }

    private fun calculateProduction(
        baseEUt: Long,
        baseRate: Long,
        threads: Int,
        availablePower: Long,
        largestFluid: Long,
        yieldTier: Int?,
        energyTier: Int?,
        perfect: Boolean
    ): Pair<Long, Long>? {
        if (
            baseEUt <= 0 ||
            baseRate <= 0 ||
            availablePower <= 0 ||
            largestFluid <= 0
        ) {
            return null
        }

        val unitEUt = if (energyTier == null) {
            baseEUt
        } else {
            scale(baseEUt, energyPercent[energyTier], 100).coerceAtLeast(1)
        }

        // Round each spectrum bonus once before combining it with the integral thread count.
        val tunedRate = if (yieldTier == null) {
            baseRate
        } else {
            scale(baseRate, yieldTenths[yieldTier], 10)
        }

        val wanted = NumberUtils.saturatedMultiply(tunedRate, threads.coerceAtLeast(1).toLong())
        val parallel = minOf(
            wanted,
            availablePower / unitEUt,
            Long.MAX_VALUE / largestFluid
        )
        if (parallel <= 0) return null

        val overclockLevel = if (perfect) {
            val powerRatio = availablePower / (unitEUt * parallel)
            val outputRatio = Long.MAX_VALUE / largestFluid / parallel

            // Each level requires 4x power and doubles the fluid output.
            val powerLevels = (Long.SIZE_BITS - 1 - powerRatio.countLeadingZeroBits()) / 2
            val outputLevels = Long.SIZE_BITS - 1 - outputRatio.countLeadingZeroBits()
            minOf(15, powerLevels, outputLevels)
        } else {
            0
        }

        val multiplier = parallel shl overclockLevel
        return multiplier to (unitEUt * multiplier)
    }

    private fun scale(value: Long, numerator: Long, denominator: Long): Long =
        BigInteger.valueOf(value)
            .multiply(BigInteger.valueOf(numerator))
            .divide(BigInteger.valueOf(denominator))
            .min(BigInteger.valueOf(Long.MAX_VALUE))
            .toLong()

    override fun addDisplayText(tooltips: MutableList<Component>) {
        super.addDisplayText(tooltips)
        if (!isFormed || isEmpty || maxHeat < 0) return

        val logic = getRecipeLogic()
        val scanSize = 2 * (logic.range / 2) + 1
        val iterator = tooltips.listIterator()

        while (iterator.hasNext()) {
            when ((iterator.next().contents as? TranslatableContents)?.key) {
                "gtceu.machine.advanced_infinite_driller.range" -> iterator.set(
                    Component.translatable(
                        "gtceu.machine.advanced_infinite_driller.range",
                        Component.literal("${scanSize}x$scanSize").withStyle(ChatFormatting.GOLD)
                    )
                )
                "gtceu.machine.advanced_infinite_driller.drilled_fluid" -> iterator.remove()
            }
        }

        val current = logic.lastRecipe?.takeIf { logic.isActive }
        if (current != null) {
            for (content in current.outputs[FluidRecipeCapability.CAP].orEmpty()) {
                val ingredient = FluidRecipeCapability.CAP.of(content.content)
                // The backing stacks field is a lazy cache; resolve it through the getter.
                val stack = ingredient.getStacks().firstOrNull() ?: continue
                addFluidDisplayText(tooltips, stack.fluid, ingredient.amount)
            }
        } else {
            val (outputMultiplier, _) = productionPlan() ?: return

            for ((fluid, amount) in logic.veinFluids) {
                addFluidDisplayText(tooltips, fluid, amount * outputMultiplier)
            }
        }
    }

    private fun addFluidDisplayText(
        tooltips: MutableList<Component>,
        fluid: Fluid,
        amount: Long
    ) {
        tooltips.add(
            Component.translatable(
                "gtladditions.drill.fluid_output",
                fluid.fluidType.description.copy().withStyle(ChatFormatting.GREEN),
                Component.literal(FormattingUtil.formatNumbers(amount)).withStyle(ChatFormatting.AQUA)
            )
        )
    }

    @Suppress("unused")
    fun supportsBatchProcessing(): Boolean = false

    @Suppress("unused")
    fun canConfigureBatchProcessing(): Boolean = false

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        clearStructureCache()
    }

    override fun onPartUnload() {
        super.onPartUnload()
        clearStructureCache()
    }

    private fun clearStructureCache() {
        threadPartMachine = null
        analysisHatch = null
        temperatureSensors = emptyList()

        val logic = getRecipeLogic()
        logic.range = DEFAULT_RANGE
        logic.veinFluids.clear()
    }

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine

    companion object {
        private const val DEFAULT_RANGE = 5
        private val yieldTenths = longArrayOf(15, 23, 32, 40)
        private val energyPercent = longArrayOf(80, 60, 20, 5)

        class AdvancedInfiniteDrillMachineLogic(machine: AdvancedInfiniteDrillMachine, range: Int) : AdvancedInfiniteDrillLogic(machine, range) {

            override fun getMachine(): AdvancedInfiniteDrillMachine = super.getMachine() as AdvancedInfiniteDrillMachine

            override fun findAndHandleRecipe() {
                val serverLevel = getMachine().level as? ServerLevel ?: return
                lastRecipe = null

                if (veinFluids.isEmpty()) {
                    this.getGridFluid(BedrockFluidVeinSavedData.getOrCreate(serverLevel))
                    if (veinFluids.isEmpty()) {
                        subscription?.unsubscribe()
                        subscription = null
                        return
                    }
                }

                if (getMachine().isEmpty || !getMachine().canRunnable()) return

                getFluidDrillRecipeWithThreadModifier()?.let { match ->
                    setupRecipe(match)
                }
            }

            override fun onRecipeFinish() {
                lastRecipe?.let { RecipeRunnerHelper.handleRecipeOutput(this.machine, it) }

                val suspendableMachine = machine as? ISuspendableMachine
                if (suspendableMachine?.`gtlcore$isSuspendAfterFinish`() == true) {
                    status = Status.SUSPEND
                    suspendableMachine.`gtlcore$setSuspendAfterFinish`(false)
                } else {
                    getFluidDrillRecipeWithThreadModifier()?.let { match ->
                        setupRecipe(match)
                        return@onRecipeFinish
                    }
                    status = Status.IDLE
                }
                progress = 0
                duration = 0
            }

            private fun getFluidDrillRecipeWithThreadModifier(): GTRecipe? {
                if (veinFluids.isEmpty()) return null

                val machine = getMachine()
                val plan = machine.productionPlan()
                if (plan == null) {
                    RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN)
                    return null
                }

                val (outputMultiplier, actualEUt) = plan
                val builder = GTRecipeBuilder.ofRaw()
                    .duration(MAX_PROGRESS)
                    .EUt(actualEUt)

                for ((fluid, amount) in veinFluids) {
                    builder.outputFluids(FluidStack.create(fluid, amount * outputMultiplier))
                }

                val recipe = builder.buildRawRecipe()

                return recipe.takeIf {
                    RecipeRunnerHelper.matchRecipe(machine, it) &&
                        it.matchTickRecipe(machine).isSuccess
                }
            }
        }
    }
}