package com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import it.unimi.dsi.fastutil.longs.LongLongPair
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.common.machine.multiblock.electric.PCBFactoryMachine

class MutablePCBFactoryMachine(holder: IMachineBlockEntity) : PCBFactoryMachine(holder),
    IWirelessThreadModifierParallelMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    private val nanoSwarmMultiplier: Double
        get() {
            val itemStack = machineStorageItem
            return when (itemStack.`kjs$getId`()) {
                "gtceu:vibranium_nanoswarm" -> (100 - itemStack.count) / 400.0
                "gtceu:gold_nanoswarm" -> (100 - itemStack.count * 0.5) / 100.0
                else -> 1.0
            }
        }

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return object : MutableRecipesLogic<MutablePCBFactoryMachine>(this) {
            override fun getEuMultiplier(): Double {
                return super.getEuMultiplier() * getMachine()!!.nanoSwarmMultiplier
            }

            override fun calculateParallel(
                machine: IRecipeLogicMachine?,
                match: GTRecipe,
                remain: Long
            ): LongLongPair? {
                return if (RecipeHelper.getInputEUt(match) <= GTValues.V[GTValues.LuV]) {
                    LongLongPair.of(IParallelLogic.getMaxParallel(machine, match, Long.Companion.MAX_VALUE), 0)
                } else super.calculateParallel(machine, match, remain)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRecipeLogic(): MutableRecipesLogic<MutablePCBFactoryMachine> {
        return super.getRecipeLogic() as MutableRecipesLogic<MutablePCBFactoryMachine>
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
}
