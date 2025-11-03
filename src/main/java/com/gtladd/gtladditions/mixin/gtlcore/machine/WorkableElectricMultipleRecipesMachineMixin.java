package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.common.machine.multiblock.electric.WorkableElectricMultipleRecipesMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine;
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

@Implements(@Interface(
                       iface = IWirelessThreadModifierParallelMachine.class,
                       prefix = "gTLAdditions$"))
@Mixin(WorkableElectricMultipleRecipesMachine.class)
public abstract class WorkableElectricMultipleRecipesMachineMixin extends WorkableElectricMultiblockMachine {

    @Unique
    protected @Nullable IThreadModifierPart gTLAdditions$threadPartMachine = null;

    public WorkableElectricMultipleRecipesMachineMixin(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Unique
    public void gTLAdditions$setThreadPartMachine(@Nullable IThreadModifierPart threadPartMachine) {
        this.gTLAdditions$threadPartMachine = threadPartMachine;
    }

    @Unique
    public @Nullable IThreadModifierPart gTLAdditions$getThreadPartMachine() {
        return gTLAdditions$threadPartMachine;
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.gTLAdditions$threadPartMachine = null;
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        this.gTLAdditions$threadPartMachine = null;
    }
}
