package com.gtladd.gtladditions.mixin.embeddium;

import com.gtladd.gtladditions.client.render.frame.FrameOverlayTerrainPass;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask", remap = false)
public abstract class FrameOverlayMeshingTaskMixin {

    @ModifyExpressionValue(method = "execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
                           require = 1,
                           at = @At(value = "FIELD",
                                    opcode = Opcodes.GETSTATIC,
                                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/terrain/DefaultTerrainRenderPasses;ALL:[Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;"))
    private TerrainRenderPass[] gtladditions$includeFramePass(TerrainRenderPass[] original) {
        return FrameOverlayTerrainPass.include(original);
    }
}
