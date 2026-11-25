package com.gtladd.gtladditions.mixin.embeddium;

import com.gtladd.gtladditions.client.render.frame.FrameOverlayModels;
import com.gtladd.gtladditions.client.render.frame.FrameOverlayTerrainPass;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.render.chunk.compile.GlobalChunkBuildContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class FrameOverlayBlockRendererMixin {

    @WrapOperation(method = "writeGeometry",
                   require = 1,
                   at = @At(value = "INVOKE",
                            target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push([Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;)V"))
    private void gtladditions$routeFrameOverlay(ChunkMeshBufferBuilder originalBuffer,
                                                ChunkVertexEncoder.Vertex[] vertices, Material material,
                                                Operation<Void> original,
                                                @Local(argsOnly = true) BlockRenderContext ctx,
                                                @Local(argsOnly = true) BakedQuadView quad) {
        var chunk = GlobalChunkBuildContext.get();
        if (chunk != null && FrameOverlayModels.separate(ctx.state(), ctx.model(), ctx.renderLayer())) {
            chunk.buffers.get(FrameOverlayTerrainPass.PASS).getVertexBuffer(quad.getNormalFace())
                    .push(vertices, FrameOverlayTerrainPass.MATERIAL);
        } else {
            original.call(originalBuffer, vertices, material);
        }
    }
}
