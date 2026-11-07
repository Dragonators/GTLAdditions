package com.gtladd.gtladditions.client.render.machine.matrix

import com.google.common.collect.ImmutableMap
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.hypot

/** Analytic seal strokes with FOA volume beams anchored to the six moving seal eyes. */
@OnlyIn(Dist.CLIENT)
object MatrixIrisStarRenderer {
    // The extra radius contains only the optical halo; the bright body keeps the FOA beam width.
    const val BEAM_HALO_RADIUS_SCALE = 2f
    val CIRCLE_FORMAT = format("iA", "iB", "iC", "iD")
    private val goldSecondary = Vector3f(1f, 0.88f, 0.66f)
    private val goldBeamCore = Vector3f(1f, 0.90f, 0.60f)
    private val goldBeamHalo = Vector3f(1f, 0.52f, 0.12f)
    private val viewport = IntArray(4)
    private var circle: VertexBuffer? = null
    private class RotationUniforms(val shader: ShaderInstance) {
        val groups = Array(4) { shader.getUniform("uRot$it") }
    }

    private var rotationUniforms: RotationUniforms? = null

    private fun format(vararg attributes: String): VertexFormat {
        val elements = ImmutableMap.builder<String, VertexFormatElement>().put("Position", DefaultVertexFormat.ELEMENT_POSITION)
        attributes.forEachIndexed { index, name ->
            elements.put(name, VertexFormatElement(index + 1, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 4))
        }

        return VertexFormat(elements.build())
    }

    /** Pose is the same Z-up local model transform as the eye; caller selects the deferred target. */
    fun render(pose: PoseStack, frame: MatrixEyeAnimation.Frame) {
        val shader = MatrixShaders.irisStar ?: return
        val rotations = rotationUniforms?.takeIf { it.shader === shader }
            ?: RotationUniforms(shader).also { rotationUniforms = it }
        RenderUtils.withSavedRenderState {
            RenderSystem.enableDepthTest()
            RenderSystem.depthFunc(GL11C.GL_LEQUAL)
            RenderSystem.depthMask(false)
            RenderSystem.disableCull()
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE
            )
            GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport)
            val view = pose.last().pose()
            shader.getUniform("uZ")?.set(MatrixIrisStarGeometry.PLANE_Z)
            for (group in 0..3) rotations.groups[group]?.set(MatrixIrisStarGeometry.rotation(group, frame.time))
            shader.getUniform("uPrimary")?.set(MatrixEyeRenderer.tint)
            shader.getUniform("uReveal")?.set(frame.sealReveal)
            shader.getUniform("ViewportHeight")?.set(viewport[3].toFloat())
            shader.getUniform("uSecondary")?.set(goldSecondary)
            shader.getUniform("uTime")?.set(frame.time.toFloat())
            shader.getUniform("uLight")?.set(frame.light)
            shader.getUniform("uOpen")?.set(frame.open)
            shader.getUniform("uFront")?.set(frame.sealFront)
            shader.getUniform("uPhase")?.set(Math.toRadians((frame.time * 15.0) % 360.0).toFloat())
            val gazeLength = hypot(frame.gazeX, frame.gazeY)
            shader.getUniform("uGazeAmt")?.set((gazeLength / 0.05f).coerceIn(0f, 1f))
            shader.getUniform("uGazeDir")?.set(
                if (gazeLength > 0.00001f) frame.gazeX / gazeLength else 1f,
                if (gazeLength > 0.00001f) frame.gazeY / gazeLength else 0f
            )
            draw(circleBuffer(), view, shader)
        }
    }

    fun beamOrigins(seconds: Double, yaw: Float, flipped: Boolean): List<Vector3f> {
        val placement = PoseStack()
        MatrixEyeRenderer.applyTransform(placement, yaw, flipped)
        return MatrixIrisStarGeometry.design.beams.map { beam ->
            val xy = MatrixIrisStarGeometry.beamPosition(beam, seconds)
            placement.last().pose().transformPosition(Vector3f(xy.x, xy.y, MatrixIrisStarGeometry.PLANE_Z))
        }
    }

    fun beamAlpha(frame: MatrixEyeAnimation.Frame): Float {
        val edge = ((MatrixIrisStarGeometry.BEAM_RING_RADIUS - frame.sealReveal + 1.5f) / 3f).coerceIn(0f, 1f)
        val lit = 1f - edge * edge * (3f - 2f * edge)
        return (0.18f + 0.62f * frame.light) * (0.25f + 0.75f * lit)
    }

    fun configureBeamShader(shader: ShaderInstance) {
        shader.getUniform("BeamColor")?.set(MatrixEyeRenderer.tint)
        shader.getUniform("CoreColor")?.set(goldBeamCore)
        shader.getUniform("HaloColor")?.set(goldBeamHalo)
        shader.getUniform("BeamRadius")?.set(MatrixEffectGeometry.BEAM_RADIUS * BEAM_HALO_RADIUS_SCALE)
    }

    private fun draw(buffer: VertexBuffer, view: Matrix4f, shader: ShaderInstance) = RenderUtils.withBoundBuffer(buffer) {
        buffer.drawWithShader(view, RenderSystem.getProjectionMatrix(), shader)
    }

    private fun circleBuffer(): VertexBuffer = circle ?: upload(CIRCLE_FORMAT, MatrixIrisStarGeometry.design.primitives.size) { bytes, index ->
        val primitive = MatrixIrisStarGeometry.design.primitives[index]
        for (part in listOf(primitive.a, primitive.b, primitive.c, primitive.d)) part.forEach(bytes::putFloat)
    }.also { circle = it }

    private inline fun upload(format: VertexFormat, count: Int, attributes: (ByteBuffer, Int) -> Unit): VertexBuffer {
        val bytes = MemoryUtil.memAlloc(count * 4 * format.vertexSize).order(ByteOrder.nativeOrder())
        try {
            for (i in 0 until count) {
                for (corner in 0..3) {
                    val x = if (corner == 1 || corner == 2) 1f else -1f
                    val y = if (corner >= 2) 1f else -1f
                    bytes.putFloat(x).putFloat(y).putFloat(0f)
                    attributes(bytes, i)
                }
            }

            bytes.flip()
            return RenderUtils.withStagingBuilder { builder ->
                builder.begin(VertexFormat.Mode.QUADS, format)
                builder.putBulkData(bytes)
                RenderUtils.uploadStaticBuffer(builder)
            }
        } finally {
            MemoryUtil.memFree(bytes)
        }
    }

    fun releaseBuffers() {
        circle?.close()
        circle = null
        rotationUniforms = null
    }
}