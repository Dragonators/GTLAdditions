package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.client.render.machine.heart.HeartHandDepthMask
import com.gtladd.gtladditions.client.render.machine.heart.HeartLayerImages
import com.gtladd.gtladditions.client.render.machine.heart.HeartSceneCompositor
import com.gtladd.gtladditions.client.render.scene.LayeredPartRenderer
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
import kotlin.math.round
import kotlin.math.sqrt

@OnlyIn(Dist.CLIENT)
internal object MatrixEnergyCoreRenderer : LayeredPartRenderer<MatrixEnergyCore> {
    const val SURFACE_COVERAGE = 0.85f
    const val INTERIOR_STRENGTH = 2.0f
    const val PHASE_OFFSET = 3.5f
    private const val CIRCUIT_SIZE = 16

    // After the six beams.
    private const val PART = 6
    private val controllerLights = ResourceLocation("ae2", "textures/block/controller_lights.png")
    private var cube: VertexBuffer? = null
    private var circuitTexture = -1

    fun submit(frame: SceneFrame, blockEntity: BlockEntity, ticks: Double, yaw: Float, flipped: Boolean) {
        val pos = blockEntity.blockPos
        val center = MatrixFrameTransforms.cubeCenter(pos, yaw)
        // The cube only turns about its centre.
        val radius = MatrixEffectGeometry.layers[MatrixEffectGeometry.ENERGY_CORE_LAYER_INDEX].side * sqrt(3.0) / 2
        frame.submit(
            ScenePart.builder(
                PartKey.block(MatrixEffectRenderer.provider, pos, PART),
                this,
                MatrixEnergyCore(blockEntity, ticks, yaw, flipped)
            )
                .blend(PartBlend.STRAIGHT)
                .bounds(PartBounds.sphere(center.x, center.y, center.z, radius))
                .features(PartFeature.SCENE_DEPTH, PartFeature.OWN_HAND_MASK)
                .build()
        )
    }

    override fun layered(part: ScenePart<MatrixEnergyCore>): Boolean = MatrixShaders.energyCoreLayers != null

    override fun draw(context: PartDrawContext, parts: List<ScenePart<MatrixEnergyCore>>) {
        val shader = MatrixShaders.energyCore ?: return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
        for (part in parts) {
            val core = part.payload
            RenderUtils.withSavedRenderState {
                HeartHandDepthMask.bind(shader, context.handMaskTexture)
                relativeToCamera(context, core) { renderLocal(this, shader, circuitTexture(), core.ticks) }
            }
        }
    }

    override fun drawLayered(
        context: PartDrawContext,
        part: ScenePart<MatrixEnergyCore>,
        depth: Int,
        count: Int,
        base: Int,
        rect: IntArray,
        local: Boolean
    ) {
        val shader = MatrixShaders.energyCoreLayers ?: return
        context.sceneDepth!!.bind(shader)
        HeartHandDepthMask.bind(shader, context.handMaskTexture)
        HeartLayerImages.bindArray(3, depth)
        shader.getUniform("LayerDepths")?.set(3)
        shader.getUniform("LayerCount")?.set(count)
        shader.getUniform("LayerBase")?.set(base)
        shader.getUniform("LayerMode")?.set(if (local) 1 else 0)
        shader.getUniform("LayerOrigin")?.set(if (local) rect[0].toFloat() else 0f, if (local) rect[1].toFloat() else 0f)
        shader.getUniform("LayerRect")?.set(rect[0].toFloat(), rect[1].toFloat(), rect[2].toFloat(), rect[3].toFloat())
        RenderSystem.colorMask(true, true, true, local)
        relativeToCamera(context, part.payload) { renderLocal(this, shader, circuitTexture(), part.payload.ticks, true) }
    }

    private fun relativeToCamera(context: PartDrawContext, core: MatrixEnergyCore, draw: PoseStack.() -> Unit) {
        val pos = core.blockEntity.blockPos
        val frame = context.frame
        context.poseStack.withPose {
            translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
            MatrixFrameTransforms.withWinding(core.flipped) { withCorePose(this, core.ticks, core.yaw, core.flipped, draw) }
        }
    }

    fun releaseBuffers() {
        HeartSceneCompositor.close()
        cube?.close()
        cube = null
        if (circuitTexture >= 0) GlStateManager._deleteTexture(circuitTexture)
        circuitTexture = -1
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

internal class MatrixEnergyCore(val blockEntity: BlockEntity, val ticks: Double, val yaw: Float, val flipped: Boolean)