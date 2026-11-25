package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.api.client.scene.SceneDepth
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.scene.SceneOrder
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C

internal object HeartSceneCompositor : AutoCloseable {
    private val images = HeartLayerImages()
    private val subset = HeartLayerImages()
    private var targets: Targets? = null

    fun prepareLayers(
        entries: List<HeartBlackHoleRenderEntry>,
        poseStack: PoseStack,
        cameraPosition: Vec3,
        handMask: Int,
        depth: SceneDepth
    ): Boolean = HeartBlackHoleRenderer.renderLayers(entries, poseStack, cameraPosition, images, handMask, depth)

    fun copyLayer(sourceKey: Long) = images.copy(images.layerFor(sourceKey), blend = true, writeAlpha = false)

    fun layers(sourceKeys: List<Long>): HeartLayerImages {
        if (sourceKeys.size == images.count) return images
        val sources = sourceKeys.map(images::layerFor).sorted()
        subset.ensure(images.width, images.height, sources.size, sources.map(images::rect))
        sources.forEachIndexed { index, source -> subset.copyLayer(images, source, index) }
        subset.uploadCenters()

        return subset
    }

    internal fun region(
        shipBounds: List<AABB>,
        heartBounds: List<AABB>,
        pose: Matrix4f,
        projection: Matrix4f,
        camera: Vec3,
        width: Int,
        height: Int
    ): IntArray? {
        val vp = Matrix4f(projection).mul(pose)
        val hearts = heartBounds.map { SceneOrder.screenBounds(vp, it.move(-camera.x, -camera.y, -camera.z), width, height, 12) }
        val ships = shipBounds.map { SceneOrder.screenBounds(vp, it.move(-camera.x, -camera.y, -camera.z), width, height, 2) }
        var x0 = width
        var y0 = height
        var x1 = 0
        var y1 = 0
        fun meet(rect: IntArray, other: IntArray) {
            val left = maxOf(rect[0], other[0])
            val bottom = maxOf(rect[1], other[1])
            val right = minOf(rect[0] + rect[2], other[0] + other[2])
            val top = minOf(rect[1] + rect[3], other[1] + other[3])
            if (right > left && top > bottom) {
                x0 = minOf(x0, left)
                y0 = minOf(y0, bottom)
                x1 = maxOf(x1, right)
                y1 = maxOf(y1, top)
            }
        }

        for ((index, heart) in hearts.withIndex()) {
            ships.forEach { meet(heart, it) }
            for (other in index + 1 until hearts.size) meet(heart, hearts[other])
        }

        if (x1 <= x0 || y1 <= y0) return null
        // Preserve the derivative-quad alignment used by shield and chart AA.
        x0 = x0 / 16 * 16
        y0 = y0 / 16 * 16
        x1 = minOf(width, (x1 + 15) / 16 * 16)
        y1 = minOf(height, (y1 + 15) / 16 * 16)

        return intArrayOf(x0, y0, x1 - x0, y1 - y0)
    }

    internal fun compose(
        output: RenderTarget,
        images: HeartLayerImages,
        local: Targets,
        projection: Matrix4f,
        rect: IntArray,
        draw: (Int, Boolean) -> Unit
    ) {
        local.sortDepths(images, projection, rect)
        output.bindWrite(true)
        draw(0, false)

        // Entries arrive far to near.
        for (index in 0 until images.count) {
            images.copy(index, blend = true, skip = rect, writeAlpha = false)
        }

        // Each group has four outputs.
        var base = images.count / 4 * 4
        while (base >= 0) {
            local.beginLayers(output, rect)
            draw(base, true)
            local.resolve(output, images, rect, base)
            base -= 4
        }
    }

    /** [width] x [height] is the rect; [images] spans the whole frame. */
    fun ensureTargets(width: Int, height: Int, images: HeartLayerImages): Targets {
        val old = targets
        if (old != null &&
            old.width >= width &&
            old.height >= height &&
            old.width <= images.width &&
            old.height <= images.height
        ) {
            old.ensureCount(images.count)
            return old
        }

        val capacityWidth = minOf(images.width, (maxOf(width, old?.width ?: 0) + 255) / 256 * 256)
        val capacityHeight = minOf(images.height, (maxOf(height, old?.height ?: 0) + 255) / 256 * 256)
        old?.close()
        targets = null
        return Targets(capacityWidth, capacityHeight, images.count).also { targets = it }
    }

    override fun close() {
        targets?.close()
        targets = null
        images.close()
        subset.close()
    }

