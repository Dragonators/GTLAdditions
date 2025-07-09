package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.capability.IControllable
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.gui.LimitedDurationConfigurator
import com.gtladd.gtladditions.api.recipeslogic.GTLAddMultipleRecipesLogic
import com.lowdragmc.lowdraglib.gui.util.ClickData
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import java.util.List
import java.util.function.BiConsumer
import java.util.function.BooleanSupplier
import java.util.function.Function
import kotlin.math.min
import kotlin.math.pow

open class GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder: IMachineBlockEntity) :
    CoilWorkableElectricMultiblockMachine(holder), ParallelMachine, ILimitedDuration {
        private var limitedDuration = 20
    public override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleRecipesLogic(this)
    }

    override fun getRecipeLogic(): GTLAddMultipleRecipesLogic {
        return super.getRecipeLogic() as GTLAddMultipleRecipesLogic
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
}
