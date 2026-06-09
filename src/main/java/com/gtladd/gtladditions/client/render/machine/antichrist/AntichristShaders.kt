package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@OnlyIn(Dist.CLIENT)
object AntichristShaders {
    private val STAR_SHADER_ID = GTLAdditions.id("gtladditions_antichrist_star")
    private val BEAM_SHADER_ID = GTLAdditions.id("gtladditions_antichrist_beam")

    var starShader: ShaderInstance? = null
        private set

    var beamShader: ShaderInstance? = null
        private set

    fun register(event: RegisterShadersEvent) {
        event.registerShader(
            ShaderInstance(event.resourceProvider, STAR_SHADER_ID, DefaultVertexFormat.POSITION_TEX)
        ) { shader -> starShader = shader }

        event.registerShader(
            ShaderInstance(event.resourceProvider, BEAM_SHADER_ID, DefaultVertexFormat.POSITION)
        ) { shader -> beamShader = shader }
    }
}

@Suppress("unused")
@Mod.EventBusSubscriber(
    modid = GTLAdditions.Companion.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = [Dist.CLIENT]
)
object AntichristShaderEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterShaders(event: RegisterShadersEvent) {
        AntichristShaders.register(event)
    }
}