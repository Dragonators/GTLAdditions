package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristShaders
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object MatrixShaders {
    var magicCircle: ShaderInstance? = null
        private set
    var seal: ShaderInstance? = null
        private set
    var energyCore: ShaderInstance? = null
        private set
    var energyCoreLayers: ShaderInstance? = null
        private set
    var eye: ShaderInstance? = null
        private set
    var eyeLid: ShaderInstance? = null
        private set
    var eyeMist: ShaderInstance? = null
        private set
    var irisStar: ShaderInstance? = null
        private set
    var irisBeamGlow: ShaderInstance? = null
        private set

    @SubscribeEvent
    @JvmStatic
    fun register(event: RegisterShadersEvent) {
        event.registerShader(
            ShaderInstance(event.resourceProvider, GTLAdditions.id("gtladditions_matrix_magic_circle"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
        ) { magicCircle = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider, GTLAdditions.id("gtladditions_matrix_seal"), DefaultVertexFormat.POSITION_COLOR)
        ) { seal = it
        }
        event.registerShader(
            ShaderInstance(event.resourceProvider, GTLAdditions.id("gtladditions_matrix_energy_core"), DefaultVertexFormat.POSITION)
        ) {
            energyCore = it
        }
        event.registerShader(
            ShaderInstance(event.resourceProvider, GTLAdditions.id("gtladditions_matrix_energy_core_layers"), DefaultVertexFormat.POSITION)
        ) {
            energyCoreLayers = it
        }
        event.registerShader(
            ShaderInstance(event.resourceProvider, GTLAdditions.id("gtladditions_matrix_eye"), DefaultVertexFormat.POSITION)
        ) { eye = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider, GTLAdditions.id("gtladditions_matrix_eye_lid"), DefaultVertexFormat.POSITION)
        ) { eyeLid = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider, GTLAdditions.id("gtladditions_matrix_eye_mist"), DefaultVertexFormat.POSITION)
        ) { eyeMist = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider, GTLAdditions.id("gtladditions_matrix_iris_star"), MatrixIrisStarRenderer.CIRCLE_FORMAT)
        ) {
            irisStar = it
        }
        event.registerShader(
            ShaderInstance(event.resourceProvider, GTLAdditions.id("gtladditions_matrix_beam_glow"), AntichristShaders.BEAM_VERTEX_FORMAT)
        ) {
            irisBeamGlow = it
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun registerReload(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(
            ResourceManagerReloadListener {
                if (RenderSystem.isOnRenderThread()) {
                    MatrixEffectRenderer.releaseBuffers()
                } else {
                    RenderSystem.recordRenderCall { MatrixEffectRenderer.releaseBuffers() }
                }
            }
        )
    }
}