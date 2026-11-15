package com.gtladd.gtladditions.mixin.gtceu.api.pattern;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockPattern.class)
public interface BlockPatternAccessor {

    @Accessor(value = "blockMatches", remap = false)
    TraceabilityPredicate[][][] getBlockMatches();

    @Accessor(value = "centerOffset", remap = false)
    int[] getCenterOffset();

    @Invoker(value = "setActualRelativeOffset", remap = false)
    BlockPos invokeSetActualRelativeOffset(int x, int y, int z, Direction facing, Direction upwardsFacing, boolean isFlipped);
}