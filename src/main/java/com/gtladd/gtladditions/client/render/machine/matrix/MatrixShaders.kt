package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
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
    var seal: ShaderInstance? = null
        private set
    var sealLayers: ShaderInstance? = null
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
        MatrixEyeTextures.invalidate()
        RenderUtils.registerShader(event, GTLAdditions.id("gtladditions_matrix_seal"), DefaultVertexFormat.POSITION_COLOR) { seal = it }
        RenderUtils.registerShader(event, GTLAdditions.id("gtladditions_matrix_seal_layers"), DefaultVertexFormat.POSITION_COLOR) {
            sealLayers = it
        }

        RenderUtils.registerShader(event, GTLAdditions.id("gtladditions_matrix_energy_core"), DefaultVertexFormat.POSITION) {
            energyCore = it
        }

        RenderUtils.registerShader(event, GTLAdditions.id("gtladditions_matrix_energy_core_layers"), DefaultVertexFormat.POSITION) {
            energyCoreLayers = it
        }

        RenderUtils.registerShader(event, GTLAdditions.id("gtladditions_matrix_eye"), DefaultVertexFormat.POSITION) { eye = it }
        RenderUtils.registerShader(event, GTLAdditions.id("gtladditions_matrix_eye_lid"), DefaultVertexFormat.POSITION) { eyeLid = it }
        RenderUtils.registerShader(event, GTLAdditions.id("gtladditions_matrix_eye_mist"), DefaultVertexFormat.POSITION) { eyeMist = it }
        RenderUtils.registerShader(event, GTLAdditions.id("gtladditions_matrix_iris_star"), MatrixIrisStarRenderer.CIRCLE_FORMAT) {
            irisStar = it
        }

        RenderUtils.registerShader(event, GTLAdditions.id("gtladditions_matrix_beam_glow"), AntichristShaders.BEAM_VERTEX_FORMAT) {
            irisBeamGlow = it
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun registerReload(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(MatrixEyeTextures.Reload)
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
