package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.api.machine.logic.UpkeepRecipeLogic
import com.gtladd.gtladditions.utils.recipe.MachineUtil.inputFluid
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.data.GTLMaterials

class SpacetimeStasisDevice(holder: IMachineBlockEntity, vararg args: Any?) : RRFWorkableModuleMachine(holder, *args) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = UpkeepRecipeLogic(this, UPKEEP_RECIPE)

    // ========================================
    // Spacetime upkeep
    // ========================================

    override fun beforeWorking(recipe: GTRecipe?): Boolean = super.beforeWorking(recipe) && paySpacetime()

    override fun onWorking(): Boolean {
        val progress = recipeLogic.progress
        return super.onWorking() && (progress == 0 || progress % 20 != 0 || paySpacetime())
    }

    private fun paySpacetime(): Boolean {
        if (inputFluid(this, SPACETIME_PER_SECOND)) return true
        RecipeResult.of(this, RecipeResult.FAIL_INPUT)
        return false
    }

    // ========================================
    // Metadata
    // ========================================

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            SpacetimeStasisDevice::class.java,
            RRFWorkableModuleMachine.MANAGED_FIELD_HOLDER
        )

        val UPKEEP_EUT: Long = GTValues.VA[GTValues.OpV].toLong()
        val SPACETIME_PER_SECOND: FluidStack = GTLMaterials.SpaceTime.getFluid(70000)
        private val UPKEEP_RECIPE = GTRecipeBuilder.ofRaw().EUt(UPKEEP_EUT).duration(UpkeepRecipeLogic.CYCLE_TICKS).buildRawRecipe()
    }
}