package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.api.client.scene.SceneDepth
import com.gtladd.gtladditions.client.render.scene.SceneOrder
import com.gtladd.gtladditions.utils.RenderUtils
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
    private const val RETENTION_NANOS = 2_000_000_000L
    private val images = HeartLayerImages()

    private val subset = HeartLayerImages()

    /** Keyed by Heart count. */
    private val targets = HashMap<Int, Targets>()
    private var lastImagesUse = 0L
    private var lastSubsetUse = 0L

    /** Minecraft has already bound main when this runs. */
    fun beginFrame() {
        val now = System.nanoTime()
        val idleImages = images.count > 0 && now - lastImagesUse > RETENTION_NANOS
        val idleSubset = subset.count > 0 && now - lastSubsetUse > RETENTION_NANOS
        if (!idleImages && !idleSubset && targets.values.none { now - it.lastUse > RETENTION_NANOS }) return
        RenderUtils.withTargetBindings {
            targets.values.removeIf { target -> (now - target.lastUse > RETENTION_NANOS).also { idle -> if (idle) target.close() } }
            if (idleImages) images.close()
            if (idleSubset) subset.close()
        }
    }

    fun prepareLayers(
        entries: List<HeartBlackHoleRenderEntry>,
        poseStack: PoseStack,
        cameraPosition: Vec3,
        handMask: Int,
        depth: SceneDepth
    ): Boolean {
        lastImagesUse = System.nanoTime()
        return HeartBlackHoleRenderer.renderLayers(entries, poseStack, cameraPosition, images, handMask, depth)
    }

    fun copyLayer(sourceKey: Long) = images.copy(images.layerFor(sourceKey), blend = true, writeAlpha = false)

    fun layers(sourceKeys: List<Long>): HeartLayerImages {
        if (sourceKeys.size == images.count) return images
        val sources = sourceKeys.map(images::layerFor).sorted()
        subset.ensure(images.width, images.height, sources.size, sources.map(images::rect))
        sources.forEachIndexed { index, source -> subset.copyLayer(images, source, index) }
        subset.uploadCenters()
        lastSubsetUse = System.nanoTime()

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
        val now = System.nanoTime()
        val count = images.count
        val old = targets[count]
        if (old != null &&
            old.width >= width &&
            old.height >= height &&
            old.width <= images.width &&
            old.height <= images.height
        ) {
            old.lastUse = now
            return old
        }

        val capacityWidth = minOf(images.width, (maxOf(width, old?.width ?: 0) + 255) / 256 * 256)
        val capacityHeight = minOf(images.height, (maxOf(height, old?.height ?: 0) + 255) / 256 * 256)
        old?.close()
        targets.remove(count)
        return Targets(capacityWidth, capacityHeight, count).also {
            it.lastUse = now
            targets[count] = it
        }
    }

    override fun close() {
        targets.values.forEach(Targets::close)
        targets.clear()
        images.close()
        subset.close()
        lastImagesUse = 0L
        lastSubsetUse = 0L
    }

    internal class Targets(val width: Int, val height: Int, val count: Int) : AutoCloseable {
        var lastUse = 0L
        val depths: Int
        private val sorting: List<RenderTarget>
        private val background: RenderTarget
        private val bins: List<RenderTarget>
        private val framebuffer: Int

        init {
            val allocated = ArrayList<RenderTarget>()
            var array = 0
            var frame = 0
            try {
                val format = when (count) {
                    1 -> GL30C.GL_R32F
                    2 -> GL30C.GL_RG32F
                    3 -> GL30C.GL_RGBA32F
                    else -> GL30C.GL_RG32F
                }

                val external = when (format) {
                    GL30C.GL_R32F -> GL11C.GL_RED
                    GL30C.GL_RG32F -> GL30C.GL_RG
                    else -> GL11C.GL_RGBA
                }

                array = HeartLayerImages.createArray(width, height, if (count <= 3) 1 else count, format, external)
                depths = array
                sorting = List(if (count <= 3) 0 else 2) {
                    HeartLayerImages.createTarget(width, height, GL30C.GL_RG32F).also(allocated::add)
                }

                background = HeartLayerImages.createTarget(width, height, GL11C.GL_RGBA8).also(allocated::add)
                bins = List(minOf(count + 1, 4)) { HeartLayerImages.createTarget(width, height, GL11C.GL_RGBA8).also(allocated::add) }
                frame = GL30C.glGenFramebuffers()
                framebuffer = frame
                GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)
                bins.forEachIndexed { index, target ->
                    GL30C.glFramebufferTexture2D(
                        GL30C.GL_FRAMEBUFFER,
                        GL30C.GL_COLOR_ATTACHMENT0 + index,
                        GL11C.GL_TEXTURE_2D,
                        target.colorTextureId,
                        0
                    )
                }

                GL20C.glDrawBuffers(IntArray(bins.size) { GL30C.GL_COLOR_ATTACHMENT0 + it })
                check(GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) == GL30C.GL_FRAMEBUFFER_COMPLETE)
            } catch (exception: Exception) {
                allocated.forEach { it.destroyBuffers() }
                if (array != 0) GlStateManager._deleteTexture(array)
                if (frame != 0) GL30C.glDeleteFramebuffers(frame)
                throw exception
            }
        }

        fun sortDepths(images: HeartLayerImages, projection: Matrix4f, rect: IntArray) {
            val shader = checkNotNull(HeartBlackHoleShaders.layerDepthShader)
            shader.setSampler("Centers", images.centers)
            shader.getUniform("HeartImages")?.set(5)
            shader.getUniform("InverseProjection")?.set(Matrix4f(projection).invert())
            shader.getUniform("FrameSize")?.set(images.width.toFloat(), images.height.toFloat())
            shader.getUniform("Origin")?.set(rect[0].toFloat(), rect[1].toFloat())
            shader.getUniform("Count")?.set(count)
            images.bindRects(shader)
            if (count <= 3) {
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
                        bins[0].colorTextureId,
                        0
                    )
                    GL20C.glDrawBuffers(IntArray(bins.size) { GL30C.GL_COLOR_ATTACHMENT0 + it })
                }

                return
            }

            for (rank in 0 until count) {
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
            GlStateManager._bindTexture(background.colorTextureId)
            GL11C.glCopyTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, rect[0], rect[1], rect[2], rect[3])
            GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer)
            HeartBlackHoleRenderState.disableScissor()
            HeartBlackHoleRenderState.prepareColorWrite()
            RenderSystem.enableScissor(0, 0, rect[2], rect[3])
            bins.indices.forEach { index -> GL30C.glClearBufferfv(GL11C.GL_COLOR, index, ZERO) }
            RenderSystem.viewport(-rect[0], -rect[1], output.viewWidth, output.viewHeight)
        }

        fun resolve(output: RenderTarget, images: HeartLayerImages, rect: IntArray, base: Int) {
            val shader = checkNotNull(HeartBlackHoleShaders.layerResolveShader)
            output.bindWrite(true)
            HeartLayerImages.preparePost(false)
            RenderSystem.colorMask(true, true, true, false)
            RenderSystem.enableScissor(rect[0], rect[1], rect[2], rect[3])
            try {
                shader.setSampler("Base", background.colorTextureId)
                for (index in 0..3) shader.setSampler("Layer$index", bins[minOf(index, bins.lastIndex)].colorTextureId)
                HeartLayerImages.bindArray(5, images.colors)
                HeartLayerImages.bindArray(6, depths)
                shader.getUniform("HeartImages")?.set(5)
                shader.getUniform("LayerDepths")?.set(6)
                shader.getUniform("Count")?.set(count)
                images.bindRects(shader)
                shader.getUniform("LayerBase")?.set(base)
                shader.getUniform("FrameSize")?.set(images.width.toFloat(), images.height.toFloat())
                shader.getUniform("Origin")?.set(rect[0].toFloat(), rect[1].toFloat())
                HeartLayerImages.drawQuad(shader)
            } finally {
                RenderSystem.disableScissor()
            }
        }

        override fun close() {
            sorting.forEach { it.destroyBuffers() }
            background.destroyBuffers()
            bins.forEach { it.destroyBuffers() }
            GL30C.glDeleteFramebuffers(framebuffer)
            GlStateManager._deleteTexture(depths)
        }

        companion object {
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