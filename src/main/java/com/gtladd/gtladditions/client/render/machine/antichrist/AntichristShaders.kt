package com.gtladd.gtladditions.client.render.machine.antichrist

import com.google.common.collect.ImmutableMap
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@OnlyIn(Dist.CLIENT)
object AntichristShaders {
    private val STAR_SHADER_ID = GTLAdditions.id("gtladditions_antichrist_star")
    private val STAR_OPAQUE_SHADER_ID = GTLAdditions.id("gtladditions_antichrist_star_opaque")
    private val BEAM_SHADER_ID = GTLAdditions.id("gtladditions_antichrist_beam")
    internal val BEAM_VERTEX_FORMAT = VertexFormat(
        ImmutableMap.of(
            "Position",
            DefaultVertexFormat.ELEMENT_POSITION,
            "UV0",
            DefaultVertexFormat.ELEMENT_UV0,
            "UV1",
            DefaultVertexFormat.ELEMENT_UV1
        )
    )

    var starShader: ShaderInstance? = null
        private set

    var starOpaqueShader: ShaderInstance? = null
        private set

    var beamShader: ShaderInstance? = null
        private set

    fun register(event: RegisterShadersEvent) {
        RingBatchShaders.vanilla = null
        try {
            event.registerShader(RingBatchShaders.createVanilla(event.resourceProvider)) { shader -> RingBatchShaders.vanilla = shader }
        } catch (error: Exception) {
            GTLAdditions.LOGGER.warn("FOA vanilla batch shader could not be created; using cached material groups", error)
        }

        RenderUtils.registerShader(event, STAR_SHADER_ID, DefaultVertexFormat.POSITION_TEX) { shader -> starShader = shader }
        RenderUtils.registerShader(event, STAR_OPAQUE_SHADER_ID, DefaultVertexFormat.POSITION_TEX) { shader -> starOpaqueShader = shader }

        RenderUtils.registerShader(event, BEAM_SHADER_ID, BEAM_VERTEX_FORMAT) { shader -> beamShader = shader }
    }
}

@Suppress("unused")
@Mod.EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = [Dist.CLIENT]
)
object AntichristShaderEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(
            ResourceManagerReloadListener {
                if (RenderSystem.isOnRenderThread()) {
                    RingStructureVertexBuffer.releaseBuffers()
                } else {
                    RenderSystem.recordRenderCall { RingStructureVertexBuffer.releaseBuffers() }
                }
            }
        )
    }

    @SubscribeEvent
    @JvmStatic
    fun onRegisterShaders(event: RegisterShadersEvent) {
        AntichristShaders.register(event)
    }
}