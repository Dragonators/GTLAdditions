package com.gtladd.gtladditions.mixin.gtlcore.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;

import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine;
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Implements(@Interface(
                       iface = IWirelessElectricMultiblockMachine.class,
                       prefix = "gTLAdditions$"))
@Mixin(WorkableElectricMultiblockMachine.class)
public abstract class WorkableElectricMultiblockMachineMixin {

    @Unique
    @Nullable
    private IWirelessNetworkEnergyHandler gTLAdditions$wirelessNetworkEnergyHandler;

    @Inject(method = "onStructureInvalid", at = @At("TAIL"), remap = false)
    private void onStructureInvalid(CallbackInfo ci) {
        this.gTLAdditions$wirelessNetworkEnergyHandler = null;
    }

    @Inject(method = "onPartUnload", at = @At("TAIL"), remap = false)
    private void onPartUnload(CallbackInfo ci) {
        this.gTLAdditions$wirelessNetworkEnergyHandler = null;
    }

    @Unique
    public void gTLAdditions$setWirelessNetworkEnergyHandler(IWirelessNetworkEnergyHandler trait) {
        this.gTLAdditions$wirelessNetworkEnergyHandler = trait;
    }

    @Unique
    public @Nullable IWirelessNetworkEnergyHandler gTLAdditions$getWirelessNetworkEnergyHandler() {
        return this.gTLAdditions$wirelessNetworkEnergyHandler;
    }
}
