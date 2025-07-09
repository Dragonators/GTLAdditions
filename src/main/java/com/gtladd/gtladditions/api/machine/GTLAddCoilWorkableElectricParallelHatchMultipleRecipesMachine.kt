package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

class GTLAddCoilWorkableElectricParallelHatchMultipleRecipesMachine(holder: IMachineBlockEntity) :
    GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder), ParallelMachine {

    override fun getMaxParallel(): Int {
        return GTLRecipeModifiers.getHatchParallel(this)
    }
}
