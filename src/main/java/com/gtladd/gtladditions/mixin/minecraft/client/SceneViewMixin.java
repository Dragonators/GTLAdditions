package com.gtladd.gtladditions.mixin.minecraft.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;

import com.gtladd.gtladditions.client.render.scene.SceneScheduler;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class SceneViewMixin {

    // Batches flushed after AFTER_BLOCK_ENTITIES (water masks, translucent item sheets) have written depth by now.
    // Embeddium's renderChunkLayer overwrite keeps the HEAD injection.
    @Inject(method = "renderChunkLayer", at = @At("HEAD"))
    private void gtladditions$beforeChunkLayer(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix,
                                               CallbackInfo ci) {
        if (renderType == RenderType.translucent()) SceneScheduler.INSTANCE.beforeTranslucentTerrain();
    }

    // Both call sites (Fabulous with the weather target bound, and the other branch after depthMask(false)): rain stays
    // in front.
    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V"))
    private void gtladditions$beforeWeather(CallbackInfo ci) {
        SceneScheduler.INSTANCE.composeBeforeWeather();
    }
}