package com.gtladd.gtladditions.mixin.minecraft.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.gtladd.gtladditions.client.render.BlockEntityDispatch;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Oculus wraps the buffer source at {@code isValid}, after HEAD, so the caller's own source is seen here. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void gtladditions$beginDispatch(BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                            CallbackInfo ci) {
        BlockEntityDispatch.begin(blockEntity, bufferSource);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void gtladditions$endDispatch(CallbackInfo ci) {
        BlockEntityDispatch.end();
    }
}
