package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderState
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceProvider
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import java.io.DataInputStream
import java.util.IdentityHashMap
import java.util.zip.GZIPInputStream
import kotlin.math.PI
import kotlin.math.abs

/** Shared mesh/texture assets; per-visible-ship images are reused across the compositor's passes. */
class LightHunterParadoxModels(private val resources: ResourceProvider) : AutoCloseable {
    private data class Part(val buffer: VertexBuffer, val texture: DynamicTexture?)
    private class Frame {
        val target = TextureTarget(512, 512, true, false)
        var used = -1L
        var angle = Float.NaN
        val eye = Vector3f(Float.NaN)
    }

    private val deathStar by lazy { read("death_star") }
    private val kamino by lazy { read("kamino") }
    private val parts = ArrayList<Part>()
    private val frames = IdentityHashMap<Any, Frame>()
    private var serial = 0L

    fun beginFrame() {
        serial++
        val iterator = frames.values.iterator()
        while (iterator.hasNext()) {
            val frame = iterator.next()
            if (serial - frame.used > 2) {
                frame.target.destroyBuffers()
                iterator.remove()
            }
        }
    }

    fun touch(key: Any) {
        frames[key]?.used = serial
    }

    fun prepare(
        key: Any,
        shader: ShaderInstance,
        eye: Vector3f,
        angle: Float
    ): Int = HeartBlackHoleRenderState.withSavedState {
        val frame = frames.getOrPut(key) { Frame() }
        frame.used = serial
        if (frame.angle != angle || frame.eye != eye) {
            frame.target.setClearColor(0f, 0f, 0f, 0f)
            RenderSystem.colorMask(true, true, true, true)
            RenderSystem.depthMask(true)
            frame.target.clear(false)
            frame.target.bindWrite(true)
            RenderSystem.enableDepthTest()
            RenderSystem.depthFunc(GL11C.GL_LEQUAL)
            RenderSystem.disableBlend()
            RenderSystem.enableCull()
            GL11C.glFrontFace(GL11C.GL_CCW)
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            val near = 0.05f
            val z = maxOf(0.05f, eye.z)
            val projection = Matrix4f().setFrustum(
                (-1f - eye.x) * near / z,
                (1f - eye.x) * near / z,
                (-1f - eye.y) * near / z,
                (1f - eye.y) * near / z,
                near,
                100f
            )
            val view = Matrix4f().translation(-eye.x, -eye.y, -z)
            shader.getUniform("Eye")?.set(eye.x, eye.y, z)
            draw(shader, kamino, Matrix4f().translation(-1.8f, 1f, -7.5f).scale(2.5f), view, projection, 1)
            draw(
                shader, deathStar,
                Matrix4f().translation(0.30f, -0.16f, -2.7f).rotateY(-PI.toFloat() / 2f + angle).scale(0.73f),
                view,
                projection,
                0
            )
            frame.eye.set(eye)
            frame.angle = angle
        }

        frame.target.colorTextureId
    }

    private fun draw(
        shader: ShaderInstance,
        model: List<Part>,
        transform: Matrix4f,
        view: Matrix4f,
        projection: Matrix4f,
        kind: Int
    ) {
        shader.getUniform("ModelMat")?.set(transform)
        shader.getUniform("Kind")?.set(kind)

        for (part in model) {
            shader.setSampler("ModelMap", part.texture?.id ?: 0)
            shader.getUniform("HasTexture")?.set(if (part.texture == null) 0 else 1)
            RenderUtils.withBoundBuffer(part.buffer) { part.buffer.drawWithShader(view, projection, shader) }
        }

        shader.clear()
    }

    private fun read(name: String): List<Part> {
        val result = ArrayList<Part>()
        val location = ResourceLocation("gtladditions", "models/effect/lighthunter/$name.bin.gz")
        DataInputStream(GZIPInputStream(resources.getResourceOrThrow(location).open())).use { data ->
            check(data.readInt() == 0x4c485031) { "Invalid Light Hunter model: $location" }
            val count = data.readInt()
            require(count in 1..32)
            repeat(count) {
                val textureName = data.readUTF()
                val color = IntArray(4) { data.readUnsignedByte() }
                val vertices = data.readInt()
                require(vertices in 3..500000 && vertices % 3 == 0)
                val texture = if (textureName.isEmpty()) {
                    null
                } else {
                    resources.getResourceOrThrow(ResourceLocation(textureName)).open().use { stream ->
                        DynamicTexture(NativeImage.read(stream)).also { it.setFilter(true, false) }
                    }
                }
                val builder = BufferBuilder(vertices * 32)
                try {
                    builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
                    repeat(vertices) {
                        val x = data.readFloat()
                        val y = data.readFloat()
                        val z = data.readFloat()
                        val u = data.readFloat()
                        val v = data.readFloat()
                        val nx = data.readFloat()
                        val ny = data.readFloat()
                        val nz = data.readFloat()
                        require(
                            x.isFinite() &&
                                y.isFinite() &&
                                z.isFinite() &&
                                u.isFinite() &&
                                v.isFinite() &&
                                nx.isFinite() &&
                                ny.isFinite() &&
                                nz.isFinite()
                        ) { "Non-finite vertex in $location" }
                        builder.vertex(x.toDouble(), y.toDouble(), z.toDouble()).uv(u, v)
                            .color(color[0], color[1], color[2], color[3]).normal(nx, ny, nz).endVertex()
                    }
                    Part(RenderUtils.uploadStaticBuffer(builder), texture).also {
                        result.add(it)
                        parts.add(it)
                    }
                } catch (error: Throwable) {
                    texture?.close()
                    throw error
                }
            }
        }
        return result
    }

    override fun close() {
        frames.values.forEach { it.target.destroyBuffers() }
        frames.clear()
        parts.forEach {
            it.buffer.close()
            it.texture?.close()
        }
        parts.clear()
    }

    data class WindowView(val eye: Vector3f, val depthBias: Float)

    companion object {
        fun windowView(modelView: Matrix4f): WindowView {
            val eye = Matrix4f(modelView).invert().transformPosition(Vector3f())
            val distance = eye.x - LightHunterParadoxTiming.WINDOW_X
            return WindowView(
                Vector3f(
                    -(eye.z - LightHunterParadoxTiming.WINDOW_Z) / LightHunterParadoxTiming.WINDOW_RADIUS,
                    (eye.y - LightHunterParadoxTiming.WINDOW_Y) / LightHunterParadoxTiming.WINDOW_RADIUS,
                    maxOf(0.05f, abs(distance) / LightHunterParadoxTiming.WINDOW_RADIUS)
                ),
                if (distance >= 0f) LightHunterParadoxTiming.WINDOW_DEPTH_BIAS else -LightHunterParadoxTiming.WINDOW_DEPTH_BIAS
            )
        }
    }
}