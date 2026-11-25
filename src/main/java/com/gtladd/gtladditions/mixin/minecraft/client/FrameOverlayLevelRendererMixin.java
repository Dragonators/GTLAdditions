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
    private void gtladditions$section(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread, CallbackInfo ci) {
        FrameOverlayRenderer.dirtySection(sectionX, sectionY, sectionZ);
    }

    // Embeddium overwrites these entry points and bypasses vanilla's nested calls.
    // The renderer deduplicates section keys from either backend.
    @Inject(method = "setBlocksDirty", at = @At("HEAD"))
    private void gtladditions$area(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CallbackInfo ci) {
        FrameOverlayRenderer.dirtyArea(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Inject(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Z)V", at = @At("HEAD"))
    private void gtladditions$block(BlockPos pos, boolean reRenderOnMainThread, CallbackInfo ci) {
        FrameOverlayRenderer.dirtyArea(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("HEAD"))
    private void gtladditions$neighbors(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
        for (int sx = sectionX - 1; sx <= sectionX + 1; sx++) {
            for (int sy = sectionY - 1; sy <= sectionY + 1; sy++) {
                for (int sz = sectionZ - 1; sz <= sectionZ + 1; sz++) FrameOverlayRenderer.dirtySection(sx, sy, sz);
            }
        }
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void gtladditions$reload(CallbackInfo ci) {
        FrameOverlayRenderer.requestReset();
    }
}
