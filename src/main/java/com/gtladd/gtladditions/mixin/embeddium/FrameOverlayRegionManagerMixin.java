package com.gtladd.gtladditions.mixin.embeddium;

import com.gtladd.gtladditions.client.render.frame.FrameOverlayTerrainPass;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RenderRegionManager.class, remap = false)
public abstract class FrameOverlayRegionManagerMixin {

    @ModifyExpressionValue(method = "uploadMeshes(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;Lme/jellysquid/mods/sodium/client/render/chunk/region/RenderRegion;Ljava/util/Collection;)V",
                           require = 1,
                           at = @At(value = "FIELD",
                                    opcode = Opcodes.GETSTATIC,
                                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/terrain/DefaultTerrainRenderPasses;ALL:[Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;"))
    private TerrainRenderPass[] gtladditions$includeFramePass(TerrainRenderPass[] original) {
        return FrameOverlayTerrainPass.include(original);
    }
}
