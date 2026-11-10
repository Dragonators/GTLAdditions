package com.gtladd.gtladditions.client.render.machine.matrix

import com.google.common.collect.ImmutableMap
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.core.BlockPos
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

@OnlyIn(Dist.CLIENT)
object MatrixIrisStarRenderer {
    // The extra radius contains only the optical halo; the bright body keeps the FOA beam width.
    const val BEAM_HALO_RADIUS_SCALE = 2f
    val CIRCLE_FORMAT = format("iA", "iB", "iC", "iD")
    private val goldSecondary = Vector3f(1f, 0.88f, 0.66f)
    private val goldBeamCore = Vector3f(1f, 0.90f, 0.60f)
    private val goldBeamHalo = Vector3f(1f, 0.52f, 0.12f)
    private val viewport = IntArray(4)
    private val circles = arrayOfNulls<VertexBuffer>(2)

    private class RotationUniforms(val shader: ShaderInstance) {
        val groups = Array(4) { shader.getUniform("uRot$it") }
    }

    private var rotationUniforms: RotationUniforms? = null
    private val widthUniforms = arrayOf("uW0", "uW1", "uW2")

    private fun format(vararg attributes: String): VertexFormat {
        val elements = ImmutableMap.builder<String, VertexFormatElement>().put("Position", DefaultVertexFormat.ELEMENT_POSITION)
        attributes.forEachIndexed { index, name ->
            elements.put(name, VertexFormatElement(index + 1, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 4))
        }

        return VertexFormat(elements.build())
    }

    /**
     * The vertex shader pads each quad by max(W.z, 2.5 px) + W.x of its width class, the pixel taken at the view depth of
     * the primitive's centre, and every centre lies within the group's [MatrixIrisStarGeometry.reach] of the seal's; a corner
     * moves at most sqrt(5) per unit of padding.
     */
    fun bounds(frame: SceneFrame, pos: BlockPos, yaw: Float, flipped: Boolean, inner: Boolean): PartBounds {
        val placement = PoseStack()
        MatrixEyeRenderer.applyTransform(placement, yaw, flipped)
        val center = placement.last().pose().transformPosition(Vector3f(0f, 0f, MatrixIrisStarGeometry.PLANE_Z))
        val x = pos.x + center.x.toDouble()
        val y = pos.y + center.y.toDouble()
        val z = pos.z + center.z.toDouble()
        val reach = MatrixIrisStarGeometry.reach(inner)
        val view = frame.modelView
        val projection = frame.projection
        val depth = if (projection.m33() > 0.5f) {
            1f
        } else {
            val centerDepth = -(
                view.m02() * (x - frame.cameraX) + view.m12() * (y - frame.cameraY) +
                    view.m22() * (z - frame.cameraZ) + view.m32()
                )
            maxOf(centerDepth.toFloat() + reach * Vector3f(view.m02(), view.m12(), view.m22()).length(), 0.1f)
        }

        val pixel = 2f * depth / (maxOf(frame.viewportHeight, 1) * abs(projection.m11()))
        val padding = MatrixShaders.irisStar?.let { shader ->
            widthUniforms.maxOf { name -> shader.getUniform(name)?.floatBuffer?.let { maxOf(it[2], 2.5f * pixel) + it[0] } ?: 0f }
        } ?: 0f

        val radius = reach + sqrt(5f) * padding
        return PartBounds.box(x - radius, y, z - radius, x + radius, y, z + radius)
    }

    /** Posed by the eye's Z-up model transform; the program's blending is ONE / 1-SRC_COLOR. */
    fun render(pose: PoseStack, frame: MatrixEyeAnimation.Frame, inner: Boolean): Boolean {
        val shader = MatrixShaders.irisStar ?: return false
        val rotations = rotationUniforms?.takeIf { it.shader === shader }
            ?: RotationUniforms(shader).also { rotationUniforms = it }
        RenderUtils.withSavedRenderState {
            RenderSystem.disableCull()
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
            draw(circleBuffer(inner), view, shader)
        }

        return true
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

    private fun circleBuffer(inner: Boolean): VertexBuffer {
        val slot = if (inner) 0 else 1
        return circles[slot] ?: MatrixIrisStarGeometry.design.group(inner).let { group ->
            upload(CIRCLE_FORMAT, group.size) { bytes, index ->
                val primitive = group[index]
                for (part in listOf(primitive.a, primitive.b, primitive.c, primitive.d)) part.forEach(bytes::putFloat)
            }
        }.also { circles[slot] = it }
    }

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
        circles.forEach { it?.close() }
        circles.fill(null)
        rotationUniforms = null
    }
}