package com.gtladd.gtladditions.client

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class GTLAddRenderTypes(
    name: String, format: VertexFormat, mode: VertexFormat.Mode, bufferSize: Int,
    affectsCrumbling: Boolean, sortOnUpload: Boolean, setupState: Runnable, clearState: Runnable
) : RenderType(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState) {

    companion object {
        fun createGlowLayer(texture: ResourceLocation): RenderType {
            val state = CompositeState.builder()
                .setShaderState(RENDERTYPE_EYES_SHADER)
                .setTextureState(TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true)

            return create(
                "gtladditions_glow_layer",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                state
            )
        }

        fun createFullBrightGlowLayer(texture: ResourceLocation): RenderType {
            val state = CompositeState.builder()
                .setShaderState(RENDERTYPE_EYES_SHADER)
                .setTextureState(TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(NO_LIGHTMAP) // Ignore world lighting - always full bright
                .setOverlayState(OVERLAY)
                .createCompositeState(true)

            return create(
                "gtladditions_full_bright_glow_layer",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                state
            )
        }
    }
}