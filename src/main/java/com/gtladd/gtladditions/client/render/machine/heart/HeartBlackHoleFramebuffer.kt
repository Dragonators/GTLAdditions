package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.SceneDepth
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL30C

internal object HeartBlackHoleFramebuffer {
    private val reportedProblems = HashSet<String>()

    /** Heart passes draw to colour-only targets, so sampling the scene depth while drawing them is safe. */
    fun bindSceneDepth(shader: ShaderInstance, depth: SceneDepth) {
        shader.getUniform("InverseProjection")?.set(Matrix4f(depth.inverseProjection))
        shader.getUniform("SceneSize")?.set(depth.width.toFloat(), depth.height.toFloat())
        shader.setSampler("SceneDepth", depth.texture)
    }

    fun bindForDraw(target: RenderTarget): Boolean {
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, target.frameBufferId)
        RenderSystem.viewport(0, 0, target.viewWidth, target.viewHeight)
        return isComplete("effect framebuffer")
    }

    fun clearColor(target: RenderTarget): Boolean {
        if (!bindForDraw(target)) return false
        HeartBlackHoleRenderState.disableScissor()
        HeartBlackHoleRenderState.prepareColorWrite()
        GlStateManager._clearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GlStateManager._clear(GL11C.GL_COLOR_BUFFER_BIT, false)

        return true
    }

    fun isComplete(description: String): Boolean {
        val status = GL30C.glCheckFramebufferStatus(GL30C.GL_DRAW_FRAMEBUFFER)
        if (status == GL30C.GL_FRAMEBUFFER_COMPLETE) return true
        reportProblem("$description is incomplete: 0x${status.toString(16)}")
        return false
    }

    private fun reportProblem(problem: String) {
        if (reportedProblems.add(problem)) {
            GTLAdditions.LOGGER.warn("Skipping Heart of the Universe effect: {}", problem)
        }
    }
}