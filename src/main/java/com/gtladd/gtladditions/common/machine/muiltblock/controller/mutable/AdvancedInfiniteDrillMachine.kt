package com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.common.machine.trait.AdvancedInfiniteDrillLogic
import kotlin.math.max
import kotlin.math.min

class AdvancedInfiniteDrillMachine(holder: IMachineBlockEntity) :
    org.gtlcore.gtlcore.common.machine.multiblock.electric.AdvancedInfiniteDrillMachine(holder),
    IThreadModifierMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    override fun getRecipeLogic(): AdvancedInfiniteDrillMachineLogic {
        return super.getRecipeLogic() as AdvancedInfiniteDrillMachineLogic
    }

    override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
        return AdvancedInfiniteDrillMachineLogic(this, 5)
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        threadPartMachine = null
    }

    override fun onPartUnload() {
        super.onPartUnload()
        threadPartMachine = null
    }

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine

    companion object {
        class AdvancedInfiniteDrillMachineLogic(machine: AdvancedInfiniteDrillMachine, range: Int) :
            AdvancedInfiniteDrillLogic(machine, range) {

            override fun getMachine(): AdvancedInfiniteDrillMachine {
                return super.getMachine() as AdvancedInfiniteDrillMachine
            }

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

                getFluidDrillRecipeWithThreadModifier()?.let { match ->
                    setupRecipe(match)
                    return@onRecipeFinish
                }

                status = Status.IDLE
                progress = 0
                duration = 0
            }

            private fun getFluidDrillRecipeWithThreadModifier(): GTRecipe? {
                if (veinFluids.isEmpty()) return null

                val total = veinFluids.values.sum()
                val euT = GTValues.V[9] + total
                val baseRecipe = GTRecipeBuilder.ofRaw()
                    .duration(MAX_PROGRESS)
                    .EUt(euT)
                    .outputFluids(
                        *veinFluids.entries.map { entry ->
                            FluidStack.create(
                                entry.key,
                                entry.value
                            )
                        }.toTypedArray()
                    )
                    .buildRawRecipe()

                val machine = getMachine()
                val threadCount = machine.threadPartMachine?.getThreadCount() ?: 0
                val actualRate = min(machine.rate.toLong() * max(1, threadCount), machine.overclockVoltage / euT)
                if (actualRate <= 0) return null.also { RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN) }

                val modifiedRecipe = baseRecipe.copy(
                    ContentModifier.multiplier(actualRate.toDouble()),
                    false
                )

                return modifiedRecipe.takeIf {
                    RecipeRunnerHelper.matchRecipe(machine, it) &&
                            it.matchTickRecipe(machine).isSuccess
                }
            }
        }
    }
}