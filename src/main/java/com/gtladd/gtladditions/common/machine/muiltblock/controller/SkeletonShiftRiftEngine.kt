package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import com.gtladd.gtladditions.api.machine.mutable.MutableCoilElectricMultiblockMachine
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SkeletonShiftRiftEngine(holder: IMachineBlockEntity) : MutableCoilElectricMultiblockMachine(holder) {
    @field:Persisted
    private var casingTier: Int = 0
    @field:Persisted
    private var parallel = 0

    override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
        return object : MutableRecipesLogic<SkeletonShiftRiftEngine>(this){
            override fun getEuMultiplier(): Double {
                return super.getEuMultiplier() * 1 / max(getMachine().casingTier, 1)
            }
        }
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        this.casingTier = multiblockState.matchContext.get("SCTier")
        parallel = maxParallel
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        this.casingTier = 0
        parallel = 0
    }

    override fun getMaxParallel(): Int {
        return min(Int.MAX_VALUE, 2.0.pow(this.coilType.coilTemperature / 1200.0).toInt())
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!this.isFormed) return
        textList.add(
            Component.translatable(
                "gtceu.multiblock.parallel",
                Component.literal(FormattingUtil.formatNumbers(parallel))
                    .withStyle(ChatFormatting.DARK_PURPLE)
            )
                .withStyle(ChatFormatting.GRAY)
        )
        textList.add(Component.translatable("gtceu.casings.tier", this.casingTier))
    }

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    companion object {
        fun recipeModifier(machine: MetaMachine?, recipe: GTRecipe, params: OCParams, result: OCResult): GTRecipe? {
            if (machine is SkeletonShiftRiftEngine) {
                val pair = ParallelLogic.applyParallel(machine, recipe, machine.parallel, false)
                if (pair.getFirst() == null || pair.getSecond()!! <= 0) return null
                val recipe1 = pair.getFirst()
                recipe1.duration = max(recipe1.duration / machine.casingTier, 1)
                return RecipeHelper.applyOverclock(
                    OverclockingLogic.PERFECT_OVERCLOCK,
                    recipe1, machine.overclockVoltage, params, result
                )
            }
            return null
        }
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(SkeletonShiftRiftEngine::class.java, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER)
    }
}
