package com.gtladd.gtladditions.mixin.gtceu.client;

import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;

import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MultiblockInWorldPreviewRenderer.class, remap = false)
public interface MultiblockInWorldPreviewRendererAccessor {

    @Accessor("LAST_POS")
    static BlockPos getLastPos() {
        throw new AssertionError();
    }
}