    internal class Targets(val width: Int, val height: Int, count: Int) : AutoCloseable {
        private val background = createColorTexture()
        private val framebuffer = GL30C.glGenFramebuffers()
        private val bins = IntArray(4)
        private val sorting = ArrayList<RenderTarget>(2)

        // Up to three distances share one texel. Larger groups store a distance/index pair per rank.
        private var packedDepths = 0
        private var packedCapacity = 0
        private var rankedDepths = 0
        private var rankedLayers = 0
        private var activeCount = 0
        private val binCount get() = minOf(activeCount + 1, 4)

        val depths: Int get() = if (activeCount <= 3) packedDepths else rankedDepths

        init {
            try {
                ensureCount(count)
            } catch (error: Exception) {
                close()
                throw error
            }
        }

        fun ensureCount(count: Int) {
            if (count == activeCount) return

            if (count <= 3 && count > packedCapacity) {
                val format = when (count) {
                    1 -> GL30C.GL_R32F
                    2 -> GL30C.GL_RG32F
                    else -> GL30C.GL_RGBA32F
                }
                val external = when (count) {
                    1 -> GL11C.GL_RED
                    2 -> GL30C.GL_RG
                    else -> GL11C.GL_RGBA
                }

                val next = HeartLayerImages.createArray(width, height, 1, format, external)
                if (packedDepths != 0) GlStateManager._deleteTexture(packedDepths)
                packedDepths = next
                packedCapacity = count
            } else if (count >= 4 && count > rankedLayers) {
                while (sorting.size < 2) {
                    sorting.add(HeartLayerImages.createTarget(width, height, GL30C.GL_RG32F))
                }

                val next = HeartLayerImages.createArray(width, height, count, GL30C.GL_RG32F, GL30C.GL_RG)
                if (rankedDepths != 0) GlStateManager._deleteTexture(rankedDepths)
                rankedDepths = next
                rankedLayers = count
            }

            val nextBins = minOf(count + 1, 4)
            if (nextBins != binCount) {
                GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)
                for (index in 0 until nextBins) {
                    if (bins[index] != 0) continue

                    bins[index] = createColorTexture()
                    GL30C.glFramebufferTexture2D(
                        GL30C.GL_FRAMEBUFFER,
                        GL30C.GL_COLOR_ATTACHMENT0 + index,
                        GL11C.GL_TEXTURE_2D,
                        bins[index],
                        0
                    )
                }

                GL20C.glDrawBuffers(DRAW_BUFFERS[nextBins - 1])
                check(GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) == GL30C.GL_FRAMEBUFFER_COMPLETE)
            }

