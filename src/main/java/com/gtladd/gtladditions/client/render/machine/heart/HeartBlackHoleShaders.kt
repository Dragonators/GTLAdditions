package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.irisshaders.iris.gl.blending.DepthColorStorage
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@OnlyIn(Dist.CLIENT)
internal object HeartBlackHoleShaders {
    private val VOLUME_SHADER_ID = GTLAdditions.id("gtladditions_heart_black_hole")
    private val DOWNSAMPLE_SHADER_ID = GTLAdditions.id("gtladditions_heart_downsample")
    private val BLUR_SHADER_ID = GTLAdditions.id("gtladditions_heart_blur")
    private val COMPOSITE_SHADER_ID = GTLAdditions.id("gtladditions_heart_composite")
    private val HAND_MASK_SHADER_ID = GTLAdditions.id("gtladditions_heart_hand_mask")

    var volumeShader: ShaderInstance? = null
        private set

    var handMaskShader: ShaderInstance? = null
        private set

    var blurShader: ShaderInstance? = null
        private set

    var downsampleShader: ShaderInstance? = null
        private set

    var compositeShader: ShaderInstance? = null
        private set

    var layerCopyShader: ShaderInstance? = null
        private set

    var layerDepthShader: ShaderInstance? = null
        private set

    var layerResolveShader: ShaderInstance? = null
        private set

    fun register(event: RegisterShadersEvent) {
        val lookupTables = HeartBlackHoleLookupTextures.readResources(event.resourceProvider)
        val appearance = HeartBlackHoleAppearanceTextures.readResources(event.resourceProvider)
        event.registerShader(
            ShaderInstance(event.resourceProvider, VOLUME_SHADER_ID, DefaultVertexFormat.POSITION)
        ) { shader ->
            HeartBlackHoleLookupTextures.upload(lookupTables)
            HeartBlackHoleAppearanceTextures.upload(appearance)
            volumeShader = shader
        }

        event.registerShader(
            ShaderInstance(event.resourceProvider, DOWNSAMPLE_SHADER_ID, DefaultVertexFormat.POSITION_TEX)
        ) { shader -> downsampleShader = shader }

        event.registerShader(
            ShaderInstance(event.resourceProvider, BLUR_SHADER_ID, DefaultVertexFormat.POSITION_TEX)
        ) { shader -> blurShader = shader }

        event.registerShader(
            ShaderInstance(event.resourceProvider, COMPOSITE_SHADER_ID, DefaultVertexFormat.POSITION_TEX)
        ) { shader -> compositeShader = shader }

        event.registerShader(
            OffscreenShader(event.resourceProvider, HAND_MASK_SHADER_ID, DefaultVertexFormat.POSITION_TEX)
        ) { shader -> handMaskShader = shader }

        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_heart_layer_copy"),
            DefaultVertexFormat.POSITION_TEX
        )
        ) { layerCopyShader = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_heart_layer_depth"),
            DefaultVertexFormat.POSITION_TEX
        )
        ) { layerDepthShader = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_heart_layer_resolve"),
            DefaultVertexFormat.POSITION_TEX)
        ) { layerResolveShader = it }
    }

    /** Iris suppresses ordinary custom world shaders; this shader only writes the owned hand mask. */
    private class OffscreenShader(
        resources: ResourceProvider,
        id: ResourceLocation,
        format: VertexFormat
    ) : ShaderInstance(resources, id, format) {
        override fun apply() {
            super.apply()
            if (DepthColorStorage.isDepthColorLocked()) DepthColorStorage.unlockDepthColor()
        }
    }
}

@Suppress("unused")
@Mod.EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = [Dist.CLIENT]
)
internal object HeartBlackHoleShaderEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterShaders(event: RegisterShadersEvent) {
        HeartBlackHoleShaders.register(event)
    }
}