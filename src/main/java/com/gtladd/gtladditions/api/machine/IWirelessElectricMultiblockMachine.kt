package com.gtladd.gtladditions.api.machine

import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler

interface IWirelessElectricMultiblockMachine {
    fun getWirelessNetworkEnergyHandler(): IWirelessNetworkEnergyHandler? = null

    fun setWirelessNetworkEnergyHandler(trait: IWirelessNetworkEnergyHandler) {}
}