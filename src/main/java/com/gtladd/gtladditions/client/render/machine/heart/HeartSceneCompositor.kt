package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import kotlin.math.ceil
import kotlin.math.floor

/** Local depth bins shared by transparent machine effects overlapping black holes. */
internal object HeartSceneCompositor : AutoCloseable {
    private val images = HeartLayerImages()
    private var targets: Targets? = null
    private const val RETENTION_NANOS = 2_000_000_000L
    private var lastImagesUse = 0L
    private var lastTargetsUse = 0L

    fun beginFrame() {
        val now = System.nanoTime()
        if (targets != null && now - lastTargetsUse > RETENTION_NANOS) {
            targets?.close()
            targets = null
        }
        if (images.count > 0 && now - lastImagesUse > RETENTION_NANOS) images.close()
    }

    fun overlap(
        effects: List<HeartSceneEffect>,
        hearts: List<HeartBlackHoleRenderEntry>,
        pose: Matrix4f,
        projection: Matrix4f,
        camera: Vec3,
        width: Int,
        height: Int
    ): IntArray? {
        val effectBounds = effects.map { it.bounds }
        val heartBounds = hearts.map { heart ->
            val center = heart.profile.center.add(Vec3.atLowerCornerOf(heart.blockEntity.blockPos))
            val radius = HeartBlackHoleVisualConfig.volumeProxyRadius(heart.profile.tick).toDouble()
            AABB.ofSize(center, radius * 2, radius * 2, radius * 2)
        }
        return region(effectBounds, heartBounds, pose, projection, camera, width, height)
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
        val shipRects = shipBounds.map { screenBounds(vp, it.move(-camera.x, -camera.y, -camera.z), width, height, 2) }
        var x0 = width
        var y0 = height
        var x1 = 0
        var y1 = 0
        for (bounds in heartBounds) {
            val rect = screenBounds(vp, bounds.move(-camera.x, -camera.y, -camera.z), width, height, 12)
            for (ship in shipRects) {
                val left = maxOf(rect[0], ship[0])
                val bottom = maxOf(rect[1], ship[1])
                val right = minOf(rect[0] + rect[2], ship[0] + ship[2])
                val top = minOf(rect[1] + rect[3], ship[1] + ship[3])
                if (right > left && top > bottom) {
                    x0 = minOf(x0, left)
                    y0 = minOf(y0, bottom)
                    x1 = maxOf(x1, right)
                    y1 = maxOf(y1, top)
                }
            }
        }

        if (x1 <= x0 || y1 <= y0) return null
        // Preserve the derivative-quad alignment used by shield and chart AA.
        x0 = x0 / 16 * 16
        y0 = y0 / 16 * 16
        x1 = minOf(width, (x1 + 15) / 16 * 16)
        y1 = minOf(height, (y1 + 15) / 16 * 16)

        return intArrayOf(x0, y0, x1 - x0, y1 - y0)
    }

    fun render(
        context: DeferredRenderContext,
        hearts: List<HeartBlackHoleRenderEntry>,
        effects: List<HeartSceneEffect>,
        projection: Matrix4f,
        handMask: Int
    ): Boolean {
        if (hearts.any { it.blockEntity.isRemoved } || effects.any { it.blockEntity.isRemoved }) {
            return render(context, hearts.filterNot { it.blockEntity.isRemoved }, effects.filterNot { it.blockEntity.isRemoved }, projection, handMask)
        }
        if (hearts.isEmpty() || effects.isEmpty() || effects.any { !it.supportsLayeredRendering }) return false
        val output = Minecraft.getInstance().mainRenderTarget
        val width = output.viewWidth
        val height = output.viewHeight
        val rect = overlap(effects, hearts, context.poseStack.last().pose(), projection, context.cameraPosition, width, height) ?: return false
        val bounds = hearts.map { heart ->
            val center = heart.profile.center.add(Vec3.atLowerCornerOf(heart.blockEntity.blockPos))
            val radius = HeartBlackHoleVisualConfig.volumeProxyRadius(heart.profile.tick).toDouble()
            HeartSceneOrder.Bounds(AABB.ofSize(center, radius * 2, radius * 2, radius * 2), center, radius)
        } + effects.map { HeartSceneOrder.Bounds(it.bounds) }
        val order = HeartSceneOrder.sort(bounds, context.poseStack.last().pose(), projection, context.cameraPosition, width, height)
        return HeartBlackHoleRenderState.withSavedState {
            HeartLayerImages.withArrayState {
                output.bindWrite(true)
                if (order != null) return@withArrayState renderOrdered(context, hearts, effects, order, output, handMask)
                if (HeartBlackHoleShaders.layerDepthShader == null || HeartBlackHoleShaders.layerResolveShader == null) return@withArrayState false
                if (hearts.size > GL11C.glGetInteger(GL30C.GL_MAX_ARRAY_TEXTURE_LAYERS)) return@withArrayState false
                if (!HeartBlackHoleRenderer.renderLayers(hearts, context.poseStack, context.cameraPosition, width, height, images, handMask)) return@withArrayState false
                lastImagesUse = System.nanoTime()
                val local = ensureTargets(rect[2], rect[3], images.count)
                output.bindWrite(true)
                if (effects.distinctBy { it.depthCaptureKey }.any { !it.captureSceneDepth() }) return@withArrayState false
                compose(output, images, local, projection, rect) { base, cropped ->
                    effects.forEach { entry -> entry.renderLayered(context, local.depths, images.count, base, rect, cropped, handMask) }
                }
                true
            }
        }
    }

