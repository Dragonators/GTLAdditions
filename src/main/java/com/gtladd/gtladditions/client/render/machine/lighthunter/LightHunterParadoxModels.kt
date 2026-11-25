package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderState
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import java.util.IdentityHashMap
import kotlin.math.PI
import kotlin.math.abs

class LightHunterParadoxModels : AutoCloseable {
    private class Frame {
        val target = TextureTarget(512, 512, true, false)
        var angle = Float.NaN
        val eye = Vector3f(Float.NaN)
    }

    private val frames = IdentityHashMap<Any, Frame>()

    fun remove(key: Any) {
        val frame = frames.remove(key) ?: return
        RenderUtils.withTargetBindings { frame.target.destroyBuffers() }
    }

    internal fun prepare(
        key: Any,
        shader: ShaderInstance,
        assets: LightHunterParadoxAssets.Loaded,
        eye: Vector3f,
        angle: Float
    ): Int = HeartBlackHoleRenderState.withSavedState {
        val frame = frames.getOrPut(key) { Frame() }
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
            draw(shader, assets.kamino, Matrix4f().translation(-1.8f, 1f, -7.5f).scale(2.5f), view, projection, 1)
            draw(
                shader,
                assets.deathStar,
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
        model: List<LightHunterParadoxAssets.Part>,
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

    override fun close() {
        if (frames.isEmpty()) return

        RenderUtils.withTargetBindings {
            frames.values.forEach { it.target.destroyBuffers() }
            frames.clear()
        }
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