            activeCount = count
        }

        /** Background and bins are sampled or attached to the shared MRT. Only the sorting targets need their own FBOs. */
        private fun createColorTexture(): Int {
            val bound = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
            val texture = GlStateManager._genTexture()

            try {
                GlStateManager._bindTexture(texture)
                RenderUtils.withTightlyPackedPixels {
                    GL11C.glTexImage2D(
                        GL11C.GL_TEXTURE_2D,
                        0,
                        GL11C.GL_RGBA8,
                        width,
                        height,
                        0,
                        GL11C.GL_RGBA,
                        GL11C.GL_UNSIGNED_BYTE,
                        0L
                    )
                }

                check(GL11C.glGetTexLevelParameteri(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_WIDTH) == width) {
                    "Could not allocate the Heart composition color texture"
                }

                GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST)
                GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST)
                GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
                GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)

                return texture
            } catch (error: Exception) {
                GlStateManager._deleteTexture(texture)
                throw error
            } finally {
                GlStateManager._bindTexture(bound)
            }
        }

        fun sortDepths(images: HeartLayerImages, projection: Matrix4f, rect: IntArray) {
            val shader = checkNotNull(HeartBlackHoleShaders.layerDepthShader)
            shader.setSampler("Centers", images.centers)
            shader.getUniform("HeartImages")?.set(5)
            shader.getUniform("InverseProjection")?.set(Matrix4f(projection).invert())
            shader.getUniform("FrameSize")?.set(images.width.toFloat(), images.height.toFloat())
            shader.getUniform("Origin")?.set(rect[0].toFloat(), rect[1].toFloat())
            shader.getUniform("Count")?.set(images.count)
            images.bindRects(shader)
            if (images.count <= 3) {
                // The common cases need only one packed R/RG/RGBA image. The material sorts three scalars.
                GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)
                GL30C.glFramebufferTextureLayer(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, depths, 0, 0)
                GL11C.glDrawBuffer(GL30C.GL_COLOR_ATTACHMENT0)
                RenderSystem.viewport(0, 0, rect[2], rect[3])
                HeartLayerImages.preparePost(false)
                HeartLayerImages.bindArray(5, images.colors)
                shader.setSampler("Previous", images.centers)
                shader.getUniform("Rank")?.set(-1)
                try {
                    HeartLayerImages.drawQuad(shader)
                } finally {
                    GL30C.glFramebufferTexture2D(
                        GL30C.GL_FRAMEBUFFER,
                        GL30C.GL_COLOR_ATTACHMENT0,
                        GL11C.GL_TEXTURE_2D,
                        bins[0],
                        0
                    )
                    GL20C.glDrawBuffers(DRAW_BUFFERS[binCount - 1])
                }

                return
            }

            for (rank in 0 until images.count) {
                val target = sorting[rank % 2]
                target.bindWrite(true)
                RenderSystem.viewport(0, 0, rect[2], rect[3])
                HeartLayerImages.preparePost(false)
                HeartLayerImages.bindArray(5, images.colors)
                shader.setSampler("Previous", sorting[(rank + 1) % 2].colorTextureId)
                shader.getUniform("Rank")?.set(rank)
                HeartLayerImages.drawQuad(shader)
                GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, target.frameBufferId)
                HeartLayerImages.bindArray(6, depths)
                GL12C.glCopyTexSubImage3D(GL30C.GL_TEXTURE_2D_ARRAY, 0, 0, 0, rank, 0, 0, rect[2], rect[3])
            }
        }

        fun beginLayers(output: RenderTarget, rect: IntArray) {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, output.frameBufferId)
            RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
            GlStateManager._bindTexture(background)
            GL11C.glCopyTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, rect[0], rect[1], rect[2], rect[3])
            GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)
            HeartBlackHoleRenderState.disableScissor()
            HeartBlackHoleRenderState.prepareColorWrite()
            RenderSystem.enableScissor(0, 0, rect[2], rect[3])
            for (index in 0 until binCount) {
                GL30C.glClearBufferfv(GL11C.GL_COLOR, index, ZERO)
            }
            RenderSystem.viewport(-rect[0], -rect[1], output.viewWidth, output.viewHeight)
        }

        fun resolve(output: RenderTarget, images: HeartLayerImages, rect: IntArray, base: Int) {
            val shader = checkNotNull(HeartBlackHoleShaders.layerResolveShader)
            output.bindWrite(true)
            HeartLayerImages.preparePost(false)
            RenderSystem.colorMask(true, true, true, false)
            RenderSystem.enableScissor(rect[0], rect[1], rect[2], rect[3])
            try {
                shader.setSampler("Base", background)
                for (index in 0..3) {
                    shader.setSampler("Layer$index", bins[minOf(index, binCount - 1)])
                }
                HeartLayerImages.bindArray(5, images.colors)
                HeartLayerImages.bindArray(6, depths)
                shader.getUniform("HeartImages")?.set(5)
                shader.getUniform("LayerDepths")?.set(6)
                shader.getUniform("Count")?.set(images.count)
                images.bindRects(shader)
                shader.getUniform("LayerBase")?.set(base)
                shader.getUniform("Origin")?.set(rect[0].toFloat(), rect[1].toFloat())
                HeartLayerImages.drawQuad(shader)
            } finally {
                RenderSystem.disableScissor()
            }
        }

        override fun close() {
            sorting.forEach { it.destroyBuffers() }
            GL30C.glDeleteFramebuffers(framebuffer)
            GlStateManager._deleteTexture(background)

            for (texture in bins) {
                if (texture != 0) GlStateManager._deleteTexture(texture)
            }
            if (packedDepths != 0) GlStateManager._deleteTexture(packedDepths)
            if (rankedDepths != 0) GlStateManager._deleteTexture(rankedDepths)
        }

        companion object {
            private val DRAW_BUFFERS = Array(4) { last -> IntArray(last + 1) { GL30C.GL_COLOR_ATTACHMENT0 + it } }
            private val ZERO = FloatArray(4)
        }
    }

    internal fun projectHeart(projection: Matrix4f, center: org.joml.Vector3f, radius: Float, width: Int, height: Int): IntArray =
        SceneOrder.screenBounds(
            projection,
            AABB.ofSize(Vec3(center.x.toDouble(), center.y.toDouble(), center.z.toDouble()), radius * 2.0, radius * 2.0, radius * 2.0),
            width,
            height,
            32
        )
}
