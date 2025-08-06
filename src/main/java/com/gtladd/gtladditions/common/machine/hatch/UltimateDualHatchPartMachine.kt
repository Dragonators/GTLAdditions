package com.gtladd.gtladditions.common.machine.hatch

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.gtladd.gtladditions.common.machine.trait.SuperNotifiableFluidTank
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

class UltimateDualHatchPartMachine(holder: IMachineBlockEntity, tier: Int, vararg args: Any?) :
    SuperDualHatchPartMachine(holder, tier, *args){
    override fun createTank(): NotifiableFluidTank {
        return object : SuperNotifiableFluidTank(this, 64, kotlin.Long.Companion.MAX_VALUE, IO.IN) {
            override fun canCapOutput(): Boolean {
                return true
            }
        }
    }
}


