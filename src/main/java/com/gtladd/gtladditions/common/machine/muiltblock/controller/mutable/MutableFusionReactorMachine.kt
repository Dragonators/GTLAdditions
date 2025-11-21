package com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import java.util.function.BiPredicate

class MutableFusionReactorMachine(holder: IMachineBlockEntity, tier: Int) : FusionReactorMachine(holder, tier),
    IWirelessThreadModifierParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    override fun getMaxVoltage(): Long {
        return super.getOverclockVoltage()
    }

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return MutableRecipesLogic<MutableFusionReactorMachine>(this, START_CHECK)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutableFusionReactorMachine> {
        return super.getRecipeLogic() as MutableRecipesLogic<MutableFusionReactorMachine>
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        threadPartMachine = null
        getRecipeLogic().setUseMultipleRecipes(false)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        threadPartMachine = null
        getRecipeLogic().setUseMultipleRecipes(false)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun getMaxParallel(): Int {
        return (this as IRecipeCapabilityMachine).parallelHatch?.currentParallel ?: 1
    }

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine

    companion object {
        private val START_CHECK = BiPredicate { recipe: GTRecipe, recipeLogicMachine: IRecipeLogicMachine ->
            if (recipeLogicMachine is MutableFusionReactorMachine && recipe.data.contains("eu_to_start")) {
                val startEu = recipe.data.getLong("eu_to_start")
                val heatDiff = startEu - recipeLogicMachine.heat
                if (heatDiff <= 0L) {
                    return@BiPredicate true
                } else if (recipeLogicMachine.energyContainer.getEnergyStored() < heatDiff) {
                    return@BiPredicate false
                } else {
                    recipeLogicMachine.energyContainer.removeEnergy(heatDiff)
                    recipeLogicMachine.heat += heatDiff
                    recipeLogicMachine.updatePreHeatSubscription()
                    return@BiPredicate true
                }
            }
            false
        }
    }
}
