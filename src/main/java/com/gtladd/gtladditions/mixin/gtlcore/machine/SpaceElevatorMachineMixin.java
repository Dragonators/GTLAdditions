package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine;

import com.gtladd.gtladditions.common.machine.multiblock.controller.SpaceElevatorMKII;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpaceElevatorMachine.class)
public abstract class SpaceElevatorMachineMixin {

    @Inject(method = { "scanAndConnectSpaceElevatorModules", "updateConnectionSubscription" },
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 2)
    private void gtladditions$useRegisteredSlots(CallbackInfo ci) {
        if ((Object) this instanceof SpaceElevatorMKII) ci.cancel();
    }
}