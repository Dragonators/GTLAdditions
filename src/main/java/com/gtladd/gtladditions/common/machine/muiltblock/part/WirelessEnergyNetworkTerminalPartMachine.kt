package com.gtladd.gtladditions.common.machine.muiltblock.part

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import java.math.BigInteger

open class WirelessEnergyNetworkTerminalPartMachine(holder: IMachineBlockEntity, io: IO) :
    WirelessEnergyNetworkTerminalPartMachineBase(holder, io) {
    protected val wirelessNetworkEnergyHandler: IWirelessNetworkEnergyHandler

    init {
        this.wirelessNetworkEnergyHandler = createWirelessNetworkEnergyHandler()
    }

    protected fun createWirelessNetworkEnergyHandler(): IWirelessNetworkEnergyHandler {
        return WirelessNetworkEnergyHandler(this)
    }

    override fun addedToController(controller: IMultiController) {
        super.addedToController(controller)
        if (controller is IWirelessElectricMultiblockMachine) controller.setWirelessNetworkEnergyHandler(
            wirelessNetworkEnergyHandler
        )
    }

    companion object {
        protected val WIRELESS_NETWORK_ENERGY_HANDLER_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            WirelessEnergyNetworkTerminalPartMachine::class.java
        )
    }

    protected inner class WirelessNetworkEnergyHandler(machine: WirelessEnergyNetworkTerminalPartMachine) :
        MachineTrait(machine), IWirelessNetworkEnergyHandler {
        override fun getMachine(): WirelessEnergyNetworkTerminalPartMachine {
            return super.getMachine() as WirelessEnergyNetworkTerminalPartMachine
        }

        override fun consumeEnergy(energy: Int): Boolean {
            return uuid != null && WirelessEnergyManager.addEUToGlobalEnergyMap(uuid, energy, machine)
        }

        override fun consumeEnergy(energy: Long): Boolean {
            return uuid != null && WirelessEnergyManager.addEUToGlobalEnergyMap(uuid, energy, machine)
        }

        override fun consumeEnergy(energy: BigInteger): Boolean {
            return uuid != null && WirelessEnergyManager.addEUToGlobalEnergyMap(uuid, energy, machine)
        }

        override val maxAvailableEnergy: BigInteger
            get() =if (uuid != null) WirelessEnergyManager.getUserEU(uuid) else BigInteger.ZERO


        override val isOnline: Boolean
            get() = uuid != null && (io == IO.OUT || WirelessEnergyManager.getUserEU(uuid).signum() > 0)

        override fun getFieldHolder(): ManagedFieldHolder {
            return WIRELESS_NETWORK_ENERGY_HANDLER_FIELD_HOLDER
        }
    }
}
