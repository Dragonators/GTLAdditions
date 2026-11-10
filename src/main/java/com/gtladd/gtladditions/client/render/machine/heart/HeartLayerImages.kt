package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL33C
import org.lwjgl.system.MemoryStack

/** Premultiplied alpha; sorting metadata in view space. */
internal class HeartLayerImages : AutoCloseable {
    var width = 0
        private set
    var height = 0
        private set
    var count = 0
        private set
    var colors = 0
        private set
    var centers = 0
        private set
    var storageWidth = 0
        private set
    var storageHeight = 0
        private set
    private var imageRects = emptyList<IntArray>()
    private var capacityCount = 0
    private var sourceKeys = LongArray(0)
    private var rectTexture = 0
    private var scratch: RenderTarget? = null
    private var layerFramebuffer = 0
    private var centerValues = FloatArray(0)

    val image: RenderTarget
        get() = scratch ?: createTarget(width, height, GL30C.GL_RGBA16F).also { scratch = it }

    fun ensure(width: Int, height: Int, count: Int) =
        ensure(width, height, count, List(count) { intArrayOf(0, 0, width, height) })

    fun ensure(width: Int, height: Int, count: Int, rects: List<IntArray>) {
        require(count > 0 && rects.size == count)
        require(count <= GL11C.glGetInteger(GL30C.GL_MAX_ARRAY_TEXTURE_LAYERS))
        rects.forEach { rect ->
            require(rect.size == 4 && rect[0] >= 0 && rect[1] >= 0 && rect[2] >= 0 && rect[3] >= 0)
            require(rect[0] + rect[2] <= width && rect[1] + rect[3] <= height)
        }

        val requiredWidth = rects.maxOf { it[2] }.coerceAtLeast(1)
        val requiredHeight = rects.maxOf { it[3] }.coerceAtLeast(1)
        val sameFrame = this.width == width && this.height == height
        if (sameFrame && capacityCount >= count && storageWidth >= requiredWidth && storageHeight >= requiredHeight) {
            this.count = count
            imageRects = rects
            return
        }

        val nextWidth = minOf(width, (maxOf(requiredWidth, if (sameFrame) storageWidth else 0) + 63) / 64 * 64)
        val nextHeight = minOf(height, (maxOf(requiredHeight, if (sameFrame) storageHeight else 0) + 63) / 64 * 64)
        val nextCount = maxOf(count, if (sameFrame) capacityCount else 0)
        close()
        try {
            this.width = width
            this.height = height
            this.count = count
            capacityCount = nextCount
            storageWidth = nextWidth
            storageHeight = nextHeight
            imageRects = rects
            centerValues = FloatArray(nextCount * 4)
            sourceKeys = LongArray(nextCount)
            colors = createArray(storageWidth, storageHeight, nextCount, GL30C.GL_RGBA16F, GL11C.GL_RGBA)
            centers = createMetadataTexture(nextCount)
            rectTexture = createMetadataTexture(nextCount)
            layerFramebuffer = GL30C.glGenFramebuffers()
        } catch (exception: Exception) {
            close()
            throw exception
        }
    }

    fun setSource(index: Int, sourceKey: Long) {
        sourceKeys[index] = sourceKey
    }

    fun layerFor(sourceKey: Long): Int = (0 until count).first { sourceKeys[it] == sourceKey }

