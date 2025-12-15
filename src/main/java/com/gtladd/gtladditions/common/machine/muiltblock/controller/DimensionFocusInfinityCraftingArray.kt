package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.machine.muiltblock.controller.MolecularAssemblerMultiblockMachine
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule

class DimensionFocusInfinityCraftingArray (holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder),
    IModularMachineModule<MolecularAssemblerMultiblockMachine, DimensionFocusInfinityCraftingArray>, IMachineLife{

    @field:Persisted
    private var hostPosition: BlockPos? = null
    private var host: MolecularAssemblerMultiblockMachine? = null

    override fun getMaxParallel(): Int = 4096
    override fun getHost(): MolecularAssemblerMultiblockMachine? = host
    override fun getHostType(): Class<MolecularAssemblerMultiblockMachine> = MolecularAssemblerMultiblockMachine::class.java
    override fun getHostPosition(): BlockPos? = hostPosition
    override fun setHost(host: MolecularAssemblerMultiblockMachine?) { this.host = host }
    override fun setHostPosition(pos: BlockPos?) { this.hostPosition = pos }

    // ========================================
    // Host connection
    // ========================================

    override fun getHostScanPositions(): Array<out BlockPos> = arrayOf(
        pos.offset(0, -2, 7),
        pos.offset(0, -2, -7),
        pos.offset(7, -2, 0),
        pos.offset(-7, -2, 0)
    )

    override fun onConnected(host: MolecularAssemblerMultiblockMachine) {
        host.isInfinityMode = true
    }

    override fun removeFromHost(host: MolecularAssemblerMultiblockMachine?) {
        host?.isInfinityMode = false
        super.removeFromHost(host)
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        if (!findAndConnectToHost()) {
            removeFromHost(this.host)
        }
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        removeFromHost(this.host)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(this.host)
    }

    override fun onMachineRemoved() {
        removeFromHost(this.host)
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!this.isFormed) return
        textList.add(
            Component.translatable(
                if (isConnectedToHost) "tooltip.gtlcore.module_installed" else "tooltip.gtlcore.module_not_installed"
            )
        )
    }

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(
                DimensionFocusInfinityCraftingArray::class.java,
                GTLAddWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
            )
    }
}