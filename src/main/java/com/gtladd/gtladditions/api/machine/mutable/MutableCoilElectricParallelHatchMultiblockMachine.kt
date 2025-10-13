package com.gtladd.gtladditions.api.machine.mutable

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine

open class MutableCoilElectricParallelHatchMultiblockMachine(holder: IMachineBlockEntity) :
    MutableCoilElectricMultiblockMachine(holder) {
    override fun getMaxParallel(): Int {
        return (this as IRecipeCapabilityMachine).parallelHatch?.currentParallel ?: 1
    }
}
