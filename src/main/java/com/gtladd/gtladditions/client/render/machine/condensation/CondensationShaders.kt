package com.gtladd.gtladditions.client.render.machine.condensation

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
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
object CondensationShaders {
    var core: ShaderInstance? = null
        private set

    @SubscribeEvent
    @JvmStatic
    fun register(event: RegisterShadersEvent) {
        RenderUtils.registerShader(event, GTLAdditions.id("gtladditions_condensation_core"), DefaultVertexFormat.POSITION) { core = it }
    }

    @SubscribeEvent
    @JvmStatic
    fun registerReload(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(
            ResourceManagerReloadListener {
                if (RenderSystem.isOnRenderThread()) {
                    CondensationCoreRenderer.releaseBuffers()
                } else {
                    RenderSystem.recordRenderCall { CondensationCoreRenderer.releaseBuffers() }
                }
            }
        )
    }
}