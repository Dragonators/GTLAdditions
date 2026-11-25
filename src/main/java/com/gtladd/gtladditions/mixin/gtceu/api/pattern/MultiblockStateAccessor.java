package com.gtladd.gtladditions.mixin.gtceu.api.pattern;

import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiblockState.class)
public interface MultiblockStateAccessor {

    @Accessor(value = "pos", remap = false)
    void setPos(BlockPos pos);

    @Accessor(value = "blockState", remap = false)
    void setBlockState(BlockState blockState);

    @Invoker(value = "update", remap = false)
    boolean invokeUpdate(BlockPos pos, TraceabilityPredicate predicate);
}
