package com.gtladd.gtladditions.client

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.function.Supplier

@Suppress("INFERRED_INVISIBLE_RETURN_TYPE_WARNING")
@OnlyIn(Dist.CLIENT)
class GTLAddRenderTypes(
    name: String,
    format: VertexFormat,
    mode: VertexFormat.Mode,
    bufferSize: Int,
    affectsCrumbling: Boolean,
    sortOnUpload: Boolean,
    setupState: Runnable,
    clearState: Runnable
) : RenderType(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState) {

    companion object {
        fun createAstrographStarLayer(texture: ResourceLocation, additive: Boolean, shader: Supplier<ShaderInstance?>): RenderType {
            val state = CompositeState.builder()
                .setShaderState(ShaderStateShard(shader))
                .setTextureState(TextureStateShard(texture, false, false))
                .setTransparencyState(if (additive) LIGHTNING_TRANSPARENCY else NO_TRANSPARENCY)
                .setWriteMaskState(if (additive) COLOR_WRITE else COLOR_DEPTH_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .createCompositeState(false)

            return create(
                "gtladditions_astrograph_star",
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                524288,
                false,
                false,
                state
            )
        }

        /** Frame bases already own depth. Decals are composed before glass/water, including Fabulous. */
        fun createFrameOverlayLayer(): RenderType = create(
            "gtladditions_frame_overlay",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            32768,
            false,
            false,
            CompositeState.builder()
                .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                .setTextureState(BLOCK_SHEET_MIPPED)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .setCullState(CULL)
                .setLightmapState(LIGHTMAP)
                .setLayeringState(POLYGON_OFFSET_LAYERING)
                .setOutputState(MAIN_TARGET)
                .createCompositeState(false)
        )

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