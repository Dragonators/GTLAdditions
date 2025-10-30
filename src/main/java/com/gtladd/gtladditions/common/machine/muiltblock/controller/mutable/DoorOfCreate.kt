package com.gtladd.gtladditions.common.machine.muiltblock.controller.mutable

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart

class DoorOfCreate(holder: IMachineBlockEntity) : WorkableElectricMultiblockMachine(holder),
    IThreadModifierMachine {
    private var threadPartMachine: IThreadModifierPart? = null

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        threadPartMachine = null
    }

    override fun onPartUnload() {
        super.onPartUnload()
        threadPartMachine = null
    }

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        this.threadPartMachine = threadModifierPart
    }

    override fun getThreadPartMachine(): IThreadModifierPart? = this.threadPartMachine
}