package com.gtladd.gtladditions.mixin.minecraft.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import com.gtladd.gtladditions.client.render.frame.FrameOverlayModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public abstract class FrameOverlayModelRendererMixin {

    @Inject(method = "tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void gtladditions$routeFrameOverlay(BlockAndTintGetter arg, BakedModel arg2, BlockState arg3,
                                                BlockPos arg4, PoseStack arg5, VertexConsumer arg6, boolean bl,
                                                RandomSource arg7, long l, int i, ModelData modelData,
                                                RenderType renderType, CallbackInfo ci) {
        if (arg instanceof RenderChunkRegion && FrameOverlayModels.separate(arg3, arg2, renderType)) ci.cancel();
    }
}