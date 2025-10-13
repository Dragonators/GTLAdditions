package com.gtladd.gtladditions.common.machine.trait;

import org.gtlcore.gtlcore.api.capability.IInt128EnergyContainer;
import org.gtlcore.gtlcore.utils.NumberUtils;
import org.gtlcore.gtlcore.utils.datastructure.Int128;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.core.Direction;

import com.gtladd.gtladditions.common.machine.muiltblock.part.WirelessEnergyNetworkTerminalPartMachineBase;
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class NetworkEnergyContainer extends NotifiableRecipeHandlerTrait<Long> implements IEnergyContainer, IInt128EnergyContainer {

    @Getter
    protected final IO handlerIO;
    @Getter
    private final long energyCapacity;
    @Getter
    private long inputVoltage;
    @Getter
    private long inputAmperage;
    @Getter
    private long outputVoltage;
    @Getter
    private long outputAmperage;

    @Nullable
    protected TickableSubscription updateSubs;
    protected Int128 lastEnergyInputPerSec = Int128.ZERO();
    protected Int128 lastEnergyOutputPerSec = Int128.ZERO();
    protected Int128 energyInputPerSec = Int128.ZERO();
    protected Int128 energyOutputPerSec = Int128.ZERO();

    public NetworkEnergyContainer(WirelessEnergyNetworkTerminalPartMachineBase machine, IO io) {
        super(machine);
        energyCapacity = Long.MAX_VALUE;
        handlerIO = io;
        switch (io) {
            case IN:
                inputVoltage = Long.MAX_VALUE;
                inputAmperage = 1;
                outputVoltage = 0;
                outputAmperage = 0;
                break;
            case OUT:
                inputVoltage = 0;
                inputAmperage = 0;
                outputVoltage = Long.MAX_VALUE;
                outputAmperage = 1;
                break;
            case BOTH:
                inputVoltage = Long.MAX_VALUE;
                inputAmperage = 1;
                outputVoltage = Long.MAX_VALUE;
                outputAmperage = 1;
        }
    }

    @Override
    public WirelessEnergyNetworkTerminalPartMachineBase getMachine() {
        return (WirelessEnergyNetworkTerminalPartMachineBase) super.getMachine();
    }

    @Override
    public void onMachineLoad() {
        super.onMachineLoad();
        updateSubs = getMachine().subscribeServerTick(updateSubs, this::updateTick);
    }

    @Override
    public void onMachineUnLoad() {
        super.onMachineUnLoad();
        if (updateSubs != null) {
            updateSubs.unsubscribe();
            updateSubs = null;
        }
    }

    public void updateTick() {
        if (getMachine().getOffsetTimer() % 20 == 0) {
            lastEnergyOutputPerSec = energyOutputPerSec.copy();
            lastEnergyInputPerSec = energyInputPerSec.copy();
            energyOutputPerSec.set(0, 0);
            energyInputPerSec.set(0, 0);
        }
    }

    @Override
    public long changeEnergy(long energyToAdd) {
        final long result = getMachine().getUUID() != null ? WirelessEnergyManager.addEUToGlobalEnergyMap(getMachine().getUUID(), energyToAdd, this.machine) ? energyToAdd : 0 : 0;
        addEnergyPerSec(result);
        return result;
    }

    @Override
    public List<Long> handleRecipeInner(IO io, GTRecipe recipe, List<Long> left, @Nullable String slotName, boolean simulate) {
        long sum = left.stream().reduce(0L, Long::sum);
        if (io == IO.IN) {
            long canOutput = this.getEnergyStored();
            long actualOutput = Math.min(canOutput, sum);
            if (!simulate) {
                this.addEnergy(-actualOutput);
            }
            sum = sum - canOutput;
        } else if (io == IO.OUT) {
            boolean canInput = getMachine().getUUID() != null;
            if (!simulate && canInput) {
                this.addEnergy(sum);
            }
            sum = canInput ? 0 : sum;
        }
        return sum <= 0 ? null : Collections.singletonList(sum);
    }

    @Override
    public long getEnergyStored() {
        return handlerIO == IO.IN ? getMachine().getUUID() != null ? NumberUtils.getLongValue(WirelessEnergyManager.getUserEU(getMachine().getUUID())) : 0 : 0;
    }

    @Override
    public List<Object> getContents() {
        return List.of(getEnergyStored());
    }

    @Override
    public double getTotalContentAmount() {
        return getEnergyStored();
    }

    @Override
    public RecipeCapability<Long> getCapability() {
        return EURecipeCapability.CAP;
    }

    @Override
    public Int128 getInt128InputPerSec() {
        return lastEnergyInputPerSec.copy();
    }

    @Override
    public Int128 getInt128OutputPerSec() {
        return lastEnergyOutputPerSec.copy();
    }

    @Override
    public void addEnergyPerSec(long energy) {
        if (energy < 0) {
            energyInputPerSec.add(-energy);
        } else if (energy > 0) {
            energyOutputPerSec.add(energy);
        }
    }

    @Override
    public Int128 getInt128EnergyStored() {
        return new Int128(getEnergyStored());
    }

    @Override
    public Int128 getInt128EnergyCapacity() {
        return new Int128(energyCapacity);
    }

    @Override
    public long getInputPerSec() {
        return lastEnergyInputPerSec.longValue();
    }

    @Override
    public long getOutputPerSec() {
        return lastEnergyOutputPerSec.longValue();
    }

    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
        return 0;
    }

    @Override
    public boolean inputsEnergy(Direction direction) {
        return false;
    }

    @Override
    public boolean outputsEnergy(Direction side) {
        return false;
    }
}