    private fun renderOrdered(
        context: DeferredRenderContext,
        hearts: List<HeartBlackHoleRenderEntry>,
        effects: List<HeartSceneEffect>,
        order: IntArray,
        output: RenderTarget,
        handMask: Int
    ): Boolean {
        if (effects.filter { it.orderedNeedsSceneDepth }.distinctBy { it.depthCaptureKey }.any { !it.captureSceneDepth() }) return false
        if (hearts.size > 1) {
            if (hearts.size > GL11C.glGetInteger(GL30C.GL_MAX_ARRAY_TEXTURE_LAYERS)) return false
            if (!HeartBlackHoleRenderer.renderLayers(hearts, context.poseStack, context.cameraPosition, output.viewWidth, output.viewHeight, images, handMask)) return false
            lastImagesUse = System.nanoTime()
        }
        output.bindWrite(true)
        RenderSystem.colorMask(true, true, true, false)
        GL11C.glFrontFace(GL11C.GL_CCW)
        for (index in order) {
            if (index < hearts.size) {
                if (hearts.size == 1) {
                    HeartBlackHoleRenderer.render(hearts, context.poseStack, context.cameraPosition, handMask)
                } else {
                    images.copy(images.layerFor(hearts[index].blockEntity.blockPos.asLong()), blend = true, writeAlpha = false)
                }
            } else {
                effects[index - hearts.size].renderOrdered(context, handMask)
            }
        }
        return true
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

    private fun ensureTargets(width: Int, height: Int, count: Int): Targets {
        lastTargetsUse = System.nanoTime()
        val old = targets
        val output = Minecraft.getInstance().mainRenderTarget
        if (old != null &&
            old.width >= width &&
            old.height >= height &&
            old.count == count &&
            old.width <= output.viewWidth &&
            old.height <= output.viewHeight
        ) {
            return old
        }

        val capacityWidth = minOf(output.viewWidth, (maxOf(width, old?.width ?: 0) + 255) / 256 * 256)
        val capacityHeight = minOf(output.viewHeight, (maxOf(height, old?.height ?: 0) + 255) / 256 * 256)
        old?.close()
        targets = null
        return Targets(capacityWidth, capacityHeight, count).also { targets = it }
    }

    override fun close() {
        targets?.close()
        targets = null
        images.close()
        lastImagesUse = 0L
        lastTargetsUse = 0L
    }

    internal class Targets(val width: Int, val height: Int, val count: Int) : AutoCloseable {
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
        screenBounds(
            projection,
            AABB.ofSize(Vec3(center.x.toDouble(), center.y.toDouble(), center.z.toDouble()), radius * 2.0, radius * 2.0, radius * 2.0),
            width,
            height,
            32
        )

    internal fun screenBounds(vp: Matrix4f, bounds: AABB, width: Int, height: Int, pad: Int): IntArray {
        var x0 = width.toFloat()
        var y0 = height.toFloat()
        var x1 = 0f
        var y1 = 0f
        val point = Vector4f()
        for (corner in 0..7) {
            vp.transform(
                point.set(
                    (if (corner and 1 == 0) bounds.minX else bounds.maxX).toFloat(),
                    (if (corner and 2 == 0) bounds.minY else bounds.maxY).toFloat(),
                    (if (corner and 4 == 0) bounds.minZ else bounds.maxZ).toFloat(),
                    1f
                )
            )
            if (point.w <= 0f) return intArrayOf(0, 0, width, height)
            val x = (point.x / point.w * 0.5f + 0.5f) * width
            val y = (point.y / point.w * 0.5f + 0.5f) * height
            x0 = minOf(x0, x)
            y0 = minOf(y0, y)
            x1 = maxOf(x1, x)
            y1 = maxOf(y1, y)
        }
        val left = maxOf(0, floor(x0).toInt() - pad)
        val bottom = maxOf(0, floor(y0).toInt() - pad)
        val right = minOf(width, ceil(x1).toInt() + pad)
        val top = minOf(height, ceil(y1).toInt() + pad)
        return intArrayOf(left, bottom, maxOf(0, right - left), maxOf(0, top - bottom))
    }
}