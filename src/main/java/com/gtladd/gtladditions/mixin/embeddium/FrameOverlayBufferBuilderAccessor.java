package com.gtladd.gtladditions.mixin.embeddium;

import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ChunkMeshBufferBuilder.class, remap = false)
public interface FrameOverlayBufferBuilderAccessor {

    @Accessor("encoder")
    ChunkVertexEncoder getEncoder();
}
