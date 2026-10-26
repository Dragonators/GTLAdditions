package com.gtladd.gtladditions.client.render.machine.deferred

import com.google.common.collect.ImmutableSet
import com.mojang.blaze3d.platform.GlStateManager
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer
import net.irisshaders.iris.targets.RenderTargets
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL30C
import java.util.ArrayDeque

/** Owns the two colortex0 targets and nested bindings for one Oculus pipeline. */
@OnlyIn(Dist.CLIENT)
class DeferredFramebufferTargets(
    private val renderTargets: RenderTargets,
    private val flippedBeforeTranslucent: ImmutableSet<Int>,
    private val flippedAfterTranslucent: ImmutableSet<Int>
) : AutoCloseable {
    private var beforeTranslucent: GlFramebuffer? = null
    private var afterTranslucent: GlFramebuffer? = null
    private var width = -1
    private var height = -1
    private val bindings = ArrayDeque<Binding>()

    fun begin(isBeforeTranslucent: Boolean, isMainBound: Boolean) {
        ensureTargets()
        bindings.push(
            Binding(
                GlStateManager._getInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING),
                GlStateManager._getInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING),
                isMainBound
            )
        )
        val target = if (isBeforeTranslucent) beforeTranslucent else afterTranslucent
        checkNotNull(target).bind()
    }

    fun end(isMainBound: Boolean): Boolean {
        val previous = bindings.pollFirst() ?: return isMainBound
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previous.readFramebuffer)
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previous.drawFramebuffer)
        return previous.mainBound
    }

    private fun ensureTargets() {
        val currentWidth = renderTargets.currentWidth
        val currentHeight = renderTargets.currentHeight
        if (beforeTranslucent != null && afterTranslucent != null && width == currentWidth && height == currentHeight) return

        close()
        beforeTranslucent = renderTargets.createGbufferFramebuffer(flippedBeforeTranslucent, intArrayOf(0))
        afterTranslucent = renderTargets.createGbufferFramebuffer(flippedAfterTranslucent, intArrayOf(0))
        width = currentWidth
        height = currentHeight
    }

    override fun close() {
        beforeTranslucent?.let(renderTargets::destroyFramebuffer)
        afterTranslucent?.let(renderTargets::destroyFramebuffer)
        beforeTranslucent = null
        afterTranslucent = null
        width = -1
        height = -1
        bindings.clear()
    }

    private data class Binding(val readFramebuffer: Int, val drawFramebuffer: Int, val mainBound: Boolean)
}