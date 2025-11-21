package com.gtladd.gtladditions.common.machine.hatch

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.gtladd.gtladditions.common.machine.trait.SuperNotifiableFluidTank

class UltimateDualHatchPartMachine(holder: IMachineBlockEntity, tier: Int, vararg args: Any?) :
    SuperDualHatchPartMachine(holder, tier, *args){
    override fun createTank(): NotifiableFluidTank {
        return object : SuperNotifiableFluidTank(this@UltimateDualHatchPartMachine, 64, Long.MAX_VALUE, IO.IN) {
            override fun canCapOutput(): Boolean {
                return true
            }
        }
    }
}


