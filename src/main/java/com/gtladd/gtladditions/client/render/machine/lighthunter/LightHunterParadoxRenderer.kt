package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleAppearanceTextures
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderState
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceProvider
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

    class Meshes @JvmOverloads constructor(private var resources: ResourceProvider? = null) : AutoCloseable {
        private val builder by lazy { BufferBuilder(256) }
        private var window: VertexBuffer? = null
        private var models: LightHunterParadoxModels? = null

        fun setResources(provider: ResourceProvider) {
            close()
            resources = provider
        }

        fun beginFrame() {
            models?.beginFrame()
        }

        fun touch(key: Any) {
            models?.touch(key)
        }

        fun buffer(): VertexBuffer = window ?: run {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
            LightHunterParadoxGeometry.write(builder)
            RenderUtils.uploadStaticBuffer(builder).also { window = it }
        }

        fun models(): LightHunterParadoxModels = models ?: LightHunterParadoxModels(requireNotNull(resources)).also { models = it }

        override fun close() {
            window?.close()
            window = null
            models?.close()
            models = null
        }
    }

    fun drawOpaque(
        meshes: Meshes,
        shader: ShaderInstance,
        modelShader: ShaderInstance,
        modelView: Matrix4f,
        projection: Matrix4f,
        seconds: Float,
        animationTick: Float,
        key: Any
    ) {
        if (LightHunterParadoxTiming.window(seconds).reveal < 1f) return
        draw(meshes, shader, modelShader, modelView, projection, seconds, animationTick, key, false, true, WINDOW_OPAQUE)
    }

    /** The game and the GL validation harness share this draw path and all model resources. */
    @JvmOverloads
    fun draw(
        meshes: Meshes,
        shader: ShaderInstance,
        modelShader: ShaderInstance,
        modelView: Matrix4f,
        projection: Matrix4f,
        seconds: Float,
        animationTick: Float,
        key: Any,
        layered: Boolean,
        writeAlpha: Boolean,
        windowPass: Int = WINDOW_COMPLETE
    ) {
        if (seconds < 4.3f || !HeartBlackHoleAppearanceTextures.setGalaxySampler(shader)) return
        val samplerObjects = GL.getCapabilities().OpenGL33
        val samplers = if (samplerObjects) IntArray(3) { GL33C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, it) } else IntArray(0)
        HeartBlackHoleRenderState.withSavedState {
            try {
                samplers.indices.forEach { GL33C.glBindSampler(it, 0) }
                val state = LightHunterParadoxTiming.window(seconds)
                // A dropout hides the signal, not the visible ship's cached render target.
                meshes.touch(key)
                val windowView = LightHunterParadoxModels.windowView(modelView)
                val eye = windowView.eye
                val angle = ((animationTick.toDouble() / 20.0 * LightHunterParadoxTiming.ROTATION_DEGREES_PER_SECOND) % 360.0 * PI / 180.0)
                    .toFloat()
                val texture = if (state.reveal > 0f) meshes.models().prepare(key, modelShader, eye, angle) else 0
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
                RenderSystem.enableBlend()
                RenderSystem.colorMask(true, true, true, writeAlpha)
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                RenderSystem.blendFuncSeparate(GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA)
                val buffer = meshes.buffer()
                RenderUtils.withBoundBuffer(buffer) { buffer.drawWithShader(modelView, projection, shader) }
                shader.clear()
            } finally {
                samplers.forEachIndexed { unit, sampler -> GL33C.glBindSampler(unit, sampler) }
            }
        }
    }
}