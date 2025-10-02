package com.gtladd.gtladditions.api.machine.wireless

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

class GTLAddWirelessWorkableElectricParallelHatchMultipleRecipesMachine(
    holder: IMachineBlockEntity,
    vararg args: Any?
) : GTLAddWirelessWorkableElectricMultipleRecipesMachine(holder, *args) {
    override fun getMaxParallel(): Int {
        return GTLRecipeModifiers.getHatchParallel(this)
    }
}
