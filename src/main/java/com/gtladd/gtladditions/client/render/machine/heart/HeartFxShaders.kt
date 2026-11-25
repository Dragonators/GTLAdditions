package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.lwjgl.opengl.GL20C

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object HeartFxShaders {
    var shellFx: ShaderInstance? = null
        private set
    var craft: ShaderInstance? = null
        private set
    var craftInstanceAttributes: IntArray? = null
        private set
    var starkiller: ShaderInstance? = null
        private set
    var glow: ShaderInstance? = null
        private set
    var line: ShaderInstance? = null
        private set

    @SubscribeEvent
    @JvmStatic
    fun register(event: RegisterShadersEvent) {
        HeartShellTerrain.register(event)
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_heart_shell_fx"),
            DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        ) { shellFx = it }
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_heart_craft"),
            HeartFleetAssets.CRAFT_SHADER_FORMAT
        ) { shader ->
            craft = shader
            craftInstanceAttributes = shader?.let { program ->
                IntArray(8) { i -> GL20C.glGetAttribLocation(program.id, if (i < 4) "InstanceModel$i" else "InstanceView${i - 4}") }
                    .takeIf { attributes -> attributes.all { it >= 4 } }
            }
        }

        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_heart_starkiller"),
            HeartFleetAssets.STARKILLER_FORMAT
        ) { starkiller = it }
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_heart_glow"),
            DefaultVertexFormat.POSITION_TEX_COLOR
        ) { glow = it }
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_heart_line"),
            DefaultVertexFormat.POSITION_COLOR
        ) { line = it }
    }
}
