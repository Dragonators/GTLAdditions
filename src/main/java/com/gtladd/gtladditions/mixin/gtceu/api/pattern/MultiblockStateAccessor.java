package com.gtladd.gtladditions.mixin.gtceu.api.pattern;

import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiblockState.class)
public interface MultiblockStateAccessor {

    @Invoker(value = "update", remap = false)
    boolean invokeUpdate(BlockPos pos, TraceabilityPredicate predicate);
}