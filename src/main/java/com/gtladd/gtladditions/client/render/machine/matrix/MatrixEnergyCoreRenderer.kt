package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredOculusCompat
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredSceneDepth
import com.gtladd.gtladditions.client.render.machine.heart.HeartLayerImages
import com.gtladd.gtladditions.client.render.machine.heart.HeartSceneCompositor
import com.gtladd.gtladditions.client.render.machine.heart.HeartSceneEffect
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayDeque
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

@OnlyIn(Dist.CLIENT)
object MatrixEnergyCoreRenderer {
    const val SURFACE_COVERAGE = 0.85f
    const val INTERIOR_STRENGTH = 2.0f
    const val PHASE_OFFSET = 3.5f
    private const val CIRCUIT_SIZE = 16
    private val controllerLights = ResourceLocation("ae2", "textures/block/controller_lights.png")
    private var cube: VertexBuffer? = null
    private var circuitTexture = -1
    private val sceneDepth = DeferredSceneDepth()

    internal data class Entry(
        override val blockEntity: BlockEntity,
        val ticks: Double,
        val yaw: Float,
        val flipped: Boolean
    ) : HeartSceneEffect {
        override val bounds: AABB
            get() {
                val angle = Math.toRadians(yaw.toDouble())
                val pos = blockEntity.blockPos
                val center = Vec3(pos.x + 0.5 + MatrixEffectGeometry.CENTER_OFFSET_Z * sin(angle), pos.y + MatrixEffectGeometry.CUBE_CENTER_Y.toDouble(), pos.z + 0.5 + MatrixEffectGeometry.CENTER_OFFSET_Z * cos(angle))
                val diameter = MatrixEffectGeometry.layers[MatrixEffectGeometry.ENERGY_CORE_LAYER_INDEX].side * sqrt(3.0)
                return AABB.ofSize(center, diameter, diameter, diameter)
            }
        override val supportsLayeredRendering: Boolean
            get() = MatrixShaders.energyCoreLayers != null

        override val depthCaptureKey: Any
            get() = MatrixEnergyCoreRenderer

        override val orderedNeedsSceneDepth: Boolean
            get() = false

        override fun renderOrdered(context: DeferredRenderContext, handMask: Int) {
            val shader = MatrixShaders.energyCore ?: return
            RenderUtils.withSavedRenderState {
                HeartSceneEffect.bindHandMask(shader, handMask)
                context.renderRelativeToCamera(blockEntity) {
                    MatrixFrameTransforms.withWinding(flipped) {
                        withCorePose(this, ticks, yaw, flipped) { renderLocal(this, shader, circuitTexture(), ticks) }
                    }
                }
            }
        }

        override fun captureSceneDepth(): Boolean = MatrixShaders.energyCoreLayers?.let { sceneDepth.capture(it) } == true

        override fun renderDirect(context: DeferredRenderContext) {
            RenderUtils.withSavedRenderState {
                context.renderRelativeToCamera(blockEntity) {
                    MatrixFrameTransforms.withWinding(flipped) { render(this, ticks, yaw, flipped) }
                }
            }
        }

        override fun renderLayered(
        context: DeferredRenderContext,
        depth: Int,
        count: Int,
        base: Int,
        rect: IntArray,
        local: Boolean, handMask: Int) {
            val shader = MatrixShaders.energyCoreLayers ?: return
            sceneDepth.bind(shader)
            HeartSceneEffect.bindHandMask(shader, handMask)
            HeartLayerImages.bindArray(3, depth)
            shader.getUniform("LayerDepths")?.set(3)
            shader.getUniform("LayerCount")?.set(count)
            shader.getUniform("LayerBase")?.set(base)
            shader.getUniform("LayerMode")?.set(if (local) 1 else 0)
            shader.getUniform("LayerOrigin")?.set(if (local) rect[0].toFloat() else 0f, if (local) rect[1].toFloat() else 0f)
            shader.getUniform("LayerRect")?.set(rect[0].toFloat(), rect[1].toFloat(), rect[2].toFloat(), rect[3].toFloat())
            RenderSystem.colorMask(true, true, true, local)
            context.renderRelativeToCamera(blockEntity) {
                MatrixFrameTransforms.withWinding(flipped) {
                    withCorePose(this, ticks, yaw, flipped) { renderLocal(this, shader, circuitTexture(), ticks, true) }
                }
            }
        }
    }

