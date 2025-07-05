package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers

open class GTLAddWorkableElectricParallelHatchMultipleRecipesMachine(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder, *args) {
    override fun getMaxParallel(): Int {
        return GTLRecipeModifiers.getHatchParallel(this)
    }
}
