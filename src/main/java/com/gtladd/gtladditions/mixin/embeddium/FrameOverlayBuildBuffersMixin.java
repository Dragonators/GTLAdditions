package com.gtladd.gtladditions.mixin.embeddium;

import com.gtladd.gtladditions.client.render.frame.FrameOverlayTerrainPass;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers", remap = false)
public abstract class FrameOverlayBuildBuffersMixin {

    @Shadow
    @Final
    private Reference2ReferenceOpenHashMap<TerrainRenderPass, BakedChunkModelBuilder> builders;

    @Inject(method = "<init>", require = 1, at = @At("RETURN"))
    private void gtladditions$addFrameBuffers(ChunkVertexType vertexType, CallbackInfo ci) {
        var buffers = new ChunkMeshBufferBuilder[ModelQuadFacing.COUNT];

        for (int facing = 0; facing < buffers.length; facing++) {
            buffers[facing] = FrameOverlayTerrainPass.createBuffer(vertexType);
        }

        builders.put(FrameOverlayTerrainPass.PASS, new BakedChunkModelBuilder(buffers, true));
    }
}