    fun releaseBuffers() {
        sceneDepth.close()
        HeartSceneCompositor.close()
        cube?.close()
        cube = null
        if (circuitTexture >= 0) GlStateManager._deleteTexture(circuitTexture)
        circuitTexture = -1
    }

    @JvmOverloads
    fun render(pose: PoseStack, ticks: Double, yaw: Float, flipped: Boolean = false) {
        val shader = MatrixShaders.energyCore ?: return
        HeartSceneEffect.bindHandMask(shader, 0)
        val texture = circuitTexture()
        withCorePose(pose, ticks, yaw, flipped) {
            DeferredOculusCompat.withDeferredShaderPass { renderLocal(this, shader, texture, ticks) }
        }
    }

    private fun withCorePose(pose: PoseStack, ticks: Double, yaw: Float, flipped: Boolean, draw: PoseStack.() -> Unit) {
        pose.withPose {
            MatrixFrameTransforms.apply(this, MatrixEffectGeometry.ENERGY_CORE_LAYER_INDEX, ticks, yaw, flipped)
            val halfSide = MatrixEffectGeometry.layers[MatrixEffectGeometry.ENERGY_CORE_LAYER_INDEX].side * 0.5f
            scale(halfSide, halfSide, halfSide)
            draw()
        }
    }

    private fun renderLocal(pose: PoseStack, shader: ShaderInstance, texture: Int, ticks: Double) =
        renderLocal(pose, shader, texture, ticks, false)

