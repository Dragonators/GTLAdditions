package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.gui.LimitedDurationConfigurator
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import java.util.function.BiPredicate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

open class GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder: IMachineBlockEntity) :
    CoilWorkableElectricMultiblockMachine(holder), ParallelMachine, IGTLAddMultiRecipe {
        private var limitedDuration = 20

    companion object {
        private val EBF_CHECK: BiPredicate<GTRecipe?, IRecipeLogicMachine?>? =
            BiPredicate { recipe: GTRecipe?, machine: IRecipeLogicMachine? ->
                val tm = machine as CoilWorkableElectricMultiblockMachine
                val temp = tm.coilType.coilTemperature + 100L * max(0, tm.getTier() - 2)
                if (temp < recipe!!.data.getInt("ebf_temp")) {
                    RecipeResult.of(machine, RecipeResult.fail(Component.translatable("gtceu.recipe.fail.no.enough.temperature")))
                    return@BiPredicate false
                }
                return@BiPredicate true
            }
    }

    public override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return CoilMachineLogic(this)
    }

    override fun getRecipeLogic(): CoilMachineLogic {
        return super.getRecipeLogic() as CoilMachineLogic
    }

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        tag.putInt("drLimit", limitedDuration)
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        limitedDuration = tag.getInt("drLimit")
    }

    override fun getMaxParallel(): Int {
        return min(Int.Companion.MAX_VALUE, 2.0.pow(this.coilType.coilTemperature.toDouble() / 900.0).toInt())
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        configuratorPanel.attachConfigurators(LimitedDurationConfigurator(this))
    }

    override fun setLimitedDuration(number: Int) {
        if (number != limitedDuration) limitedDuration = number
    }

    override fun getLimitedDuration(): Int {
        return this.limitedDuration
    }

    class CoilMachineLogic(machine: GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine) :
        GTLAddMultipleRecipesLogic((machine as ParallelMachine?) !!) {

        override fun checkRecipe(recipe: GTRecipe?): Boolean {
            return super.checkRecipe(recipe) && EBF_CHECK!!.test(recipe, machine)
        }
    }
}
