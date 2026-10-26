package com.gtladd.gtladditions.mixin.ae2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.core.AEConfig;
import appeng.parts.automation.ItemPlacementStrategy;
import com.gtladd.gtladditions.config.ConfigHolder;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemPlacementStrategy.class)
public abstract class ItemPlacementStrategyMixin {

    @Final
    @Shadow(remap = false)
    private ServerLevel level;
    @Final
    @Shadow(remap = false)
    private Direction side;
    @Final
    @Shadow(remap = false)
    private BlockEntity host;

    @Shadow(remap = false)
    private static void spawnItemEntity(Level level, BlockEntity te, Direction side, ItemStack is) {
        throw new AssertionError();
    }

    @WrapOperation(method = "placeInWorld",
                   at = @At(value = "INVOKE",
                            target = "Lappeng/parts/automation/ItemPlacementStrategy;countEntitesAround(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"),
                   remap = false,
                   require = 1)
    private int gtladditions$placeMultipleItemEntities(ItemPlacementStrategy instance, Level level, BlockPos placePos,
                                                       Operation<Integer> original,
                                                       AEKey what, long amount, Actionable type, boolean placeAsEntity,
                                                       @Share("handledEntityOutput") LocalBooleanRef handled,
                                                       @Share("entityOutput") LocalLongRef entityOutput) {
        int sum = original.call(instance, level, placePos);
        int configuredRounds = ConfigHolder.getINSTANCE().ae2.formationPlaneItemEntityOutputRounds;
        int rounds = Math.clamp(configuredRounds, 1, 1000);
        if (rounds <= 1) return sum;

        handled.set(true);
        int maxRounds = Math.min(rounds, AEConfig.instance().getFormationPlaneEntityLimit() - sum);
        AEItemKey itemKey = (AEItemKey) what;
        int maxStackSize = itemKey.getMaxStackSize();
        Direction spawnSide = this.side.getOpposite();
        long remaining = amount;
        long placed = 0;
        for (int round = 0; round < maxRounds && remaining > 0; round++) {
            int stackSize = (int) Math.min(remaining, maxStackSize);
            if (type == Actionable.MODULATE) {
                spawnItemEntity(this.level, this.host, spawnSide, itemKey.toStack(stackSize));
            }
            placed += stackSize;
            remaining -= stackSize;
        }
        entityOutput.set(placed);
        return Integer.MAX_VALUE;
    }

    @ModifyReturnValue(method = "placeInWorld", at = @At("RETURN"), remap = false, require = 1)
    private long gtladditions$returnEntityOutput(long original,
                                                 @Share("handledEntityOutput") LocalBooleanRef handled,
                                                 @Share("entityOutput") LocalLongRef entityOutput) {
        return handled.get() ? entityOutput.get() : original;
    }
}