package com.gtladd.gtladditions.api.client.scene

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.ShaderInstance
import org.joml.Matrix4fc

/** Called on the render thread; the scheduler catches no exception. */
fun interface PartRenderer<P : Any> {
    /** Adjacent parts of this renderer with equal batch keys, in scheduler order. */
    fun draw(context: PartDrawContext, parts: List<ScenePart<P>>)

    /** Once per frame and pass before it draws, with this renderer's visible parts; offscreen work only. */
    fun prepare(context: PartDrawContext, parts: List<ScenePart<P>>) {}
}

/** Valid during one callback. */
interface PartDrawContext {
    val frame: SceneFrame

    /** Camera-relative; push/pop must balance. */
    val poseStack: PoseStack

    /** Non-null only for [PartFeature.SCENE_DEPTH] parts in the TRANSLUCENT pass. */
    val sceneDepth: SceneDepth?

    /** Non-zero only for [PartFeature.OWN_HAND_MASK] parts under a shaderpack; red exceeds 0.5 on the first-person hand. */
    val handMaskTexture: Int

    /** Restores the target of this pass, its viewport and the baseline state. */
    fun bindTarget()

    /** Replaces the outcome the scheduler recorded for [part] in this frame. */
    fun report(part: ScenePart<*>, outcome: PartOutcome)
}

/** This frame's scene depth; `#moj_import <gtladditions:scene_depth.glsl>` declares what [bind] sets. */
interface SceneDepth {
    /** R32F window depth in [0, 1]; sample with texelFetch. */
    val texture: Int
    val width: Int
    val height: Int

    /** Scene pixel = gl_FragCoord.xy - origin. */
    val originX: Int
    val originY: Int

    /** Of [SceneFrame.projection]. */
    val inverseProjection: Matrix4fc

    /** Always false in v1. */
    val translucentTerrain: Boolean

    /** The depth may carry a shaderpack's jitter; true for every shaderpack frame. */
    val jittered: Boolean

    /** Changes whenever [texture] is reallocated. */
    val generation: Int

    fun bind(shader: ShaderInstance)
}
