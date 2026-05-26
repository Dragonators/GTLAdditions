package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic
import com.gtladd.gtladditions.common.machine.hatch.OreProcessorHatch
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.OreProcessorRecipeHelper
import org.gtlcore.gtlcore.api.machine.trait.ICheckPatternMachine
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

class BasicOreProcessorMachine(holder: IMachineBlockEntity) : WorkableElectricMultiblockMachine(holder) {

    private var opHatch: OreProcessorHatch? = null

    override fun onStructureFormed() {
        super.onStructureFormed()
        parts.forEach {
            if (it is OreProcessorHatch) {
                opHatch = it
                return
            }
        }
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        opHatch = null
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        configuratorPanel.attachConfigurators(
            IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.0, 1.0, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.5, 1.0, 0.5),
                { isWorkingEnabled },
                { _, pressed -> isWorkingEnabled = pressed }
            )
                .setTooltipsSupplier { listOf((if (it) "behaviour.soft_hammer.enabled" else "behaviour.soft_hammer.disabled").toComponent) }
        )
        ICheckPatternMachine.attachConfigurators(configuratorPanel, self())
        IRecipeCapabilityMachine.attachConfigurators(configuratorPanel, self() as WorkableElectricMultiblockMachine)
    }

    fun getParallel(): Long = GTLRecipeModifiers.getHatchParallel(this).toLong() * (opHatch?.parallelMultiplier ?: 1)

    companion object {
        fun recipeModifier(machine: MetaMachine?, recipe: GTRecipe, params: OCParams, result: OCResult): GTRecipe? {
            val oreProcessor = machine as? BasicOreProcessorMachine ?: return null

            val hatch = oreProcessor.opHatch
            val parallelMultiplier = hatch?.parallelMultiplier ?: 1
            val parallelResult = ParallelLogic.applyParallel(oreProcessor, recipe, parallelMultiplier, false)
            val paralleledRecipe = parallelResult.first ?: return null
            if (parallelResult.getSecond()!! <= 0) return null

            val modifiedRecipe = OreProcessorRecipeHelper.copyForOreProcessor(
                paralleledRecipe,
                itemChanceBoost = hatch?.itemChanceBoost ?: 1,
                durationMultiplier = hatch?.durationMultiplier ?: 1.0
            )
            val overclock = if (hatch?.matchAll == true) {
                OverclockingLogic.PERFECT_OVERCLOCK_SUBTICK
            } else {
                OverclockingLogic.NON_PERFECT_OVERCLOCK_SUBTICK
            }
            return RecipeHelper.applyOverclock(overclock, modifiedRecipe, oreProcessor.overclockVoltage, params, result)
        }
    }
}