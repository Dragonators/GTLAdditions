package com.gtladd.gtladditions.client.render.machine.heart

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL11C

internal object HeartBlackHolePostTargets {
    private const val BLUR_SCALE = 4
    private var targets: HeartBlackHoleTargets? = null

    fun ensure(width: Int, height: Int): HeartBlackHoleTargets {
        val existing = targets
        val blurWidth = (width / BLUR_SCALE).coerceAtLeast(1)
        val blurHeight = (height / BLUR_SCALE).coerceAtLeast(1)
        if (
            existing != null &&
            existing.effect.viewWidth == width &&
            existing.effect.viewHeight == height &&
            existing.effectBlurA.viewWidth == blurWidth &&
            existing.effectBlurA.viewHeight == blurHeight
        ) {
            return existing
        }

        val next = HeartBlackHoleTargets(
            createTarget(width, height, true),
            createTarget(width, height, true),
            createTarget(blurWidth, blurHeight, false),
            createTarget(blurWidth, blurHeight, false),
            createTarget(blurWidth, blurHeight, false),
            createTarget(blurWidth, blurHeight, false)
        )
        targets?.destroy()
        targets = next
        return next
    }

    private fun createTarget(width: Int, height: Int, useDepth: Boolean): TextureTarget =
        TextureTarget(width, height, useDepth, Minecraft.ON_OSX).apply {
            setClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            setFilterMode(GL11C.GL_LINEAR)
        }
}

internal data class HeartBlackHoleTargets(
    val effect: TextureTarget,
    val mask: TextureTarget,
    val effectBlurA: TextureTarget,
    val effectBlurB: TextureTarget,
    val maskBlurA: TextureTarget,
    val maskBlurB: TextureTarget
) {
    fun clearVolumeTargets() {
        clearColorOnly(effect)
        clearColorOnly(mask)
    }

    fun destroy() {
        effect.destroyBuffers()
        mask.destroyBuffers()
        effectBlurA.destroyBuffers()
        effectBlurB.destroyBuffers()
        maskBlurA.destroyBuffers()
        maskBlurB.destroyBuffers()
    }
}

private fun clearColorOnly(target: RenderTarget) {
    target.bindWrite(true)
    HeartBlackHoleFramebuffer.prepareFramebufferWrite(false)
    GlStateManager._clearColor(0.0f, 0.0f, 0.0f, 0.0f)
    GlStateManager._clear(GL11C.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX)
}