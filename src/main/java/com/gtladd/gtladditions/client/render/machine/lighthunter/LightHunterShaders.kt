package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.Minecraft
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

    var paradoxOpaque: ShaderInstance? = null
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
        // The live manager rather than this reload's shader cache: the compute files are read only when first used.
        LightHunterVfxRenderer.setResources(Minecraft.getInstance().resourceManager)
        LightHunterParadoxAssets.invalidate()
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_light_hunter_paradox"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        ) { paradox = it }
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_light_hunter_paradox_opaque"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        ) { paradoxOpaque = it }
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_light_hunter_paradox_model"),
            DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        ) { paradoxModel = it }
        // MC 1.20.1 allocates float[10] uniforms unzeroed and only initializes element 0 via setSafe.
        val zeroArray = FloatArray(10)
        fun zeroUniform(shader: ShaderInstance?, name: String): ShaderInstance? =
            shader?.also { it.getUniform(name)?.set(zeroArray) }

        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_light_hunter_fusion"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        ) { fusion = zeroUniform(it, "FusionNoiseOffset") }
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_light_hunter_interior"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        ) { interior = it }
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_light_hunter"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        ) { material = zeroUniform(it, "PlasmaPhase") }
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_light_hunter_chart_field"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        ) { chartField = zeroUniform(it, "PlasmaPhase") }
        if (GL.getCapabilities().OpenGL33) {
            RenderUtils.registerShader(
                event,
                GTLAdditions.id("gtladditions_light_hunter_fusion_layers"),
                DefaultVertexFormat.POSITION_TEX_COLOR
            ) { fusionLayers = zeroUniform(it, "FusionNoiseOffset") }
            RenderUtils.registerShader(
                event,
                GTLAdditions.id("gtladditions_light_hunter_paradox_layers"),
                DefaultVertexFormat.POSITION_TEX_COLOR
            ) { paradoxLayers = it }
            RenderUtils.registerShader(
                event,
                GTLAdditions.id("gtladditions_light_hunter_layers"),
                DefaultVertexFormat.POSITION_TEX_COLOR
            ) { layers = zeroUniform(it, "PlasmaPhase") }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun registerReload(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(LightHunterParadoxAssets.Reload)
        event.registerReloadListener(ResourceManagerReloadListener { LightHunterVfxRenderer.releaseBuffers() })
    }
}
