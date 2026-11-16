package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.api.client.scene.SceneDepth
import com.gtladd.gtladditions.client.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL14C

internal object HeartBlackHoleCompositeRenderer {
    private val IDENTITY_MATRIX = Matrix4f()
    private val BLOOM_SAMPLERS = arrayOf("BloomSampler", "BloomSampler1", "BloomSampler2", "BloomSampler3", "BloomSampler4")

    fun renderPendingComposite(
        width: Int,
        height: Int,
        pending: PendingHeartComposite,
        handMask: Int = 0,
        writeAlpha: Boolean = false,
        origin: IntArray? = null
    ) {
        val compositeShader = HeartBlackHoleShaders.compositeShader ?: return
        HeartBlackHoleRenderState.withSavedState {
            if (!HeartBlackHoleFramebuffer.isComplete("composite framebuffer")) return@withSavedState
            RenderSystem.viewport(-(origin?.get(0) ?: 0), -(origin?.get(1) ?: 0), width, height)
            compositeShader.getUniform("CompositeOrigin")?.set((origin?.get(0) ?: 0).toFloat(), (origin?.get(1) ?: 0).toFloat())
            HeartBlackHoleRenderState.prepareColorWrite(writeAlpha = writeAlpha)
            RenderSystem.disableDepthTest()
            RenderSystem.disableCull()
            RenderSystem.enableBlend()
            RenderSystem.blendEquation(GL14C.GL_FUNC_ADD)
            RenderSystem.blendFuncSeparate(
                GL11C.GL_ONE,
                GL11C.GL_ONE_MINUS_SRC_ALPHA,
                GL11C.GL_ONE,
                GL11C.GL_ONE_MINUS_SRC_ALPHA
            )

            try {
                drawCompositeQuad(compositeShader, pending, handMask)
            } finally {
                compositeShader.clear()
            }
        }
    }

    private fun drawCompositeQuad(
        compositeShader: ShaderInstance,
        pending: PendingHeartComposite,
        handMask: Int
    ) {
        compositeShader.setSampler("EffectSampler", pending.targets.effect.colorTextureId)
        compositeShader.setSampler("BlurredEffectSampler", pending.targets.effectBlurred.colorTextureId)
        compositeShader.setSampler("MaskSampler", pending.targets.effect.colorTextureId)
        compositeShader.setSampler("BlurredMaskSampler", pending.targets.maskBlurred.colorTextureId)
        compositeShader.setSampler("HandMaskSampler", if (handMask != 0) handMask else pending.targets.effect.colorTextureId)
        compositeShader.getUniform("HandMaskEnabled")?.set(if (handMask != 0) 1f else 0f)
        HeartBlackHoleFramebuffer.bindSceneDepth(compositeShader, pending.depth)
        compositeShader.getUniform("HeartSpheres")?.set(pending.spheres)
        pending.targets.bloomLevels.forEachIndexed { index, level ->
            compositeShader.setSampler(BLOOM_SAMPLERS[index], level.blurred.colorTextureId)
        }

        compositeShader.getUniform("BlurInputScale")?.set(
            pending.width.toFloat() / pending.blurWidth.toFloat(),
            pending.height.toFloat() / pending.blurHeight.toFloat()
        )

        RenderUtils.withBoundBuffer(HeartBlackHoleQuadBuffers.screenQuadBuffer) {
            HeartBlackHoleQuadBuffers.screenQuadBuffer.drawWithShader(IDENTITY_MATRIX, IDENTITY_MATRIX, compositeShader)
        }
    }
}

internal data class PendingHeartComposite(
    val targets: HeartBlackHoleTargets,
    val width: Int,
    val height: Int,
    val blurWidth: Int,
    val blurHeight: Int,
    val depth: SceneDepth,
    /** One proxy sphere per column, view-space centre and radius; a zero radius ends the list. */
    val spheres: Matrix4f
)