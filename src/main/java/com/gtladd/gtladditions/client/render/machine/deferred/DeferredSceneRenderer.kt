package com.gtladd.gtladditions.client.render.machine.deferred

import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderEntry
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderProfile
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderer
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleSceneEntry
import com.gtladd.gtladditions.client.render.machine.heart.HeartHandDepthMask
import com.gtladd.gtladditions.client.render.machine.heart.HeartSceneCompositor
import com.gtladd.gtladditions.client.render.machine.heart.HeartSceneEffect
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexSorting
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.irisshaders.iris.shadows.ShadowRenderingState
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f

/**
 * Coordinates scene effects that must see each other: black holes and overlapping transparent machine effects.
 * [DeferredSceneEvents] supplies the frame boundaries; machine renderers only submit their entries.
 */
@OnlyIn(Dist.CLIENT)
object DeferredSceneRenderer {
    private val entries = Long2ObjectLinkedOpenHashMap<HeartBlackHoleSceneEntry>()
    private var prepared: Frame? = null
    internal var frameId = 0L
        private set

    fun enqueueBlackHole(blockEntity: BlockEntity, profile: HeartBlackHoleRenderProfile) {
        if (!ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            entries.put(blockEntity.blockPos.asLong(), HeartBlackHoleSceneEntry(blockEntity, profile))
        }
    }

    fun beginFrame() {
        frameId++
        entries.clear()
        prepared = null
        HeartHandDepthMask.beginFrame()
        HeartSceneCompositor.beginFrame()
    }

    internal fun tryDeferEffect(entry: HeartSceneEffect): Boolean {
        val frame = prepared ?: return false
        if (entry.blockEntity.level !== frame.level || !entry.supportsOrderedRendering) return false

        val target = Minecraft.getInstance().mainRenderTarget
        val rect = HeartSceneCompositor.overlap(
            listOf(entry),
            frame.entries,
            frame.pose,
            frame.projection,
            frame.camera,
            target.viewWidth,
            target.viewHeight
        ) ?: return false

        // Admission computes the exact order used later by the same frame's compositor.
        frame.effects.add(entry)
        if (frame.effects.any { !it.supportsLayeredRendering }) {
            val order = HeartSceneCompositor.order(
                frame.entries,
                frame.effects,
                frame.pose,
                frame.projection,
                frame.camera,
                target.viewWidth,
                target.viewHeight
            )
            if (order == null) {
                frame.effects.removeAt(frame.effects.lastIndex)
                return false
            }
            frame.order = order
        }

        val previous = frame.overlap
        if (previous == null) {
            frame.overlap = rect
        } else {
            val x0 = minOf(previous[0], rect[0])
            val y0 = minOf(previous[1], rect[1])
            val x1 = maxOf(previous[0] + previous[2], rect[0] + rect[2])
            val y1 = maxOf(previous[1] + previous[3], rect[1] + rect[3])
            previous[0] = x0
            previous[1] = y0
            previous[2] = x1 - x0
            previous[3] = y1 - y0
        }

        return true
    }

    internal fun prepareBlackHoles(event: RenderLevelStageEvent) {
        prepared = null
        val client = Minecraft.getInstance()
        val level = client.level ?: return
        val context = DeferredRenderContext(event.camera.position, event.poseStack, event.frustum)
        try {
            val visible = entries.values.filter { it.blockEntity.level === level && it.isVisible(context) }
            if (visible.isEmpty()) return
            prepared = Frame(
                visible,
                level,
                context.cameraPosition,
                Matrix4f(event.poseStack.last().pose()),
                Matrix4f(event.projectionMatrix),
                DeferredOculusCompat.shouldRenderAfterShaderpackFinal()
            )
        } finally {
            entries.clear()
        }
    }

    internal fun renderPreparedScene() {
        val frame = prepared ?: return
        prepared = null
        if (Minecraft.getInstance().level !== frame.level) return

        val previousProjection = Matrix4f(RenderSystem.getProjectionMatrix())
        val previousSorting = RenderSystem.getVertexSorting()
        try {
            RenderSystem.setProjectionMatrix(frame.projection, VertexSorting.DISTANCE_TO_ORIGIN)
            val pose = PoseStack()
            pose.mulPoseMatrix(frame.pose)
            renderScene(frame, DeferredRenderContext(frame.camera, pose, null))
        } finally {
            RenderSystem.setProjectionMatrix(previousProjection, previousSorting)
        }
    }

    private fun renderScene(frame: Frame, context: DeferredRenderContext) {
        // An unavailable hand mask suppresses black holes, but must not suppress deferred VFX.
        if (frame.shaderpack && !HeartHandDepthMask.isReady()) {
            frame.effects.forEach { it.renderDirect(context) }
            return
        }
        val handMask = if (frame.shaderpack) HeartHandDepthMask.textureId() else 0
        if (HeartSceneCompositor.render(
                context,
                frame.entries,
                frame.effects,
                frame.projection,
                handMask,
                frame.overlap,
                frame.order
            )
        ) {
            return
        }

        // No overlap or unavailable compositor: retain the independent draw path and its ordering.
        frame.effects.forEach { it.renderDirect(context) }
        HeartBlackHoleRenderer.render(frame.entries, context.poseStack, frame.camera, handMask)
    }

    @JvmStatic
    fun needsHandMask(): Boolean = prepared?.shaderpack == true

    private data class Frame(
        val entries: List<HeartBlackHoleRenderEntry>,
        val level: ClientLevel,
        val camera: Vec3,
        val pose: Matrix4f,
        val projection: Matrix4f,
        val shaderpack: Boolean,
        val effects: MutableList<HeartSceneEffect> = ArrayList(),
        var overlap: IntArray? = null,
        var order: IntArray? = null
    )
}