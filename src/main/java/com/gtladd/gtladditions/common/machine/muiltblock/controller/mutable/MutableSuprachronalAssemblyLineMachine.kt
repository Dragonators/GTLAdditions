package com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SuprachronalAssemblyLineMachine
import java.util.function.BiPredicate

class MutableSuprachronalAssemblyLineMachine(holder: IMachineBlockEntity, vararg args: Any?) :
    SuprachronalAssemblyLineMachine(holder, *args), IWirelessThreadModifierParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return MutableRecipesLogic(this, DATA_CHECK, 0.4)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutableSuprachronalAssemblyLineMachine> {
        return super.getRecipeLogic() as MutableRecipesLogic<MutableSuprachronalAssemblyLineMachine>
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
        private val DATA_CHECK = BiPredicate { recipe: GTRecipe, recipeLogicMachine: IRecipeLogicMachine ->
            if(recipeLogicMachine is IRecipeCapabilityMachine){
                val result = recipeLogicMachine.dataAccessHatch?.isRecipeAvailable(recipe)
                    ?: recipe.conditions.none { it is ResearchCondition }
                if (!result) RecipeResult.of(recipeLogicMachine, RecipeResult.FAIL_NO_FIND_RESEARCHED)
                return@BiPredicate result
            }
            false
        }
    }
}
