package com.gtladd.gtladditions.mixin.gtlcore.client;

import org.gtlcore.gtlcore.client.preview.SparsePreviewShape;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Fills an empty shape with a cut of another, as the class only builds from a full shape info. */
@Mixin(value = SparsePreviewShape.class, remap = false)
public interface SparsePreviewShapeAccessor {

    @Mutable
    @Accessor("positions")
    void setPositions(long[] positions);

    @Mutable
    @Accessor("blocks")
    void setBlocks(BlockInfo[] blocks);

    @Mutable
    @Accessor("controller")
    void setController(BlockPos controller);

    @Mutable
    @Accessor("height")
    void setHeight(int height);
}