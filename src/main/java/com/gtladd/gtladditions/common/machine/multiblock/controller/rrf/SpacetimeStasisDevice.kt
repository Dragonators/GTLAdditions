package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder

class SpacetimeStasisDevice(holder: IMachineBlockEntity, vararg args: Any?) : RRFWorkableModuleMachine(holder, *args) {

    // ========================================
    // Metadata
    // ========================================

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            SpacetimeStasisDevice::class.java,
            RRFWorkableModuleMachine.MANAGED_FIELD_HOLDER
        )
    }
}