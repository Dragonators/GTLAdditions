package com.gtladd.gtladditions.mixin.minecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

import com.gtladd.gtladditions.client.render.frame.FrameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class FrameOverlayClientLevelMixin {

    @Inject(method = "onChunkLoaded", at = @At("RETURN"))
    private void gtladditions$loaded(ChunkPos pos, CallbackInfo ci) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (level != Minecraft.getInstance().level) return;
        LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        if (chunk != null) FrameOverlayRenderer.chunkLoaded(chunk);
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void gtladditions$unloaded(LevelChunk chunk, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().level) {
            FrameOverlayRenderer.chunkUnloaded(chunk.getPos().x, chunk.getPos().z);
        }
    }
}