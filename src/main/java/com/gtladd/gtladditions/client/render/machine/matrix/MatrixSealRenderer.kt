package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL11C
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Cached capsules and tori, widened within the seal plane without increasing vertical thickness. */
@OnlyIn(Dist.CLIENT)
object MatrixSealRenderer {
    private const val STROKE_RADIUS = 0.055f
    private const val RUNE_RADIUS = 0.025f
    private const val TUBE_SEGMENTS = 12
    private const val CAP_SEGMENTS = 4
    private const val RUNE_GLOW_SCALE = 3.6f
    private const val MAIN_GLOW_SCALE = 5f
    private const val MAIN_PLANAR_SCALE = 1.5f
    private const val RUNE_PLANAR_SCALE = 1.2f
    private const val FLOW_DEGREES_PER_SECOND = 12.0

    // Main halos spread sideways; their vertical radius stays below the pedestal clearance.
    private const val CENTER_Y = 0.144f
    private var seal: VertexBuffer? = null
    private val viewport = IntArray(4)

    fun releaseBuffers() {
        seal?.close()
        seal = null
    }

    private fun sealBuffer(): VertexBuffer = seal ?: run {
        val builder = BufferBuilder(1048576)
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
        val mesh = SealMesh(builder)
        for (line in MatrixEffectGeometry.circle()) mesh.capsule(line)
        for (circle in MatrixEffectGeometry.sealRings) mesh.torus(circle)
        mesh.upload()
    }.also { seal = it }

    private fun renderLocal(poseStack: PoseStack, shader: ShaderInstance, ticks: Double) {
        RenderSystem.enableDepthTest()
        RenderSystem.enableCull()
        RenderSystem.enableBlend()
        // Screen blending keeps the background visible and softens overlapping rune joints.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR)
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport)
        shader.getUniform("ViewportHeight")?.set(viewport[3].toFloat())
        // Positive Y rotation decreases the XZ polar angle; compensate it for reverse world-space flow.
        val flowDegrees = MatrixEffectGeometry.angle(ticks, MatrixEffectGeometry.CIRCLE_DEGREES_PER_SECOND + FLOW_DEGREES_PER_SECOND)
        shader.getUniform("FlowPhase")?.set(Math.toRadians(flowDegrees.toDouble()).toFloat())
        val buffer = sealBuffer()
        RenderUtils.withBoundBuffer(buffer) {
            // Every layer is transmitted light; only scene geometry supplies occluding depth.
            RenderSystem.depthMask(false)
            shader.getUniform("RadiusScale")?.set(RUNE_PLANAR_SCALE, 1f, MAIN_PLANAR_SCALE, 1f)
            shader.getUniform("Emission")?.set(0.70f, 0.66f)
            buffer.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shader)

