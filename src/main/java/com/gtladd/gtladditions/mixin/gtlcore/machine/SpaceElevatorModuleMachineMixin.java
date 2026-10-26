package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.common.data.GTLBlocks;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorModuleMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import com.gtladd.gtladditions.common.machine.multiblock.controller.SpaceElevatorMKII;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpaceElevatorModuleMachine.class)
public abstract class SpaceElevatorModuleMachineMixin {

    @WrapOperation(method = "getHostScanPositions",
                   at = @At(value = "INVOKE",
                            target = "Lorg/gtlcore/gtlcore/common/machine/multiblock/electric/SpaceElevatorModuleMachine;findPowerCore(Lnet/minecraft/world/level/Level;)Lnet/minecraft/core/BlockPos;"),
                   remap = false,
                   require = 1)
    private BlockPos gtladditions$findExpandedPowerCore(SpaceElevatorModuleMachine module, Level level,
                                                        Operation<BlockPos> original) {
        BlockPos powerCore = original.call(module, level);
        BlockPos modulePos = module.getPos();
        // Keep Add's existing priority between these two legacy anchors.
        if (modulePos.offset(-3, -2, 8).equals(powerCore)) {
            BlockPos preferred = modulePos.offset(3, -2, -8);
            if (level.getBlockState(preferred).is(GTLBlocks.POWER_CORE.get())) return preferred;
        }
        if (powerCore != null) return powerCore;
        for (int[] offset : SpaceElevatorMKII.Companion.getMODULE_OFFSETS()) {
            BlockPos candidate = modulePos.offset(offset[0], -offset[1], offset[2]);
            if (level.getBlockState(candidate).is(GTLBlocks.POWER_CORE.get())) return candidate;
        }
        return null;
    }
}