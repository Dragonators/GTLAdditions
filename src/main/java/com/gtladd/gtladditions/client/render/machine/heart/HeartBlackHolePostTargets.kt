package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL30C

internal object HeartBlackHolePostTargets {
    private const val BLUR_SCALE = 4
    private const val MATERIAL_SCALE = 2
    private var targets: HeartBlackHoleTargets? = null

    fun ensure(width: Int, height: Int): HeartBlackHoleTargets {
        val existing = targets
        val blurWidth = (width / BLUR_SCALE).coerceAtLeast(1)
        val blurHeight = (height / BLUR_SCALE).coerceAtLeast(1)
        if (
            existing != null &&
            existing.effect.viewWidth == width &&
            existing.effect.viewHeight == height &&
            existing.effectBlurred.viewWidth == blurWidth &&
            existing.effectBlurred.viewHeight == blurHeight
        ) {
            return existing
        }

        val allocated = ArrayList<RenderTarget>(16)
        val next = try {
            HeartBlackHoleTargets(
                createTarget(
                    ((width + MATERIAL_SCALE - 1) / MATERIAL_SCALE).coerceAtLeast(1),
                    ((height + MATERIAL_SCALE - 1) / MATERIAL_SCALE).coerceAtLeast(1),
                    allocated,
                    hdr = true
                ),
                createTarget(width, height, allocated, hdr = true),
                createTarget(blurWidth, blurHeight, allocated, hdr = true),
                createTarget(blurWidth, blurHeight, allocated, hdr = true),
                createTarget(blurWidth, blurHeight, allocated),
                createTarget(blurWidth, blurHeight, allocated),
                List(5) { level ->
                    val divisor = 1 shl (level + 1)
                    HeartBlackHoleBloomLevel(
                        createTarget((width / divisor).coerceAtLeast(1), (height / divisor).coerceAtLeast(1), allocated, hdr = true),
                        createTarget((width / divisor).coerceAtLeast(1), (height / divisor).coerceAtLeast(1), allocated, hdr = true)
                    )
                }
            )
        } catch (exception: Exception) {
            allocated.forEach { it.destroyBuffers() }
            throw exception
        }

        targets?.destroy()
        targets = next
        return next
    }

    fun close() {
        targets?.destroy()
        targets = null
    }

    private fun createTarget(width: Int, height: Int, allocated: MutableList<RenderTarget>, hdr: Boolean = false): RenderTarget =
        RenderUtils.withTightlyPackedPixels {
            val target = ColorTarget()
            // Register ownership before allocating GL resources so partial allocation is cleaned up too.
            allocated.add(target)
            target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            target.resize(width, height, Minecraft.ON_OSX)
            if (hdr) {
                GlStateManager._bindTexture(target.colorTextureId)
                GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA16F, width, height, 0, GL11C.GL_RGBA, GL11C.GL_FLOAT, 0L)
            }

            target.setFilterMode(GL11C.GL_LINEAR)
            target
        }

    private class ColorTarget : RenderTarget(false)
}

internal data class HeartBlackHoleTargets(
    val material: RenderTarget,
    val effect: RenderTarget,
    val effectBlurred: RenderTarget,
    val effectBlurScratch: RenderTarget,
    val maskBlurred: RenderTarget,
    val maskBlurScratch: RenderTarget,
    val bloomLevels: List<HeartBlackHoleBloomLevel>
) {
    val bloom: RenderTarget get() = bloomLevels[0].blurred
    val bloomScratch: RenderTarget get() = bloomLevels[0].scratch

    fun clearVolumeTargets(): Boolean =
        HeartBlackHoleFramebuffer.clearColor(effect)

    fun destroy() {
        material.destroyBuffers()
        effect.destroyBuffers()
        effectBlurred.destroyBuffers()
        effectBlurScratch.destroyBuffers()
        maskBlurred.destroyBuffers()
        maskBlurScratch.destroyBuffers()
        bloomLevels.forEach { level ->
            level.blurred.destroyBuffers()
            level.scratch.destroyBuffers()
        }
    }
}

internal data class HeartBlackHoleBloomLevel(val blurred: RenderTarget, val scratch: RenderTarget)