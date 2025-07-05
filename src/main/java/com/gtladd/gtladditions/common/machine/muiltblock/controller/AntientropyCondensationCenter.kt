package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricParallelHatchMultipleRecipesMachine
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.utils.MachineIO
import org.gtlcore.gtlcore.utils.Registries

class AntientropyCondensationCenter(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricParallelHatchMultipleRecipesMachine(holder, *args) {
    private var ITEM_INPUT = 0

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (this.isFormed()) {
            this.setItemInput()
            textList.add(Component.literal("需要凛冰粉：" + ITEM_INPUT + "个"))
        }
    }

    private fun setItemInput() {
        ITEM_INPUT = 1 shl (GTValues.MAX - this.getTier())
    }

    companion object {
        @JvmStatic
        fun beforeWorking(machine: IRecipeLogicMachine?, recipe: GTRecipe): Boolean {
            if (machine is AntientropyCondensationCenter) {
                machine.setItemInput()
                return MachineIO.inputItem(machine, Registries.getItemStack("kubejs:dust_cryotheum", machine.ITEM_INPUT))
            }
            return false
        }
    }
}
