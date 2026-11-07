package com.gtladd.gtladditions.mixin.minecraft.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;

import com.gtladd.gtladditions.client.render.frame.FrameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1100)
public abstract class FrameOverlayLevelRendererMixin {

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void gtladditions$section(int x, int y, int z, boolean important, CallbackInfo ci) {
        FrameOverlayRenderer.dirtySection(x, y, z);
    }

    // Embeddium overwrites these entry points and bypasses vanilla's nested calls.
    // The renderer deduplicates section keys from either backend.
    @Inject(method = "setBlocksDirty", at = @At("HEAD"))
    private void gtladditions$area(int x0, int y0, int z0, int x1, int y1, int z1, CallbackInfo ci) {
        FrameOverlayRenderer.dirtyArea(x0, y0, z0, x1, y1, z1);
    }

    @Inject(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Z)V", at = @At("HEAD"))
    private void gtladditions$block(BlockPos pos, boolean important, CallbackInfo ci) {
        FrameOverlayRenderer.dirtyArea(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("HEAD"))
    private void gtladditions$neighbors(int x, int y, int z, CallbackInfo ci) {
        for (int sx = x - 1; sx <= x + 1; sx++) {
            for (int sy = y - 1; sy <= y + 1; sy++) {
                for (int sz = z - 1; sz <= z + 1; sz++) FrameOverlayRenderer.dirtySection(sx, sy, sz);
            }
        }
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void gtladditions$reload(CallbackInfo ci) {
        FrameOverlayRenderer.requestReset();
    }
}