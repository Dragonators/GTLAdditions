package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.data.CircularMotionParams
import com.gtladd.gtladditions.common.data.RotationParams
import com.gtladd.gtladditions.mixin.gtlcore.client.GlobalRenderClockAccessor
import com.gtladd.gtladditions.mixin.minecraft.client.BlendModeAccessor
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.util.RandomSource
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.client.model.data.ModelData
import org.gtlcore.gtlcore.client.ClientUtil
import org.gtlcore.gtlcore.config.ConfigHolder
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL14C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL21C
import kotlin.math.*

@Suppress("DuplicatedCode", "unused", "SameParameterValue")
@OnlyIn(Dist.CLIENT)
object RenderUtils {

    /** Uses Core's pause-aware nanosecond clock. */
    fun getSmoothTickDouble(): Double = GlobalRenderClockAccessor.getElapsedNanos() / 50_000_000.0

    fun getSmoothTickDouble(machine: MetaMachine, partialTick: Float): Double =
        if (ConfigHolder.INSTANCE.graphics.enableSmoothAnimations) getSmoothTickDouble() else machine.offsetTimer.toDouble() + partialTick

    // Narrow only a bounded final angle, so float matrices retain sub-frame motion in old worlds.
    fun rotationRadians(angle: Double): Float = Math.IEEEremainder(angle, 2 * PI).toFloat()

    /** Only the RGB tint is applied. */
    fun renderFullBrightModel(
        pose: PoseStack.Pose,
        consumer: VertexConsumer,
        model: BakedModel,
        type: RenderType,
        argb32: Int = -1
    ) {
        ClientUtil.modelRenderer().renderModel(
            pose,
            consumer,
            null,
            model,
            FastColor.ARGB32.red(argb32) / 255f,
            FastColor.ARGB32.green(argb32) / 255f,
            FastColor.ARGB32.blue(argb32) / 255f,
            LightTexture.FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            ModelData.EMPTY,
            type
        )
    }

    fun renderFullBrightModel(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        modelLocation: ResourceLocation?,
        type: RenderType,
        argb32: Int = -1
    ) = renderFullBrightModel(poseStack.last(), buffer.getBuffer(type), ClientUtil.getBakedModel(modelLocation), type, argb32)

