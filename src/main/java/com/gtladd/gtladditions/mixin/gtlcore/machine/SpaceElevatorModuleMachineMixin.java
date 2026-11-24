package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorModuleMachine;
import org.gtlcore.gtlcore.utils.MachineUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.gtladd.gtladditions.utils.module.SpaceElevatorConnections;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpaceElevatorModuleMachine.class)
public abstract class SpaceElevatorModuleMachineMixin implements IModularMachineModule<SpaceElevatorMachine, SpaceElevatorModuleMachine> {

    @Inject(method = { "updateConnectionSubscription", "reconcileHostConnection" },
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 2)
    private void gtladditions$useRegisteredHosts(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "getHostScanPositions", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void gtladditions$noHostScan(CallbackInfoReturnable<BlockPos[]> cir) {
        cir.setReturnValue(MachineUtil.EMPTY_POS_ARRAY);
    }

    @Override
    public boolean findAndConnectToHost() {
        var module = (SpaceElevatorModuleMachine) (Object) this;
        return module.getLevel() instanceof ServerLevel level &&
                SpaceElevatorConnections.connectRegistered(level, module.getPos(), module);
    }

    @Inject(method = "onStructureFormed", at = @At("TAIL"), remap = false, require = 1)
    private void gtladditions$publishModule(CallbackInfo ci) {
        SpaceElevatorConnections.register(this);
    }

    @Inject(method = "onLoad", at = @At("TAIL"), remap = false, require = 1)
    private void gtladditions$restoreModule(CallbackInfo ci) {
        var module = (SpaceElevatorModuleMachine) (Object) this;
        if (module.isRecipeLogicAvailable()) SpaceElevatorConnections.register(module);
    }

    @Inject(method = { "onStructureInvalid", "onPartUnload", "onMachineRemoved" },
            at = @At("HEAD"),
            remap = false,
            require = 3)
    private void gtladditions$unpublishModule(CallbackInfo ci) {
        SpaceElevatorConnections.unregister(this);
    }

    @Inject(method = "onUnload", at = @At("HEAD"), remap = false, require = 1)
    private void gtladditions$detachRegisteredHost(CallbackInfo ci) {
        SpaceElevatorConnections.unload((SpaceElevatorModuleMachine) (Object) this);
    }
}