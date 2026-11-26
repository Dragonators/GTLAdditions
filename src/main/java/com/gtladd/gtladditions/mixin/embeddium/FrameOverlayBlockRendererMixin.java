package com.gtladd.gtladditions.mixin.embeddium;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.gtladd.gtladditions.client.render.frame.FrameOverlayModels;
import com.gtladd.gtladditions.client.render.frame.FrameOverlayTerrainPass;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = BlockRenderer.class, remap = false)
public abstract class FrameOverlayBlockRendererMixin {

    @WrapOperation(method = "renderModel(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;" + "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;)V",
                   require = 2,
                   allow = 2,
                   at = @At(value = "INVOKE",
                            target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderQuadList(" + "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;" + "Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;" + "Lme/jellysquid/mods/sodium/client/model/light/LightPipeline;" + "Lme/jellysquid/mods/sodium/client/model/color/ColorProvider;" + "Lnet/minecraft/world/phys/Vec3;" + "Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;" + "Ljava/util/List;Lnet/minecraft/core/Direction;)V"))
    private void gtladditions$routeFrameOverlay(BlockRenderer renderer, BlockRenderContext ctx, Material material,
                                                LightPipeline light, ColorProvider<BlockState> colorProvider,
                                                Vec3 offset, ChunkModelBuilder builder, List<BakedQuad> quads, Direction face,
                                                Operation<Void> original,
                                                @Local(argsOnly = true) ChunkBuildBuffers buffers) {
        if (FrameOverlayModels.separate(ctx.state(), ctx.model(), ctx.renderLayer())) {
            material = FrameOverlayTerrainPass.MATERIAL;
            builder = buffers.get(FrameOverlayTerrainPass.PASS);
        }
        original.call(renderer, ctx, material, light, colorProvider, offset, builder, quads, face);
    }
}