            shader.getUniform("RadiusScale")?.set(RUNE_GLOW_SCALE * RUNE_PLANAR_SCALE, RUNE_GLOW_SCALE, MAIN_GLOW_SCALE * MAIN_PLANAR_SCALE, 3f)
            shader.getUniform("Emission")?.set(0.085f, 0.085f)
            buffer.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shader)
            shader.getUniform("RadiusScale")?.set(2f * RUNE_PLANAR_SCALE, 2f, 2.6f * MAIN_PLANAR_SCALE, 2f)
            shader.getUniform("Emission")?.set(0.30f, 0.30f)
            buffer.drawWithShader(poseStack.last().pose(), RenderSystem.getProjectionMatrix(), shader)
        }
    }

    private class SealMesh(private val builder: BufferBuilder) {
        private data class Vertex(val x: Double, val y: Double, val z: Double, val nx: Double, val ny: Double, val nz: Double, val tangentAngle: Float)
        private data class ShadedVertex(val vertex: Vertex, val radius: Float, val purple: Boolean, val flowing: Boolean)

        // Only identical complete attributes share a node; every triangle and its order remain intact.
        private val vertices = HashMap<ShadedVertex, Int>()
        private val indices = IntArrayList()

        fun upload(): VertexBuffer = MatrixSealVertexBuffer(builder, indices.toIntArray())

        private fun vertex(v: Vertex, radius: Float, purple: Boolean, flowing: Boolean): Int = vertices.getOrPut(ShadedVertex(v, radius, purple, flowing)) {
            val index = vertices.size
            builder.vertex(v.x, v.y + CENTER_Y, v.z)
                .uv(radius, v.tangentAngle)
                .color(
                    if (purple) 0.67f else 0.54f,
                    if (purple) 0.42f else 0.85f,
                    1f,
                    if (radius == RUNE_RADIUS) {
                        0f
                    } else if (flowing) {
                        1f
                    } else {
                        0.5f
                    }
                )
                .normal(v.nx.toFloat(), v.ny.toFloat(), v.nz.toFloat()).endVertex()
            index
        }

        private fun triangle(a: Vertex, b: Vertex, c: Vertex, radius: Float, purple: Boolean, flowing: Boolean = false) {
            indices.add(vertex(a, radius, purple, flowing))
            indices.add(vertex(b, radius, purple, flowing))
            indices.add(vertex(c, radius, purple, flowing))
        }

        fun capsule(line: MatrixEffectGeometry.Line) {
            val x = line.x0 * MatrixEffectGeometry.CIRCLE_RADIUS.toDouble()
            val z = line.z0 * MatrixEffectGeometry.CIRCLE_RADIUS.toDouble()
            val dx = (line.x1 - line.x0) * MatrixEffectGeometry.CIRCLE_RADIUS.toDouble()
            val dz = (line.z1 - line.z0) * MatrixEffectGeometry.CIRCLE_RADIUS.toDouble()
            val length = sqrt(dx * dx + dz * dz)
            if (length <= 0.000001) return
            val tx = dx / length
            val tz = dz / length
            val tangentAngle = atan2(tz, tx).toFloat()
            val radius = if (line.fine) RUNE_RADIUS else STROKE_RADIUS
            val lastSection = CAP_SEGMENTS * 2 + 1

            fun section(index: Int): Array<Vertex> {
                val atEnd = index > CAP_SEGMENTS
                val phi = if (atEnd) {
                    (index - CAP_SEGMENTS - 1) * Math.PI / (2 * CAP_SEGMENTS)
                } else {
                    (index - CAP_SEGMENTS) * Math.PI / (2 * CAP_SEGMENTS)
                }
                val axial = sin(phi)
                val radial = if (index == 0 || index == lastSection) 0.0 else cos(phi)
                return Array(TUBE_SEGMENTS) { side ->
                    val theta = side * Math.PI * 2 / TUBE_SEGMENTS
                    val nx = tx * axial - tz * radial * cos(theta)
                    val ny = radial * sin(theta)
                    val nz = tz * axial + tx * radial * cos(theta)
                    Vertex(x + (if (atEnd) dx else 0.0) + radius * nx, radius * ny, z + (if (atEnd) dz else 0.0) + radius * nz, nx, ny, nz, tangentAngle)
                }
            }

            var previous = section(0)
            for (index in 1..lastSection) {
                val next = section(index)
                for (side in 0 until TUBE_SEGMENTS) {
                    val j = (side + 1) % TUBE_SEGMENTS
                    // Each pole is a triangle fan, with no zero-area triangles at its tip.
                    if (index < lastSection) triangle(previous[side], next[side], next[j], radius, line.purple)
                    if (index > 1) triangle(previous[side], next[j], previous[j], radius, line.purple)
                }
                previous = next
            }
        }

        fun torus(circle: MatrixEffectGeometry.SealRing) {
            val radius = if (circle.fine) RUNE_RADIUS else STROKE_RADIUS
            val ringRadius = circle.radius * MatrixEffectGeometry.CIRCLE_RADIUS.toDouble()
            val cx = circle.x * MatrixEffectGeometry.CIRCLE_RADIUS.toDouble()
            val cz = circle.z * MatrixEffectGeometry.CIRCLE_RADIUS.toDouble()
            // Bound chord error below 1/20 of the tube radius, including the large outer rings.
            val segments = ceil(Math.PI / kotlin.math.acos(1.0 - radius / (20 * ringRadius))).toInt().coerceAtLeast(64)
            fun section(index: Int): Array<Vertex> {
                val angle = (index % segments) * Math.PI * 2 / segments
                val rx = cos(angle)
                val rz = sin(angle)
                return Array(TUBE_SEGMENTS) { side ->
                    val theta = side * Math.PI * 2 / TUBE_SEGMENTS
                    val nx = rx * cos(theta)
                    val ny = sin(theta)
                    val nz = rz * cos(theta)
                    Vertex(cx + ringRadius * rx + radius * nx, radius * ny, cz + ringRadius * rz + radius * nz, nx, ny, nz, (angle + Math.PI / 2).toFloat())
                }
            }

            var previous = section(0)
            for (index in 1..segments) {
                val next = section(index)
                for (side in 0 until TUBE_SEGMENTS) {
                    val j = (side + 1) % TUBE_SEGMENTS
                    triangle(previous[side], previous[j], next[j], radius, circle.purple, circle.flowing)
                    triangle(previous[side], next[j], next[side], radius, circle.purple, circle.flowing)
                }
                previous = next
            }
        }
    }
}