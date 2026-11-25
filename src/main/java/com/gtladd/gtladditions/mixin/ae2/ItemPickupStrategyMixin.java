package com.gtladd.gtladditions.mixin.ae2;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import appeng.parts.automation.ItemPickupStrategy;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemPickupStrategy.class)
public abstract class ItemPickupStrategyMixin {

    @ModifyExpressionValue(method = "canHandleBlock",
                           at = @At(value = "INVOKE",
                                    target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroySpeed(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
                                    remap = true),
                           remap = false,
                           require = 1)
    private float gtladditions$allowCommandBlockHardness(float hardness, ServerLevel level, BlockPos pos, BlockState state) {
        return state.is(Blocks.CHAIN_COMMAND_BLOCK) || state.is(Blocks.REPEATING_COMMAND_BLOCK) ? 0.0F : hardness;
    }

    @Inject(method = "obtainBlockDrops", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void gtladditions$commandBlockDrops(ServerLevel level, BlockPos pos, CallbackInfoReturnable<List<ItemStack>> cir) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.CHAIN_COMMAND_BLOCK)) {
            cir.setReturnValue(List.of(new ItemStack(Items.CHAIN_COMMAND_BLOCK)));
        } else if (state.is(Blocks.REPEATING_COMMAND_BLOCK)) {
            cir.setReturnValue(List.of(new ItemStack(Items.REPEATING_COMMAND_BLOCK)));
        }
    }
}
