package com.gtladd.gtladditions.api.machine.mutable

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine

open class MutableElectricParallelHatchMultiblockMachine(holder: IMachineBlockEntity, vararg args: Any?) :
    MutableElectricMultiblockMachine(holder, *args) {
    override fun getMaxParallel(): Int {
        return (this as IRecipeCapabilityMachine).parallelHatch?.currentParallel ?: 1
    }
}
