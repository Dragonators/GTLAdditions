package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.lwjgl.opengl.GL

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object LightHunterShaders {
    var material: ShaderInstance? = null
        private set
    var chartField: ShaderInstance? = null
        private set
    var layers: ShaderInstance? = null
        private set
    var interior: ShaderInstance? = null
        private set
    var paradox: ShaderInstance? = null
        private set
    var paradoxLayers: ShaderInstance? = null
        private set
    var paradoxModel: ShaderInstance? = null
        private set
    var fusion: ShaderInstance? = null
        private set
    var fusionLayers: ShaderInstance? = null
        private set

    @SubscribeEvent
    @JvmStatic
    fun register(event: RegisterShadersEvent) {
        LightHunterVfxRenderer.setResources(event.resourceProvider)
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_light_hunter_paradox"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        )
        ) { paradox = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_light_hunter_paradox_model"),
            DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        )
        ) { paradoxModel = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_light_hunter_fusion"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        )
        ) { fusion = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_light_hunter_interior"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        )
        ) { interior = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_light_hunter"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        )
        ) { material = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_light_hunter_chart_field"),
            DefaultVertexFormat.POSITION_TEX_COLOR)
        ) { chartField = it }
        if (GL.getCapabilities().OpenGL33) {
            event.registerShader(
                ShaderInstance(event.resourceProvider,
                GTLAdditions.id("gtladditions_light_hunter_fusion_layers"),
                DefaultVertexFormat.POSITION_TEX_COLOR
            )
            ) { fusionLayers = it }
            event.registerShader(
                ShaderInstance(event.resourceProvider,
                GTLAdditions.id("gtladditions_light_hunter_paradox_layers"),
                DefaultVertexFormat.POSITION_TEX_COLOR
            )
            ) { paradoxLayers = it }
            event.registerShader(
                ShaderInstance(event.resourceProvider,
                GTLAdditions.id("gtladditions_light_hunter_layers"),
                DefaultVertexFormat.POSITION_TEX_COLOR)
            ) { layers = it }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun registerReload(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(ResourceManagerReloadListener { LightHunterVfxRenderer.releaseBuffers() })
    }
}