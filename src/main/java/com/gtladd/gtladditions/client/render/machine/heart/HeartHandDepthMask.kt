package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.client.render.machine.deferred.DeferredSceneRenderer
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import net.irisshaders.iris.targets.RenderTargets
import net.minecraft.client.Minecraft
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11C

/** Reuses Iris's solid-hand depth snapshots and captures only an actual translucent-hand pass. */
object HeartHandDepthMask {
    private val identity = Matrix4f()
    private var beforeTranslucent: RenderTarget? = null
    private var mask: RenderTarget? = null
    private var solidHand = false
    private var translucentHand = false
    private var translucentCaptured = false
    private var ready = false

    fun beginFrame() {
        solidHand = false
        translucentHand = false
        translucentCaptured = false
        ready = false
    }

    fun isReady(): Boolean = ready

    fun textureId(): Int = if (ready && (solidHand || translucentHand)) mask?.colorTextureId ?: 0 else 0

    @JvmStatic
    fun beginSolidHand() {
        if (DeferredSceneRenderer.needsHandMask()) solidHand = true
    }

    @JvmStatic
    fun beginTranslucentHand() {
        if (!DeferredSceneRenderer.needsHandMask()) return
        translucentHand = true
        translucentCaptured = false
        if (HeartBlackHoleShaders.handMaskShader == null) return
        val main = Minecraft.getInstance().mainRenderTarget
        HeartBlackHoleRenderState.withSavedState {
            ensureTargets(main)
            beforeTranslucent!!.copyDepthFrom(main)
            translucentCaptured = true
        }
    }

    @JvmStatic
    fun finishFrame(renderTargets: RenderTargets) {
        if (!DeferredSceneRenderer.needsHandMask()) return
        if (!solidHand && !translucentHand) {
            ready = true
            return
        }

        val shader = HeartBlackHoleShaders.handMaskShader ?: return
        val main = Minecraft.getInstance().mainRenderTarget
        HeartBlackHoleRenderState.withSavedState {
            ensureTargets(main)
            if (translucentHand && !translucentCaptured) return@withSavedState
            if (!HeartBlackHoleFramebuffer.bindForDraw(mask!!)) return@withSavedState
            HeartBlackHoleRenderState.prepareColorWrite()
            RenderSystem.disableDepthTest()
            RenderSystem.disableCull()
            RenderSystem.disableBlend()
            // These snapshots bracket only the solid hand, before any translucent world draws.
            shader.setSampler("SolidBeforeDepthSampler", renderTargets.depthTextureNoHand.textureId)
            shader.setSampler("SolidAfterDepthSampler", renderTargets.depthTextureNoTranslucents.textureId)
            shader.setSampler(
                "TranslucentBeforeDepthSampler",
                if (translucentHand) beforeTranslucent!!.depthTextureId else renderTargets.depthTexture
            )
            shader.setSampler("TranslucentAfterDepthSampler", renderTargets.depthTexture)
            shader.getUniform("SolidHandEnabled")?.set(if (solidHand) 1f else 0f)
            shader.getUniform("TranslucentHandEnabled")?.set(if (translucentHand) 1f else 0f)
            try {
                RenderUtils.withBoundBuffer(HeartBlackHoleQuadBuffers.screenQuadBuffer) {
                    HeartBlackHoleQuadBuffers.screenQuadBuffer.drawWithShader(identity, identity, shader)
                }
                ready = true
            } finally {
                shader.clear()
            }
        }
    }

    @JvmStatic
    fun destroy() {
        beforeTranslucent?.destroyBuffers()
        mask?.destroyBuffers()
        beforeTranslucent = null
        mask = null
        beginFrame()
    }

    private fun ensureTargets(main: RenderTarget) {
        val current = beforeTranslucent
        if (current != null &&
            current.viewWidth == main.viewWidth &&
            current.viewHeight == main.viewHeight &&
            current.isStencilEnabled == main.isStencilEnabled
        ) {
            return
        }

        val nextBefore = DepthTarget()
        val nextMask = ColorTarget()
        try {
            nextBefore.resize(main.viewWidth, main.viewHeight, Minecraft.ON_OSX)
            if (main.isStencilEnabled) nextBefore.enableStencil()
            nextMask.resize(main.viewWidth, main.viewHeight, Minecraft.ON_OSX)
            nextMask.setFilterMode(GL11C.GL_NEAREST)
        } catch (exception: Exception) {
            nextBefore.destroyBuffers()
            nextMask.destroyBuffers()
            throw exception
        }

        beforeTranslucent?.destroyBuffers()
        mask?.destroyBuffers()
        beforeTranslucent = nextBefore
        mask = nextMask
        translucentCaptured = false
        ready = false
    }

    private class DepthTarget : RenderTarget(true)

    private class ColorTarget : RenderTarget(false)
}