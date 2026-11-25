package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.TierCasingMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.gtladd.gtladditions.utils.module.SpaceElevatorConnections;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpaceElevatorMachine.class)
public abstract class SpaceElevatorMachineMixin extends TierCasingMachine implements IModularMachineHost<SpaceElevatorMachine> {

    @Unique
    private BlockPos[] gtladditions$registeredSlots;

    protected SpaceElevatorMachineMixin(IMachineBlockEntity holder, String tierType, Object... args) {
        super(holder, tierType, args);
    }

    @Inject(method = { "scanAndConnectSpaceElevatorModules", "updateConnectionSubscription" },
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 2)
    private void gtladditions$useRegisteredSlots(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "getPowerCore", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void gtladditions$powerCorePosition(BlockPos pos, Level level, CallbackInfoReturnable<BlockPos> cir) {
        cir.setReturnValue(pos.relative(getFrontFacing().getOpposite(), 3).below(2));
    }

    @Override
    public void scanAndConnectModules() {
        if (isRecipeLogicAvailable()) {
            gtladditions$registeredSlots = SpaceElevatorConnections.registerHost(
                    (SpaceElevatorMachine) (Object) this,
                    gtladditions$registeredSlots);
        }
    }

    @Redirect(method = "onStructureFormed",
              at = @At(value = "INVOKE",
                       target = "Lorg/gtlcore/gtlcore/common/machine/multiblock/electric/SpaceElevatorMachine;safeClearModules()V"),
              remap = false,
              require = 1)
    private void gtladditions$registerFormedHost(SpaceElevatorMachine host) {
        // A formation check can follow onLoad with the preferred overlapping host already connected.
        host.scanAndConnectModules();
    }

    @Inject(method = "onLoad", at = @At("TAIL"), remap = false, require = 1)
    private void gtladditions$restoreHost(CallbackInfo ci) {
        scanAndConnectModules();
    }

    @Redirect(method = { "onStructureInvalid", "onMachineRemoved" },
              at = @At(value = "INVOKE",
                       target = "Lorg/gtlcore/gtlcore/common/machine/multiblock/electric/SpaceElevatorMachine;safeClearModules()V"),
              remap = false,
              require = 2)
    private void gtladditions$releaseFormedHost(SpaceElevatorMachine host) {
        gtladditions$releaseSlots();
    }

    @Override
    public void onPartUnload() {
        gtladditions$releaseSlots();
        super.onPartUnload();
    }

    @Inject(method = "onUnload", at = @At("HEAD"), remap = false, require = 1)
    private void gtladditions$unloadHost(CallbackInfo ci) {
        gtladditions$releaseSlots();
    }

    @Unique
    private void gtladditions$releaseSlots() {
        if (gtladditions$registeredSlots == null) return;

        SpaceElevatorConnections.releaseHost((SpaceElevatorMachine) (Object) this, gtladditions$registeredSlots);
        gtladditions$registeredSlots = null;
    }
}
