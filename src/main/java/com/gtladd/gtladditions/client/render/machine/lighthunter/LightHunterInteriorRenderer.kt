package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11C
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
object LightHunterInteriorRenderer {
    const val FAN_REVOLUTION_TICKS = 80f
    const val DISPLAY_SCAN_TICKS = 160f
    private val meshes = Meshes()

    internal fun draw(modelView: Matrix4f, projection: Matrix4f, tick: Float): Boolean {
        val shader = LightHunterShaders.interior ?: return false
        draw(meshes, shader, modelView, projection, tick)
        return true
    }

    fun draw(meshes: Meshes, shader: ShaderInstance, modelView: Matrix4f, projection: Matrix4f, tick: Float) {
        RenderUtils.withSavedRenderState {
            RenderSystem.enableDepthTest()
            RenderSystem.depthFunc(GL11C.GL_LEQUAL)
            RenderSystem.depthMask(true)
            RenderSystem.enableCull()
            RenderSystem.disableBlend()
            val angle = (tick % FAN_REVOLUTION_TICKS) * (2 * PI / FAN_REVOLUTION_TICKS).toFloat()
            shader.getUniform("FanRotation")?.set(cos(angle), sin(angle))
            shader.getUniform("FanCenters")?.set(LightHunterInteriorGeometry.fanPositions[0], LightHunterInteriorGeometry.fanPositions[1])
            shader.getUniform("DisplayTime")?.set((tick % DISPLAY_SCAN_TICKS) / 20f)
            val buffer = meshes.buffer()
            RenderUtils.withBoundBuffer(buffer) { buffer.drawWithShader(modelView, projection, shader) }
        }
    }

    fun close() {
        meshes.close()
    }

    class Meshes : AutoCloseable {
        private val builder by lazy { BufferBuilder(262144) }
        private var buffer: VertexBuffer? = null

        fun buffer(): VertexBuffer = buffer ?: run {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
            LightHunterInteriorGeometry.write(builder)
            RenderUtils.uploadStaticBuffer(builder).also { buffer = it }
        }

        override fun close() {
            buffer?.close()
            buffer = null
        }
    }
}
