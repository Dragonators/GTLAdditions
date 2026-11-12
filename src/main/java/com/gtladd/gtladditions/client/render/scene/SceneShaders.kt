package com.gtladd.gtladditions.client.render.scene

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
internal object SceneShaders {
    var compose: ShaderInstance? = null
        private set
    var handStencil: ShaderInstance? = null
        private set

    @SubscribeEvent
    @JvmStatic
    fun register(event: RegisterShadersEvent) {
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_scene_depth_compose"),
            DefaultVertexFormat.POSITION_TEX
        ) { compose = it }
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_scene_hand_stencil"),
            DefaultVertexFormat.POSITION_TEX
        ) { handStencil = it }
    }
}