package com.gtladd.gtladditions.common.machine.muiltblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine;
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public class WirelessEnergyNetworkTerminalPartMachine extends WirelessEnergyNetworkTerminalPartMachineBase {

    protected final IWirelessNetworkEnergyHandler wirelessNetworkEnergyHandler;

    public WirelessEnergyNetworkTerminalPartMachine(IMachineBlockEntity holder, IO io) {
        super(holder, io);
        this.wirelessNetworkEnergyHandler = createWirelessNetworkEnergyHandler();
    }

    protected IWirelessNetworkEnergyHandler createWirelessNetworkEnergyHandler() {
        return new WirelessNetworkEnergyHandler(this);
    }

    @Override
    public void addedToController(@NotNull IMultiController controller) {
        super.addedToController(controller);
        if (controller instanceof IWirelessElectricMultiblockMachine machine) machine.setWirelessNetworkEnergyHandler(wirelessNetworkEnergyHandler);
    }

    protected class WirelessNetworkEnergyHandler extends MachineTrait implements IWirelessNetworkEnergyHandler {

        protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
                WirelessEnergyNetworkTerminalPartMachine.class);

        public WirelessNetworkEnergyHandler(WirelessEnergyNetworkTerminalPartMachine machine) {
            super(machine);
        }

        @Override
        public WirelessEnergyNetworkTerminalPartMachine getMachine() {
            return (WirelessEnergyNetworkTerminalPartMachine) super.getMachine();
        }

        @Override
        public boolean consumeEnergy(int energy) {
            return uuid != null && WirelessEnergyManager.addEUToGlobalEnergyMap(uuid, energy, machine);
        }

        @Override
        public boolean consumeEnergy(long energy) {
            return uuid != null && WirelessEnergyManager.addEUToGlobalEnergyMap(uuid, energy, machine);
        }

        @Override
        public boolean consumeEnergy(BigInteger energy) {
            return uuid != null && WirelessEnergyManager.addEUToGlobalEnergyMap(uuid, energy, machine);
        }

        @Override
        public BigInteger getMaxAvailableEnergy() {
            return uuid != null ? WirelessEnergyManager.getUserEU(uuid) : BigInteger.ZERO;
        }

        @Override
        public boolean isOnline() {
            return uuid != null && (io == IO.OUT || WirelessEnergyManager.getUserEU(uuid).signum() > 0);
        }

        @Override
        public ManagedFieldHolder getFieldHolder() {
            return MANAGED_FIELD_HOLDER;
        }
    }
}
