package com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.machine.multiblock.electric.TierCasingMachine
import java.util.function.BiPredicate

open class MutableTierCasingMachine(holder: IMachineBlockEntity, tierType: String, vararg args: Any?) :
    TierCasingMachine(holder, tierType, *args), IWirelessThreadModifierParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return MutableRecipesLogic<MutableTierCasingMachine?>(this, TIER_CHECK)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutableTierCasingMachine> {
        return super.getRecipeLogic() as MutableRecipesLogic<MutableTierCasingMachine>
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

    override fun getMaxParallel(): Int {
        return (this as IRecipeCapabilityMachine).parallelHatch?.currentParallel ?: 1
    }

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine

    companion object {
        @JvmStatic
        protected val TIER_CHECK: BiPredicate<GTRecipe, IRecipeLogicMachine> =
            BiPredicate { recipe: GTRecipe, recipeLogicMachine: IRecipeLogicMachine ->
                if (recipeLogicMachine is TierCasingMachine) {
                    if (recipe.data.contains(recipeLogicMachine.tierType) &&
                        recipe.data.getInt(recipeLogicMachine.tierType) > recipeLogicMachine.tier
                    ) {
                        RecipeResult.of(recipeLogicMachine, RecipeResult.FAIL_NO_ENOUGH_TIER)
                        return@BiPredicate false
                    }
                }
                true
            }
    }
}
