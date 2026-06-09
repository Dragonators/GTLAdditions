package com.gtladd.gtladditions.mixin.gtceu.api.pattern;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockPattern.class)
public interface BlockPatternAccessor {

    @Accessor(value = "blockMatches", remap = false)
    TraceabilityPredicate[][][] gtladd$getBlockMatches();
}