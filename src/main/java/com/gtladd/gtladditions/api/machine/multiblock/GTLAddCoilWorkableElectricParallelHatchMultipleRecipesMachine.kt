package com.gtladd.gtladditions.api.machine.multiblock

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

class GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine(holder: IMachineBlockEntity) :
    GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder) {

    override fun getMaxParallel(): Int {
        return GTLRecipeModifiers.getHatchParallel(this)
    }
}
