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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Unique
    private long gtladditions$entityOutput = -1;

    @Shadow(remap = false)
    private int countEntitesAround(Level level, BlockPos pos) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    private static void spawnItemEntity(Level level, BlockEntity te, Direction side, ItemStack is) {
        throw new AssertionError();
    }

    @Inject(method = "placeInWorld", at = @At("HEAD"), remap = false)
    private void gtladditions$resetEntityOutput(AEKey what, long amount, Actionable type, boolean placeAsEntity,
                                                CallbackInfoReturnable<Long> cir) {
        this.gtladditions$entityOutput = -1;
    }

    @Redirect(method = "placeInWorld",
              at = @At(value = "INVOKE",
                       target = "Lappeng/parts/automation/ItemPlacementStrategy;countEntitesAround(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"),
              remap = false)
    private int gtladditions$placeMultipleItemEntities(ItemPlacementStrategy instance, Level level, BlockPos placePos,
                                                       AEKey what, long amount, Actionable type,
                                                       boolean placeAsEntity) {
        int sum = this.countEntitesAround(level, placePos);
        int configuredRounds = ConfigHolder.getINSTANCE().ae2.formationPlaneItemEntityOutputRounds;
        int rounds = Math.min(Math.max(configuredRounds, 1), 1000);
        if (rounds <= 1) {
            return sum;
        }

        int entityLimit = AEConfig.instance().getFormationPlaneEntityLimit();
        int maxRounds = Math.min(rounds, entityLimit - sum);
        if (maxRounds <= 0) {
            this.gtladditions$entityOutput = 0;
            return Integer.MAX_VALUE;
        }

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

        this.gtladditions$entityOutput = placed;
        return Integer.MAX_VALUE;
    }

    @Inject(method = "placeInWorld", at = @At("RETURN"), cancellable = true, remap = false)
    private void gtladditions$returnMultipleItemEntityOutput(AEKey what, long amount, Actionable type,
                                                             boolean placeAsEntity, CallbackInfoReturnable<Long> cir) {
        long entityOutput = this.gtladditions$entityOutput;
        this.gtladditions$entityOutput = -1;
        if (entityOutput >= 0) {
            cir.setReturnValue(entityOutput);
        }
    }
}