    private fun renderLocal(pose: PoseStack, shader: ShaderInstance, texture: Int, ticks: Double, layered: Boolean) {
        val modelView = pose.last().pose()
        val eye = Matrix4f(modelView).invert().transformPosition(Vector3f())
        shader.getUniform("CameraPosition")?.set(eye.x, eye.y, eye.z)
        shader.getUniform("Phase")?.set((ticks / 20.0 + PHASE_OFFSET).toFloat())
        shader.getUniform("SurfaceCoverage")?.set(SURFACE_COVERAGE)
        shader.getUniform("InteriorStrength")?.set(INTERIOR_STRENGTH)
        RenderSystem.setShaderTexture(0, texture)
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(
            if (layered) GlStateManager.SourceFactor.ONE else GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        )
        if (layered) RenderSystem.disableDepthTest() else RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11C.GL_LEQUAL)
        RenderSystem.depthMask(false)
        RenderSystem.enableCull()
        val buffer = cubeBuffer()
        RenderUtils.withBoundBuffer(buffer) {
            buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), shader)
        }
    }

    private fun cubeBuffer(): VertexBuffer = cube ?: RenderUtils.withStagingBuilder { builder ->
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)

        for (axis in 0..2) {
            for (sign in intArrayOf(-1, 1)) {
                // Inward winding draws each ray once, on the exit face, including from inside the volume.
                val order = if (sign > 0) intArrayOf(0, 3, 2, 1) else intArrayOf(0, 1, 2, 3)

                for (corner in order) {
                    val point = DoubleArray(3)
                    point[axis] = sign.toDouble()
                    point[(axis + 1) % 3] = if (corner == 1 || corner == 2) 1.0 else -1.0
                    point[(axis + 2) % 3] = if (corner >= 2) 1.0 else -1.0
                    builder.vertex(point[0], point[1], point[2]).endVertex()
                }
            }
        }

        RenderUtils.uploadStaticBuffer(builder)
    }.also { cube = it }

    private fun circuitTexture(): Int {
        if (circuitTexture >= 0) return circuitTexture
        val pixels = try {
            readCircuitPixels(Minecraft.getInstance().resourceManager)
        } catch (exception: IOException) {
            // Retain the energy volume if a resource pack removes or corrupts the AE2 texture.
            GTLAdditions.LOGGER.warn("Unable to read matrix energy-core circuit pattern", exception)
            ByteArray(CIRCUIT_SIZE * CIRCUIT_SIZE * 4) { if (it % 4 == 3) 0xff.toByte() else 0 }
        }

        return uploadCircuitTexture(pixels).also { circuitTexture = it }
    }

    /** Read AE2's existing artwork; do not bundle another copy of the controller texture. */
    fun readCircuitPixels(resources: ResourceProvider): ByteArray = resources.getResourceOrThrow(controllerLights).open().use { input ->
        NativeImage.read(NativeImage.Format.RGBA, input).use { image ->
            val occupied = BooleanArray(CIRCUIT_SIZE * CIRCUIT_SIZE) { cell ->
                val x = ((cell % CIRCUIT_SIZE + 0.5) * image.width / CIRCUIT_SIZE).toInt().coerceAtMost(image.width - 1)
                val y = ((cell / CIRCUIT_SIZE + 0.5) * image.width / CIRCUIT_SIZE).toInt().coerceAtMost(image.height - 1)
                image.getPixelRGBA(x, y) ushr 24 > 127
            }

            val distance = IntArray(occupied.size) { -1 }
            val queue = ArrayDeque<Int>()
            var component = 0

            for (seed in occupied.indices) {
                if (!occupied[seed] || distance[seed] >= 0) continue
                distance[seed] = component++ * 4
                queue.add(seed)

                while (queue.isNotEmpty()) {
                    val cell = queue.removeFirst()
                    val x = cell % CIRCUIT_SIZE
                    val y = cell / CIRCUIT_SIZE
                    val adjacent = intArrayOf(
                        if (x > 0) cell - 1 else -1,
                        if (x + 1 < CIRCUIT_SIZE) cell + 1 else -1,
                        if (y > 0) cell - CIRCUIT_SIZE else -1,
                        if (y + 1 < CIRCUIT_SIZE) cell + CIRCUIT_SIZE else -1
                    )

                    for (next in adjacent) {
                        if (next < 0 || !occupied[next] || distance[next] >= 0) continue
                        distance[next] = distance[cell] + 1
                        queue.add(next)
                    }
                }
            }

            ByteArray(occupied.size * 4).also { pixels ->
                for (cell in occupied.indices) {
                    pixels[cell * 4] = if (occupied[cell]) 0xff.toByte() else 0
                    pixels[cell * 4 + 1] = if (occupied[cell]) (round(distance[cell] * 255.0 / 32.0).toInt() and 255).toByte() else 0
                    pixels[cell * 4 + 3] = 0xff.toByte()
                }
            }
        }
    }

    private fun uploadCircuitTexture(pixels: ByteArray): Int {
        val previousActive = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0)
        val previousTexture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        val texture = GL11C.glGenTextures()
        try {
            GlStateManager._bindTexture(texture)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
            val data = ByteBuffer.allocateDirect(pixels.size).put(pixels).flip()
            RenderUtils.withTightlyPackedPixels {
                GL11C.glTexImage2D(
                    GL11C.GL_TEXTURE_2D,
                    0,
                    GL11C.GL_RGBA8,
                    CIRCUIT_SIZE,
                    CIRCUIT_SIZE,
                    0,
                    GL11C.GL_RGBA,
                    GL11C.GL_UNSIGNED_BYTE,
                    data
                )
            }

            return texture
        } catch (exception: Exception) {
            GlStateManager._deleteTexture(texture)
            throw exception
        } finally {
            GlStateManager._bindTexture(previousTexture)
            GlStateManager._activeTexture(previousActive)
        }
    }
}