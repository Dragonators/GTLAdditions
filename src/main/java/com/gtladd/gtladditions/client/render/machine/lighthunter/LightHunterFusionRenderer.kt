package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderState
import com.gtladd.gtladditions.client.render.machine.heart.HeartLayerImages
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.FrustumIntersection
import org.joml.Matrix4f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL33C

@OnlyIn(Dist.CLIENT)
object LightHunterFusionRenderer {
    internal val meshes = Meshes()
    private val clipMatrix = Matrix4f()
    private val frustum = FrustumIntersection()

    private fun visible(modelView: Matrix4f, projection: Matrix4f): Boolean =
        frustum.set(clipMatrix.set(projection).mul(modelView)).testAab(
            LightHunterFusionGeometry.MIN_X,
            -LightHunterFusionGeometry.EXTENT,
            -LightHunterFusionGeometry.EXTENT,
            LightHunterFusionGeometry.MAX_X,
            LightHunterFusionGeometry.EXTENT,
            LightHunterFusionGeometry.EXTENT
        )

    class Meshes : AutoCloseable {
        private val builder by lazy { BufferBuilder(1024) }
        private var volume: VertexBuffer? = null
        private val field = LightHunterFusionFieldCache()

        fun setResources(provider: ResourceProvider) {
            close()
            field.setResources(provider)
        }

        fun beginFrame() = field.beginFrame()

        fun buffer(): VertexBuffer = volume ?: run {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
            LightHunterFusionGeometry.write(builder)
            RenderUtils.uploadStaticBuffer(builder).also { volume = it }
        }

        fun prepareField(tick: Float): LightHunterFusionFieldCache.Field? = field.prepare(tick)

        override fun close() {
            volume?.close()
            volume = null
            field.close()
        }
    }

    internal fun drawn(seconds: Float) = seconds > 0f

    /** For a colour-only target; the caller binds the scene depth to [shader]. */
    fun draw(meshes: Meshes, shader: ShaderInstance, modelView: Matrix4f, projection: Matrix4f, seconds: Float, tick: Float) {
        if (!drawn(seconds) || !visible(modelView, projection)) return
        HeartBlackHoleRenderState.withSavedState { render(meshes, shader, modelView, projection, seconds, tick, false) }
    }

    fun drawLayered(
        meshes: Meshes,
        shader: ShaderInstance,
        modelView: Matrix4f,
        projection: Matrix4f,
        seconds: Float,
        tick: Float,
        depth: Int,
        count: Int,
        base: Int,
        rect: IntArray,
        local: Boolean
    ) {
        if (!drawn(seconds) || !visible(modelView, projection)) return
        HeartBlackHoleRenderState.withSavedState {
            HeartLayerImages.bindArray(3, depth)
            shader.getUniform("LayerDepths")?.set(3)
            shader.getUniform("LayerCount")?.set(count)
            shader.getUniform("LayerBase")?.set(base)
            shader.getUniform("LayerMode")?.set(if (local) 1 else 0)
            shader.getUniform("LayerOrigin")?.set(if (local) rect[0].toFloat() else 0f, if (local) rect[1].toFloat() else 0f)
            shader.getUniform("LayerRect")?.set(rect[0].toFloat(), rect[1].toFloat(), rect[2].toFloat(), rect[3].toFloat())
            render(meshes, shader, modelView, projection, seconds, tick, local)
        }
    }

    private fun render(
        meshes: Meshes,
        shader: ShaderInstance,
        modelView: Matrix4f,
        projection: Matrix4f,
        seconds: Float,
        tick: Float,
        writeAlpha: Boolean
    ) {
        shader.getUniform("Time")?.set(tick / 20f)
        shader.getUniform("Seconds")?.set(seconds)
        shader.getUniform("Effect")?.set(0)
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.disableCull()
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GL11C.GL_ONE, GL11C.GL_ONE)
        RenderSystem.colorMask(true, true, true, writeAlpha)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        val activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
        val previousField = GL11C.glGetInteger(GL12C.GL_TEXTURE_BINDING_3D)
        val hasSamplers = GL.getCapabilities().OpenGL33
        val previousSampler = if (hasSamplers) GL11C.glGetInteger(GL33C.GL_SAMPLER_BINDING) else 0
        try {
            val field = if (seconds > 2.5f) meshes.prepareField(tick) else null
            shader.getUniform("FusionField")?.set(2)
            shader.getUniform("UseFusionField")?.set(if (field != null) 1 else 0)
            if (field != null) shader.getUniform("FusionFieldOrigin")?.set(field.origin, field.originSlot)
            RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
            GL11C.glBindTexture(GL12C.GL_TEXTURE_3D, field?.texture ?: 0)
            if (hasSamplers) GL33C.glBindSampler(2, 0)
            val buffer = meshes.buffer()
            RenderUtils.withBoundBuffer(buffer) { buffer.drawWithShader(modelView, projection, shader) }
        } finally {
            shader.clear()
            RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
            GL11C.glBindTexture(GL12C.GL_TEXTURE_3D, previousField)
            if (hasSamplers) GL33C.glBindSampler(2, previousSampler)
            RenderSystem.activeTexture(activeTexture)
        }
    }
}
