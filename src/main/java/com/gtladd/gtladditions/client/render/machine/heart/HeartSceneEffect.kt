package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import org.lwjgl.opengl.GL13C

/** Transparent effects that can be placed on either side of a Heart at each pixel. */
internal interface HeartSceneEffect {
    val blockEntity: BlockEntity
    val bounds: AABB
    val supportsLayeredRendering: Boolean

    /** Entries from the same renderer share one opaque depth snapshot per composition. */
    val depthCaptureKey: Any

    val orderedNeedsSceneDepth: Boolean
        get() = true

    fun renderOrdered(context: DeferredRenderContext, handMask: Int) =
        renderLayered(context, 0, 0, 0, FULL_VIEW, false, handMask)

    fun captureSceneDepth(): Boolean
    fun renderDirect(context: DeferredRenderContext)
    fun renderLayered(context: DeferredRenderContext, depth: Int, count: Int, base: Int, rect: IntArray, local: Boolean, handMask: Int)

    companion object {
        private val FULL_VIEW = IntArray(4)

        /** Reuse the completed first-person mask only in the post-hand composition path. */
        fun bindHandMask(shader: ShaderInstance, texture: Int) {
            shader.getUniform("SceneHandMaskEnabled")?.set(if (texture != 0) 1 else 0)
            shader.getUniform("SceneHandMask")?.set(4)
            if (texture != 0) {
                RenderSystem.activeTexture(GL13C.GL_TEXTURE4)
                GlStateManager._bindTexture(texture)
            }
        }
    }
}