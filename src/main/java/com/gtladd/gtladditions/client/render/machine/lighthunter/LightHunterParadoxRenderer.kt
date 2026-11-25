package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderState
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleTextures
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL33C
import kotlin.math.PI

@OnlyIn(Dist.CLIENT)
object LightHunterParadoxRenderer {
    const val WINDOW_COMPLETE = 0
    const val WINDOW_OPAQUE = 1
    const val WINDOW_TRANSLUCENT = 2
    internal val meshes = Meshes()

    class Meshes : AutoCloseable {
        private val builder by lazy { BufferBuilder(256) }
        private var window: VertexBuffer? = null
        private val models = LightHunterParadoxModels()

        fun buffer(): VertexBuffer = window ?: run {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
            LightHunterParadoxGeometry.write(builder)
            RenderUtils.uploadStaticBuffer(builder).also { window = it }
        }

        fun models(): LightHunterParadoxModels = models

        override fun close() {
            window?.close()
            window = null
            models.close()
        }
    }

    fun releaseModel(key: Any) {
        if (RenderSystem.isOnRenderThread()) {
            meshes.models().remove(key)
        } else {
            RenderSystem.recordRenderCall { meshes.models().remove(key) }
        }
    }

    fun drawOpaque(
        meshes: Meshes,
        shader: ShaderInstance,
        modelShader: ShaderInstance,
        modelView: Matrix4f,
        projection: Matrix4f,
        seconds: Float,
        animationTick: Double,
        key: Any
    ): Boolean {
        if (LightHunterParadoxTiming.window(seconds).reveal < 1f) return false
        return draw(meshes, shader, modelShader, modelView, projection, seconds, animationTick, key, false, true, WINDOW_OPAQUE)
    }

    internal fun windowDrawn(seconds: Float) = seconds >= 4.3f

    internal fun windowOutcome(drawn: Boolean, seconds: Float): PartOutcome? = when {
        drawn || !windowDrawn(seconds) -> null
        HeartBlackHoleTextures.pending -> PartOutcome.PENDING
        LightHunterParadoxTiming.window(seconds).reveal > 0f && LightHunterParadoxAssets.pending -> PartOutcome.PENDING
        else -> PartOutcome.RESOURCE_UNAVAILABLE
    }

    fun draw(
        meshes: Meshes,
        shader: ShaderInstance,
        modelShader: ShaderInstance,
        modelView: Matrix4f,
        projection: Matrix4f,
        seconds: Float,
        animationTick: Double,
        key: Any,
        layered: Boolean,
        writeAlpha: Boolean,
        windowPass: Int
    ): Boolean {
        if (!windowDrawn(seconds)) return false
        val state = LightHunterParadoxTiming.window(seconds)
        val assets = if (state.reveal > 0f) LightHunterParadoxAssets.current ?: return false else null
        if (!HeartBlackHoleTextures.setGalaxySampler(shader)) return false
        val samplerObjects = GL.getCapabilities().OpenGL33
        val samplers = if (samplerObjects) IntArray(3) { GL33C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, it) } else IntArray(0)
        HeartBlackHoleRenderState.withSavedState {
            try {
                samplers.indices.forEach { GL33C.glBindSampler(it, 0) }
                val windowView = LightHunterParadoxModels.windowView(modelView)
                val eye = windowView.eye
                val angle = ((animationTick / 20.0 * LightHunterParadoxTiming.ROTATION_DEGREES_PER_SECOND) % 360.0 * PI / 180.0)
                    .toFloat()
                val texture = if (assets != null) meshes.models().prepare(key, modelShader, assets, eye, angle) else 0
                shader.setSampler("PortalMap", texture)
                shader.getUniform("WindowEye")?.set(eye.x, eye.y, eye.z)
                shader.getUniform("WindowPlane")?.set(LightHunterParadoxTiming.WINDOW_X)
                shader.getUniform("WindowBias")?.set(windowView.depthBias)
                shader.getUniform("WindowPass")?.set(windowPass)
                shader.getUniform("Signal")?.set(
                    state.reveal,
                    state.glitch,
                    state.frame,
                    LightHunterParadoxTiming.smooth(4.3f, 4.8f, seconds) * state.rim
                )
                if (layered) RenderSystem.disableDepthTest() else RenderSystem.enableDepthTest()
                RenderSystem.depthFunc(GL11C.GL_LEQUAL)
                RenderSystem.depthMask(windowPass == WINDOW_OPAQUE)
                RenderSystem.disableCull()
                // The solid pass keeps only fragments with alpha >= 0.999 and draws them unblended.
                if (windowPass == WINDOW_OPAQUE) {
                    RenderSystem.disableBlend()
                } else {
                    RenderSystem.enableBlend()
                    RenderSystem.blendFuncSeparate(GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA)
                }

                RenderSystem.colorMask(true, true, true, writeAlpha)
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                val buffer = meshes.buffer()
                RenderUtils.withBoundBuffer(buffer) { buffer.drawWithShader(modelView, projection, shader) }
                shader.clear()
            } finally {
                samplers.forEachIndexed { unit, sampler -> GL33C.glBindSampler(unit, sampler) }
            }
        }

        return true
    }
}