    private fun createMetadataTexture(count: Int): Int {
        val texture = GlStateManager._genTexture()
        try {
            RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
            GlStateManager._bindTexture(texture)
            RenderUtils.withTightlyPackedPixels {
                GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA32F, count, 1, 0, GL11C.GL_RGBA, GL11C.GL_FLOAT, 0L)
            }

            configureTexture(GL11C.GL_TEXTURE_2D)
            return texture
        } catch (exception: Exception) {
            GlStateManager._deleteTexture(texture)
            throw exception
        }
    }

    fun bindRects(shader: ShaderInstance) {
        RenderSystem.activeTexture(GL13C.GL_TEXTURE7)
        GlStateManager._bindTexture(rectTexture)
        shader.getUniform("ImageRects")?.set(7)
        shader.getUniform("ImageStorageSize")?.set(storageWidth.toFloat(), storageHeight.toFloat())
    }

    fun setCenter(index: Int, x: Float, y: Float, z: Float, radius: Float) {
        val offset = index * 4
        centerValues[offset] = x
        centerValues[offset + 1] = y
        centerValues[offset + 2] = z
        centerValues[offset + 3] = radius
    }

    fun uploadCenters() = MemoryStack.stackPush().use { stack ->
        RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
        GlStateManager._bindTexture(centers)
        val data = stack.mallocFloat(count * 4).put(centerValues, 0, count * 4)
        data.flip()
        RenderUtils.withTightlyPackedPixels {
            GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, count, 1, GL11C.GL_RGBA, GL11C.GL_FLOAT, data)
            GlStateManager._bindTexture(rectTexture)
            data.clear()
            imageRects.forEach { rect -> rect.forEach { data.put(it.toFloat()) } }
            data.flip()
            GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, count, 1, GL11C.GL_RGBA, GL11C.GL_FLOAT, data)
        }
    }

    fun capture(source: RenderTarget, index: Int) {
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, source.frameBufferId)
        bindArray(5, colors)
        val rect = imageRects[index]
        if (rect[2] > 0 && rect[3] > 0) {
            GL12C.glCopyTexSubImage3D(GL30C.GL_TEXTURE_2D_ARRAY, 0, 0, 0, index, rect[0], rect[1], rect[2], rect[3])
        }
    }

    /**
     * [ensure] gave layer [index] the source layer's rect size. The source layer is only attached for reading: [bindLayer]
     * would clear it.
     */
    fun copyLayer(source: HeartLayerImages, sourceIndex: Int, index: Int) {
        val rect = imageRects[index]
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, source.layerFramebuffer)
        GL30C.glFramebufferTextureLayer(GL30C.GL_READ_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, source.colors, 0, sourceIndex)
        bindArray(5, colors)
        if (rect[2] > 0 && rect[3] > 0) GL12C.glCopyTexSubImage3D(GL30C.GL_TEXTURE_2D_ARRAY, 0, 0, 0, index, 0, 0, rect[2], rect[3])
        sourceKeys[index] = source.sourceKeys[sourceIndex]
        System.arraycopy(source.centerValues, sourceIndex * 4, centerValues, index * 4, 4)
    }

    fun bindLayer(index: Int) {
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, layerFramebuffer)
        GL30C.glFramebufferTextureLayer(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, colors, 0, index)
        check(GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) == GL30C.GL_FRAMEBUFFER_COMPLETE)
        RenderSystem.viewport(0, 0, width, height)
        HeartBlackHoleRenderState.disableScissor()
        HeartBlackHoleRenderState.prepareColorWrite()
        GlStateManager._clearColor(0f, 0f, 0f, 0f)
        GlStateManager._clear(GL11C.GL_COLOR_BUFFER_BIT, false)
        val rect = imageRects[index]
        RenderSystem.viewport(-rect[0], -rect[1], width, height)
        RenderSystem.enableScissor(0, 0, rect[2], rect[3])
    }

    fun rect(index: Int): IntArray = imageRects[index]

    fun copyTo(target: RenderTarget, index: Int, alphaOnly: Boolean = false, blend: Boolean = false) {
        if (!blend) check(HeartBlackHoleFramebuffer.clearColor(target))
        target.bindWrite(true)
        copy(index, alphaOnly, blend)
    }

    fun copy(index: Int, alphaOnly: Boolean = false, blend: Boolean = false, skip: IntArray? = null, writeAlpha: Boolean = true) {
        val shader = checkNotNull(HeartBlackHoleShaders.layerCopyShader)
        preparePost(blend)
        HeartBlackHoleRenderState.prepareColorWrite(writeAlpha)
        bindArray(5, colors)
        shader.getUniform("HeartImages")?.set(5)
        shader.getUniform("ImageIndex")?.set(index)
        shader.getUniform("AlphaOnly")?.set(if (alphaOnly) 1 else 0)
        bindRects(shader)
        shader.getUniform("SkipRect")?.set(
            skip?.get(0)?.toFloat() ?: 0f,
            skip?.get(1)?.toFloat() ?: 0f,
            skip?.get(2)?.toFloat() ?: 0f,
            skip?.get(3)?.toFloat() ?: 0f
        )
        val rect = imageRects[index]
        RenderSystem.enableScissor(rect[0], rect[1], rect[2], rect[3])
        try {
            drawQuad(shader)
        } finally {
            RenderSystem.disableScissor()
        }
    }

    override fun close() {
        if (rectTexture != 0) GlStateManager._deleteTexture(rectTexture)
        rectTexture = 0
        storageWidth = 0
        storageHeight = 0
        if (colors != 0) GlStateManager._deleteTexture(colors)
        if (centers != 0) GlStateManager._deleteTexture(centers)
        if (layerFramebuffer != 0) GL30C.glDeleteFramebuffers(layerFramebuffer)
        scratch?.destroyBuffers()
        scratch = null
        colors = 0
        centers = 0
        layerFramebuffer = 0
        width = 0
        height = 0
        count = 0
        centerValues = FloatArray(0)
        sourceKeys = LongArray(0)
        imageRects = emptyList()
        capacityCount = 0
    }

    companion object {
        private val IDENTITY = Matrix4f()

        fun createArray(width: Int, height: Int, count: Int, format: Int, external: Int): Int {
            val texture = GlStateManager._genTexture()
            try {
                bindArray(5, texture)
                RenderUtils.withTightlyPackedPixels {
                    GL12C.glTexImage3D(GL30C.GL_TEXTURE_2D_ARRAY, 0, format, width, height, count, 0, external, GL11C.GL_FLOAT, 0L)
                }

                configureTexture(GL30C.GL_TEXTURE_2D_ARRAY)
                return texture
            } catch (exception: Exception) {
                GlStateManager._deleteTexture(texture)
                throw exception
            }
        }

        fun createTarget(width: Int, height: Int, format: Int, depth: Boolean = false): RenderTarget = RenderUtils.withTightlyPackedPixels {
            val target = object : RenderTarget(depth) {}
            try {
                target.setClearColor(0f, 0f, 0f, 0f)
                target.resize(width, height, false)
                RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
                GlStateManager._bindTexture(target.colorTextureId)
                val external = if (format == GL30C.GL_RG32F) GL30C.GL_RG else GL11C.GL_RGBA
                if (format != GL11C.GL_RGBA8) {
                    GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, format, width, height, 0, external, GL11C.GL_FLOAT, 0L)
                }

                target.setFilterMode(GL11C.GL_NEAREST)
                target
            } catch (exception: Exception) {
                target.destroyBuffers()
                throw exception
            }
        }

        private fun configureTexture(target: Int) {
            GL11C.glTexParameteri(target, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST)
            GL11C.glTexParameteri(target, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST)
            GL11C.glTexParameteri(target, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
            GL11C.glTexParameteri(target, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
        }

        fun bindArray(unit: Int, texture: Int) {
            RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + unit)
            GL11C.glBindTexture(GL30C.GL_TEXTURE_2D_ARRAY, texture)
        }

        fun preparePost(blend: Boolean) {
            HeartBlackHoleRenderState.prepareColorWrite()
            RenderSystem.disableDepthTest()
            RenderSystem.disableCull()
            if (blend) {
                RenderSystem.enableBlend()
                RenderSystem.blendFuncSeparate(GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA)
            } else {
                RenderSystem.disableBlend()
            }
        }

        fun drawQuad(shader: ShaderInstance) {
            try {
                RenderUtils.withBoundBuffer(HeartBlackHoleQuadBuffers.screenQuadBuffer) {
                    HeartBlackHoleQuadBuffers.screenQuadBuffer.drawWithShader(IDENTITY, IDENTITY, shader)
                }
            } finally {
                shader.clear()
            }
        }

        /** ShaderInstance manages 2D samplers only; preserve our array bindings separately. */
        fun <T> withArrayState(draw: () -> T): T {
            val active = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
            val arrays = IntArray(7)
            val samplers = IntArray(10)
            try {
                for (unit in samplers.indices) {
                    RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + unit)
                    if (unit < arrays.size) arrays[unit] = GL11C.glGetInteger(GL30C.GL_TEXTURE_BINDING_2D_ARRAY)
                    samplers[unit] = GL11C.glGetInteger(GL33C.GL_SAMPLER_BINDING)
                    GL33C.glBindSampler(unit, 0)
                }
                return draw()
            } finally {
                for (unit in samplers.indices) {
                    RenderSystem.activeTexture(GL13C.GL_TEXTURE0 + unit)
                    if (unit < arrays.size) GL11C.glBindTexture(GL30C.GL_TEXTURE_2D_ARRAY, arrays[unit])
                    GL33C.glBindSampler(unit, samplers[unit])
                }
                RenderSystem.activeTexture(active)
            }
        }
    }
}