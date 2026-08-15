package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.common.data.GTLBlocks;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorModuleMachine;
import org.gtlcore.gtlcore.utils.MachineUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import com.gtladd.gtladditions.common.machine.multiblock.controller.SpaceElevatorMKII;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SpaceElevatorModuleMachine.class)
public abstract class SpaceElevatorModuleMachineMixin {

    /**
     * @author GNSW
     * @reason Support the expanded module layout of Space Elevator MKII.
     */
    @Overwrite(remap = false)
    public BlockPos[] getHostScanPositions() {
        SpaceElevatorModuleMachine module = (SpaceElevatorModuleMachine) (Object) this;
        Level level = module.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return MachineUtil.EMPTY_POS_ARRAY;

        BlockPos modulePos = module.getPos();
        int[][] originalOffsets = new int[][] {
                { 8, 3 }, { 8, -3 }, { -8, 3 }, { -8, -3 },
                { 3, 8 }, { 3, -8 }, { -3, 8 }, { -3, -8 }
        };
        for (int[] offset : originalOffsets) {
            BlockPos powerCore = modulePos.offset(offset[0], -2, offset[1]);
            if (serverLevel.getBlockState(powerCore).getBlock() == GTLBlocks.POWER_CORE.get()) {
                return gtladditions$hostPositions(powerCore);
            }
        }
        for (int[] offset : SpaceElevatorMKII.Companion.getMODULE_OFFSETS()) {
            BlockPos powerCore = modulePos.offset(offset[0], -offset[1], offset[2]);
            if (serverLevel.getBlockState(powerCore).getBlock() == GTLBlocks.POWER_CORE.get()) {
                return gtladditions$hostPositions(powerCore);
            }
        }
        return MachineUtil.EMPTY_POS_ARRAY;
    }

    @Unique
    private static BlockPos[] gtladditions$hostPositions(BlockPos powerCore) {
        return new BlockPos[] {
                powerCore.offset(3, 2, 0),
                powerCore.offset(-3, 2, 0),
                powerCore.offset(0, 2, 3),
                powerCore.offset(0, 2, -3)
        };
    }
}