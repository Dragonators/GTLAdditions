package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorModuleMachine;

import com.gregtechceu.gtceu.api.machine.MetaMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.gtladd.gtladditions.common.machine.multiblock.controller.SpaceElevatorMKII;
import com.gtladd.gtladditions.utils.module.SpaceElevatorConnections;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpaceElevatorModuleMachine.class)
public abstract class SpaceElevatorModuleMachineMixin {

    @Inject(method = "onStructureFormed",
            at = @At(value = "INVOKE",
                     target = "Lorg/gtlcore/gtlcore/common/machine/multiblock/electric/SpaceElevatorModuleMachine;updateConnectionSubscription()V"),
            remap = false,
            require = 1)
    private void gtladditions$publishModule(CallbackInfo ci) {
        SpaceElevatorConnections.register((SpaceElevatorModuleMachine) (Object) this);
    }

    @WrapWithCondition(method = { "onStructureFormed", "connectionTick" },
                       at = @At(value = "INVOKE",
                                target = "Lorg/gtlcore/gtlcore/common/machine/multiblock/electric/SpaceElevatorModuleMachine;reconcileHostConnection(Ljava/lang/String;)V"),
                       remap = false,
                       require = 2)
    private boolean gtladditions$reconcileOriginalElevator(SpaceElevatorModuleMachine module, String trigger) {
        return !(module.getHost() instanceof SpaceElevatorMKII);
    }

    @Inject(method = "onLoad", at = @At("TAIL"), remap = false, require = 1)
    private void gtladditions$restoreModule(CallbackInfo ci) {
        var module = (SpaceElevatorModuleMachine) (Object) this;
        if (module.isRecipeLogicAvailable()) SpaceElevatorConnections.register(module);
    }

    @Inject(method = { "onStructureInvalid", "onPartUnload", "onMachineRemoved", "onUnload" },
            at = @At("HEAD"),
            remap = false,
            require = 4)
    private void gtladditions$unpublishModule(CallbackInfo ci) {
        SpaceElevatorConnections.unregister((SpaceElevatorModuleMachine) (Object) this);
    }

    @Inject(method = "onUnload", at = @At("HEAD"), remap = false, require = 1)
    private void gtladditions$detachRegisteredHost(CallbackInfo ci) {
        var module = (SpaceElevatorModuleMachine) (Object) this;
        if (module.getHost() instanceof SpaceElevatorMKII host) module.removeFromHost(host);
    }

    @Redirect(method = { "getLiveHost", "reconcileHostConnection" },
              at = @At(value = "INVOKE",
                       target = "Lcom/gregtechceu/gtceu/api/machine/MetaMachine;getMachine(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lcom/gregtechceu/gtceu/api/machine/MetaMachine;"),
              remap = false,
              require = 2)
    private MetaMachine gtladditions$loadedHost(BlockGetter level, BlockPos pos) {
        return SpaceElevatorConnections.loadedOriginalHost((ServerLevel) level, pos);
    }

    @Redirect(method = "findPowerCore",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                       remap = true),
              remap = false,
              require = 1)
    private BlockState gtladditions$loadedPowerCore(Level level, BlockPos pos) {
        var chunk = ((ServerLevel) level).getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? Blocks.AIR.defaultBlockState() : chunk.getBlockState(pos);
    }
}