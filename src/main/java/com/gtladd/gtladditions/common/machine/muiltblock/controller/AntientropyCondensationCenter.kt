package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipe
import com.gtladd.gtladditions.api.machine.gui.LimitedDurationConfigurator
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import net.minecraft.nbt.CompoundTag
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers
import org.gtlcore.gtlcore.utils.MachineIO
import org.gtlcore.gtlcore.utils.Registries
import java.util.function.BiPredicate

class AntientropyCondensationCenter(holder: IMachineBlockEntity, vararg args: Any?) :
    WorkableElectricMultiblockMachine(holder, *args), ParallelMachine, IGTLAddMultiRecipe {
    private var limitedDuration = 20

    public override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleRecipesLogic(this, BEFORE_RECIPE)
    }

    override fun getRecipeLogic(): GTLAddMultipleRecipesLogic {
        return super.getRecipeLogic() as GTLAddMultipleRecipesLogic
    }

    override fun getMaxParallel(): Int {
        return GTLRecipeModifiers.getHatchParallel(this)
    }

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        tag.putInt("drLimit", limitedDuration)
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        limitedDuration = tag.getInt("drLimit")
    }

    override fun getLimitedDuration(): Int {
        return this.limitedDuration
    }

    override fun setLimitedDuration(duration: Int) {
        if (duration != this.limitedDuration) this.limitedDuration = duration
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        configuratorPanel.attachConfigurators(LimitedDurationConfigurator(this))
    }

    companion object {
        private val BEFORE_RECIPE = BiPredicate { recipe: GTRecipe?, machine: IRecipeLogicMachine? ->
            if (machine is AntientropyCondensationCenter) return@BiPredicate MachineIO.inputItem(machine,
                Registries.getItemStack("kubejs:dust_cryotheum", 1 shl (14 - machine.getTier()))
            )
            false
        }
    }
}
