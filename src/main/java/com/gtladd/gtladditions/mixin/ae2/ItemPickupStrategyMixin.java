package com.gtladd.gtladditions.mixin.ae2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import appeng.parts.automation.ItemPickupStrategy;
import appeng.util.Platform;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

import static appeng.parts.automation.ItemPickupStrategy.isBlockBlacklisted;

@Mixin(ItemPickupStrategy.class)
public abstract class ItemPickupStrategyMixin {

    // Shadow 原方法里用到的 ownerUuid
    @Final
    @Shadow(remap = false)
    @Nullable
    private UUID ownerUuid;

    /**
     * @author Dragons
     * @reason ae2破坏面板支持连锁/循环命令方块
     */
    @Overwrite(remap = false)
    private boolean canHandleBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (isBlockBlacklisted(state.getBlock())) {
            return false;
        }

        // 新逻辑：只在硬度<0 且 不是命令方块/基岩 时才跳过
        var hardness = state.getDestroySpeed(level, pos);
        var ignoreAirAndFluids = state.isAir() || state.liquid();
        var isCreateAggregationOutput = state.getBlock() == Blocks.CHAIN_COMMAND_BLOCK || state.getBlock() == Blocks.REPEATING_COMMAND_BLOCK;

        return !ignoreAirAndFluids && (hardness >= 0f || isCreateAggregationOutput) && level.isLoaded(pos) && level.mayInteract(Platform.getFakePlayer(level, ownerUuid), pos);
    }

    @Inject(
            remap = false,
            method = "obtainBlockDrops",
            at = @At("HEAD"),
            cancellable = true)
    private void onObtainBlockDrops(ServerLevel level, BlockPos pos, CallbackInfoReturnable<List<ItemStack>> cir) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() == Blocks.CHAIN_COMMAND_BLOCK) {
            cir.setReturnValue(List.of(new ItemStack(Items.CHAIN_COMMAND_BLOCK)));
        } else if (state.getBlock() == Blocks.REPEATING_COMMAND_BLOCK) {
            cir.setReturnValue(List.of(new ItemStack(Items.REPEATING_COMMAND_BLOCK)));
        }
    }
}
