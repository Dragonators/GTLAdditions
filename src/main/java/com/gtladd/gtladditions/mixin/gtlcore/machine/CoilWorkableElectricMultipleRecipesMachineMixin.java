package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.api.machine.multiblock.CoilWorkableElectricMultipleRecipesMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;

import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine;
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Implements(@Interface(
                       iface = IWirelessThreadModifierParallelMachine.class,
                       prefix = "gTLAdditions$"))
@Mixin(CoilWorkableElectricMultipleRecipesMachine.class)
public abstract class CoilWorkableElectricMultipleRecipesMachineMixin extends CoilWorkableElectricMultiblockMachine {

    @Unique
    protected @Nullable IThreadModifierPart gTLAdditions$threadPartMachine = null;

    public CoilWorkableElectricMultipleRecipesMachineMixin(IMachineBlockEntity holder) {
        super(holder);
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