    /** Framebuffer and shader bindings are not preserved. */
    inline fun <T> withSavedRenderState(draw: () -> T): T {
        val shaderBlendMode = BlendModeAccessor.getLastApplied()
        val blend = GL11C.glIsEnabled(GL11C.GL_BLEND)
        val cull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE)
        val depth = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST)
        val depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK)
        val depthFunc = GL11C.glGetInteger(GL11C.GL_DEPTH_FUNC)
        val srcRgb = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB)
        val dstRgb = GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB)
        val srcAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA)
        val dstAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA)
        val blendEquationRgb = GL11C.glGetInteger(GL20C.GL_BLEND_EQUATION_RGB)
        val blendEquationAlpha = GL11C.glGetInteger(GL20C.GL_BLEND_EQUATION_ALPHA)
        val texture = RenderSystem.getShaderTexture(0)

        return try {
            draw()
        } finally {
            RenderSystem.setShaderTexture(0, texture)
            RenderSystem.depthMask(depthMask)
            RenderSystem.depthFunc(depthFunc)
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
            GL20C.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha)
            // A stale shader cache can overwrite the next draw's explicit blend factors (e.g. the HUD vignette).
            BlendModeAccessor.setLastApplied(shaderBlendMode)
            if (blend) RenderSystem.enableBlend() else RenderSystem.disableBlend()
            if (cull) RenderSystem.enableCull() else RenderSystem.disableCull()
            if (depth) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
        }
    }

    /** Also keeps null allocations independent of a bound PBO. */
    inline fun <T> withTightlyPackedPixels(upload: () -> T): T {
        val alignment = GL11C.glGetInteger(GL11C.GL_UNPACK_ALIGNMENT)
        val rowLength = GL11C.glGetInteger(GL11C.GL_UNPACK_ROW_LENGTH)
        val skipPixels = GL11C.glGetInteger(GL11C.GL_UNPACK_SKIP_PIXELS)
        val skipRows = GL11C.glGetInteger(GL11C.GL_UNPACK_SKIP_ROWS)
        val imageHeight = GL11C.glGetInteger(GL12C.GL_UNPACK_IMAGE_HEIGHT)
        val skipImages = GL11C.glGetInteger(GL12C.GL_UNPACK_SKIP_IMAGES)
        val swapBytes = GL11C.glGetInteger(GL11C.GL_UNPACK_SWAP_BYTES)
        val unpackBuffer = GL11C.glGetInteger(GL21C.GL_PIXEL_UNPACK_BUFFER_BINDING)
        try {
            GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, 0)
            GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, 1)
            GL11C.glPixelStorei(GL11C.GL_UNPACK_ROW_LENGTH, 0)
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_PIXELS, 0)
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_ROWS, 0)
            GL11C.glPixelStorei(GL12C.GL_UNPACK_IMAGE_HEIGHT, 0)
            GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_IMAGES, 0)
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SWAP_BYTES, GL11C.GL_FALSE)
            return upload()
        } finally {
            GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, alignment)
            GL11C.glPixelStorei(GL11C.GL_UNPACK_ROW_LENGTH, rowLength)
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_PIXELS, skipPixels)
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_ROWS, skipRows)
            GL11C.glPixelStorei(GL12C.GL_UNPACK_IMAGE_HEIGHT, imageHeight)
            GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_IMAGES, skipImages)
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SWAP_BYTES, swapBytes)
            GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, unpackBuffer)
        }
    }

    fun uploadStaticBuffer(builder: BufferBuilder): VertexBuffer {
        val buffer = VertexBuffer(VertexBuffer.Usage.STATIC)
        try {
            withBoundBuffer(buffer) {
                // upload releases the RenderedBuffer before the builder is reused.
                buffer.upload(builder.end())
            }
            return buffer
        } catch (error: Throwable) {
            buffer.close()
            throw error
        }
    }

    /** GameRenderer.reloadShaders only catches IOException; a failing program reaches [onLoad] as null instead of aborting the reload. */
    fun registerShader(event: RegisterShadersEvent, id: ResourceLocation, format: VertexFormat, onLoad: (ShaderInstance?) -> Unit) =
        registerShader(event, id, { ShaderInstance(event.resourceProvider, id, format) }, onLoad)

    fun registerShader(event: RegisterShadersEvent, id: ResourceLocation, create: () -> ShaderInstance, onLoad: (ShaderInstance?) -> Unit) {
        val shader = try {
            create()
        } catch (error: Exception) {
            GTLAdditions.LOGGER.error("Unable to load shader {}", id, error)
            onLoad(null)
            return
        }

        event.registerShader(shader) { loaded ->
            try {
                onLoad(loaded)
            } catch (error: Exception) {
                GTLAdditions.LOGGER.error("Unable to apply shader {}", id, error)
                onLoad(null)
            }
        }
    }

    private val stagingBuilders = ArrayDeque<BufferBuilder>()

    /** 1.20.1 never frees a BufferBuilder's native memory; one-off meshes borrow builders that live for the process. */
    fun <T> withStagingBuilder(build: (BufferBuilder) -> T): T {
        val builder = stagingBuilders.removeLastOrNull() ?: BufferBuilder(256)
        try {
            return build(builder).also { stagingBuilders.addLast(builder) }
        } catch (error: Throwable) {
            if (!builder.building() || runCatching { builder.end().release() }.isSuccess) stagingBuilders.addLast(builder)
            throw error
        }
    }

    fun releaseBuffers(buffers: Iterable<VertexBuffer>?) {
        buffers?.forEach(VertexBuffer::close)
    }

    /** Unbinds rather than restoring an earlier VBO. */
    inline fun <T> withBoundBuffer(buffer: VertexBuffer, draw: () -> T): T {
        buffer.bind()
        return try {
            draw()
        } finally {
            VertexBuffer.unbind()
        }
    }

    private var batchedShader: ShaderInstance? = null

    internal inline fun <T> withShaderBatch(shader: ShaderInstance, enabled: Boolean, draw: () -> T): T {
        if (!enabled || batchedShader === shader) return draw()
        RenderSystem.assertOnRenderThread()
        val previous = batchedShader
        batchedShader = shader
        return try {
            draw()
        } finally {
            batchedShader = previous
            shader.clear()
        }
    }

    @JvmStatic
    fun shouldDeferShaderClear(shader: ShaderInstance): Boolean = batchedShader === shader

    inline fun <T> withRenderType(type: RenderType, draw: () -> T): T {
        type.setupRenderState()
        return try {
            draw()
        } finally {
            type.clearRenderState()
        }
    }

    /**
     * Generates random rotation parameters with uniformly distributed axis and speed
     *
     * @param random   Random source for generating rotation parameters
     * @param minSpeed Minimum rotation speed in degrees per tick
     * @param maxSpeed Maximum rotation speed in degrees per tick
     * @return Rotation parameters containing axis, speed, and offset
     */
    fun createRandomRotation(random: RandomSource, minSpeed: Float, maxSpeed: Float): RotationParams {
        val theta = random.nextFloat() * 2.0f * Math.PI.toFloat()
        val phi = acos((2.0f * random.nextFloat() - 1.0f).toDouble()).toFloat()

        val rotationAxis = Vector3f(
            (sin(phi.toDouble()) * cos(theta.toDouble())).toFloat(),
            (sin(phi.toDouble()) * sin(theta.toDouble())).toFloat(),
            cos(phi.toDouble()).toFloat()
        )

        // Use square root distribution to concentrate speeds in the middle range, avoiding extreme fast rotation
        val speedFactor = sqrt(random.nextFloat().toDouble()).toFloat()
        val rotationSpeed = minSpeed + speedFactor * (maxSpeed - minSpeed)
        val rotationOffset = random.nextFloat() * 360f
        return RotationParams(rotationAxis, rotationSpeed, rotationOffset)
    }

    /**
     * Renders a star layer with color tinting and rotation
     *
     * @param poseStack     Pose stack for transformations
     * @param buffer        Multi-buffer source for rendering
     * @param modelLocation Resource location of the model to render
     * @param size          Scale size of the star layer
     * @param rotationAxis  Rotation axis vector
     * @param angle         Rotation angle in degrees
     * @param argb32        ARGB32 color value for tinting
     * @param type          Render type to use
     */
    fun renderStarLayer(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        modelLocation: ResourceLocation?,
        size: Float,
        rotationAxis: Vector3f,
        angle: Float,
        argb32: Int,
        type: RenderType
    ) {
        poseStack.withPose {
            scale(size, size, size)
            mulPose(
                Quaternionf().fromAxisAngleDeg(
                    rotationAxis.x,
                    rotationAxis.y,
                    rotationAxis.z,
                    angle
                )
            )

            renderFullBrightModel(this, buffer, modelLocation, type, argb32)
        }
    }

    /**
     * Renders a halo layer with glow effect using eyes render type
     *
     * @param poseStack     Pose stack for transformations
     * @param buffer        Multi-buffer source for rendering
     * @param size          Scale size of the halo layer
     * @param rotationAxis  Rotation axis vector
     * @param angle         Rotation angle in degrees
     * @param haloTexture   Texture resource location for the halo glow effect
     * @param modelLocation Resource location of the model to render
     */
    fun renderHaloLayer(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        size: Float,
        rotationAxis: Vector3f,
        angle: Float,
        haloTexture: ResourceLocation,
        modelLocation: ResourceLocation
    ) {
        poseStack.withPose {
            scale(size, size, size)
            mulPose(
                Quaternionf().fromAxisAngleDeg(
                    rotationAxis.x,
                    rotationAxis.y,
                    rotationAxis.z,
                    angle
                )
            )

            val renderType = RenderType.eyes(haloTexture)

            renderFullBrightModel(this, buffer, modelLocation, renderType)
        }
    }

    /**
     * Renders a model performing circular motion using a pre-obtained VertexConsumer and BakedModel
     * Use this for batch rendering multiple models with the same RenderType
     *
     * @param poseStack     Pose stack for transformations
     * @param vertexConsumer Pre-obtained vertex consumer (for batch rendering)
     * @param bakedModel    Pre-obtained baked model (for batch rendering)
     * @param motionParams  Parameters defining the circular motion
     * @param tick          Current tick with partial ticks for animation
     * @param size          Scale size of the model
     * @param argb32        ARGB32 color value for tinting
     * @param type          Render type to use
     * @param faceMotion    If true, rotate the model to face the direction of motion
     * @param additionalRotation Optional additional rotation to apply after positioning
     * @param centered      If true, center the model at the orbit position (offsets by -0.5 in all axes after scaling)
     */
    fun renderCircularMotionModelDirect(
        poseStack: PoseStack,
        vertexConsumer: VertexConsumer,
        bakedModel: BakedModel,
        motionParams: CircularMotionParams,
        tick: Double,
        size: Float = 1.0f,
        argb32: Int = 0xFFFFFFFF.toInt(),
        type: RenderType,
        faceMotion: Boolean = true,
        additionalRotation: Quaternionf? = null,
        centered: Boolean = false
    ) {
        val position = motionParams.getPosition(tick)

        poseStack.withPose {
            translate(position.x, position.y, position.z)

            if (faceMotion) {
                val facingAngle = motionParams.getFacingAngle(tick)
                mulPose(Quaternionf().fromAxisAngleDeg(0f, 1f, 0f, facingAngle + 90f))
            }

            if (additionalRotation != null) {
                mulPose(additionalRotation)
            }

            scale(size, size, size)

            if (centered) {
                translate(-0.5, -0.5, -0.5)
            }

            renderFullBrightModel(last(), vertexConsumer, bakedModel, type, argb32)
        }
    }

    class SmoothAnimationTimer {
        private var startTick: Double = RenderUtils.getSmoothTickDouble()

        fun reset() {
            startTick = RenderUtils.getSmoothTickDouble()
        }

        /**
         * @param durationMillis Animation duration (milliseconds)
         * @return Current progress, range 0.0 to 1.0
         */
        fun getProgress(durationMillis: Long): Float {
            val durationTicks = durationMillis / 50.0 // 1 tick = 50ms
            val elapsedTicks = RenderUtils.getSmoothTickDouble() - startTick
            return (elapsedTicks / durationTicks).coerceIn(0.0, 1.0).toFloat()
        }
    }
}