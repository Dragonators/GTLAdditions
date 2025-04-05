package com.gtladd.gtladditions.mixin.gtceu.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;

import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.*;

import java.util.List;

@Mixin(FusionReactorMachine.class)
public class FusionReactorMachineMixin extends WorkableElectricMultiblockMachine {

    @Shadow(remap = false)
    protected long heat = 0L;
    @Mutable
    @Final
    @Shadow(remap = false)
    protected final NotifiableEnergyContainer energyContainer;

    public FusionReactorMachineMixin(IMachineBlockEntity holder, NotifiableEnergyContainer energyContainer, Object... args) {
        super(holder, args);
        this.energyContainer = energyContainer;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (this.isFormed()) {
            textList.add(Component.translatable("gtceu.multiblock.fusion_reactor_energy", this.energyContainer.getEnergyStored() / 1000000, this.energyContainer.getEnergyCapacity() / 1000000));
            textList.add(Component.translatable("gtceu.multiblock.fusion_reactor.heat", this.heat));
        }
    }
}
