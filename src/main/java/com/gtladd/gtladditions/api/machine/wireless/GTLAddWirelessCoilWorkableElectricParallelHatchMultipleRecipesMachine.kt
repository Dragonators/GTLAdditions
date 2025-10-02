package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

open class GTLAddWirelessCoilWorkableElectricParallelHatchMultipleRecipesMachine(
    holder: IMachineBlockEntity,
    vararg args: Any?
) : GTLAddWirelessCoilWorkableElectricMultipleRecipesMultiblockMachine(holder, *args) {
    override fun getMaxParallel(): Int {
        return GTLRecipeModifiers.getHatchParallel(this)
    }
}
