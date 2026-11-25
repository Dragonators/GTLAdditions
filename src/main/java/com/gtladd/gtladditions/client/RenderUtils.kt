package com.gtladd.gtladditions.client

import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.mixin.gtlcore.client.GlobalRenderClockAccessor
import com.gtladd.gtladditions.mixin.minecraft.client.BlendModeAccessor
import com.mojang.blaze3d.platform.GlStateManager
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
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.client.model.data.ModelData
import org.gtlcore.gtlcore.client.ClientUtil
import org.gtlcore.gtlcore.config.ConfigHolder
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL14C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL21C
import org.lwjgl.opengl.GL30C
import kotlin.math.*

@Suppress("DuplicatedCode", "SameParameterValue")
@OnlyIn(Dist.CLIENT)
object RenderUtils {

    /** Uses Core's pause-aware nanosecond clock. */
    fun getSmoothTickDouble(): Double = GlobalRenderClockAccessor.invokeGetElapsedNanos() / 50_000_000.0

    fun getSmoothTickDouble(machine: MetaMachine, partialTick: Float): Double =
        if (ConfigHolder.INSTANCE.graphics.enableSmoothAnimations) getSmoothTickDouble() else machine.offsetTimer.toDouble() + partialTick

    // Narrow only a bounded final angle, so float matrices retain sub-frame motion in old worlds.
    fun rotationRadians(angle: Double): Float = angle.IEEErem(2 * PI).toFloat()

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

    /** `RenderTarget.destroyBuffers` binds framebuffer 0 and the active unit's texture 0; this keeps the caller's three bindings. */
    inline fun withTargetBindings(release: () -> Unit) {
        val read = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
        val draw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
        val texture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        try {
            release()
        } finally {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, read)
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, draw)
            GlStateManager._bindTexture(texture)
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

    class SmoothAnimationTimer {
        private var startTick: Double = getSmoothTickDouble()

        fun reset() {
            startTick = getSmoothTickDouble()
        }

        /**
         * @param durationMillis Animation duration (milliseconds)
         * @return Current progress, range 0.0 to 1.0
         */
        fun getProgress(durationMillis: Long): Float {
            val durationTicks = durationMillis / 50.0 // 1 tick = 50ms
            val elapsedTicks = getSmoothTickDouble() - startTick
            return (elapsedTicks / durationTicks).coerceIn(0.0, 1.0).toFloat()
        }
    }
}
