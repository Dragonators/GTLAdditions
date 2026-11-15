package com.gtladd.gtladditions.client.render.scene

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.SceneDepth
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleCompositeRenderer
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderProfile
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderState
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderer
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleSceneEntry
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleShaders
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleTextures
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleVisualConfig
import com.gtladd.gtladditions.client.render.machine.heart.HeartHandDepthMask
import com.gtladd.gtladditions.client.render.machine.heart.HeartSceneCompositor
import com.gtladd.gtladditions.client.render.machine.heart.PendingHeartComposite
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
internal object SceneVolumes : PartRenderer<HeartBlackHoleSceneEntry> {
    private const val VIEW_DISTANCE = 384.0
    private val provider = SceneScheduler.register(ResourceLocation(GTLAdditions.MOD_ID, "heart"), null)
    private var mode = Mode.NONE
    private var pending: PendingHeartComposite? = null
    private var failureReported = false

    private enum class Mode { NONE, COMBINED, LAYERS }

    @JvmOverloads
    fun submit(frame: SceneFrame, blockEntity: BlockEntity, profile: HeartBlackHoleRenderProfile, limitViewDistance: Boolean = true) {
        val center = profile.center.add(Vec3.atLowerCornerOf(blockEntity.blockPos))
        val limit = VIEW_DISTANCE + HeartBlackHoleVisualConfig.CPU_CULLING_RADIUS
        if (limitViewDistance && center.distanceToSqr(frame.cameraX, frame.cameraY, frame.cameraZ) > limit * limit) return
        // The proxy sphere is the only geometry the volume pass rasterises.
        val radius = HeartBlackHoleVisualConfig.volumeProxyRadius(profile.tick).toDouble()
        frame.submit(
            ScenePart.builder(PartKey.block(provider, blockEntity.blockPos, 0), this, HeartBlackHoleSceneEntry(blockEntity, profile))
                .blend(PartBlend.PREMULTIPLIED)
                .bounds(PartBounds.sphere(center.x, center.y, center.z, radius))
                .features(PartFeature.SCENE_DEPTH, PartFeature.OWN_HAND_MASK)
                .build()
        )
    }

    override fun draw(context: PartDrawContext, parts: List<ScenePart<HeartBlackHoleSceneEntry>>) {
        when (mode) {
            Mode.COMBINED -> pending?.let { composite ->
                HeartBlackHoleCompositeRenderer.renderPendingComposite(
                    composite.width,
                    composite.height,
                    composite,
                    context.handMaskTexture
                )
                pending = null
            }
            Mode.LAYERS -> parts.forEach { HeartSceneCompositor.copyLayer(it.key.instance) }
            Mode.NONE -> Unit
        }
    }

    /** [hearts] are the first items of [plan]. */
    fun prepare(
        frame: SceneFrameState,
        poseStack: PoseStack,
        hearts: List<ScenePart<HeartBlackHoleSceneEntry>>,
        plan: SceneOrder.Plan,
        depth: SceneDepth,
        handMask: Int
    ) {
        if (hearts.isEmpty()) return
        if (HeartBlackHoleTextures.pending) return report(frame, hearts, PartOutcome.PENDING)
        // Without the completed mask a Heart would cover the first-person hand.
        if (frame.shaderpackActive && !HeartHandDepthMask.isReady()) return report(frame, hearts, PartOutcome.RESOURCE_UNAVAILABLE)
        val entries = hearts.map { it.payload }
        val combined = combined(plan, hearts.size)
        val camera = Vec3(frame.cameraX, frame.cameraY, frame.cameraZ)
        val ready = heart(frame, hearts, plan, 0) {
            if (combined) {
                pending = HeartBlackHoleRenderer.prepare(entries, poseStack, camera, depth)
                pending != null
            } else {
                HeartSceneCompositor.prepareLayers(entries, poseStack, camera, handMask, depth)
            }
        } ?: return

        if (!ready) return report(frame, hearts, PartOutcome.RESOURCE_UNAVAILABLE)
        mode = if (combined) Mode.COMBINED else Mode.LAYERS
        // Intersecting Hearts keep the centre-distance blend instead of depth bins.
        if (combined) {
            for (part in hearts) {
                if (frame.outcomes[part.key] == PartOutcome.DRAWN_LAYERED) frame.outcomes[part.key] = PartOutcome.DRAWN_UNORDERED
            }
        }
    }

    fun finish() {
        mode = Mode.NONE
        pending = null
    }

    private fun combined(plan: SceneOrder.Plan, count: Int): Boolean {
        val first = plan.steps.indexOfFirst { step -> step.items.any { it < count } }
        val last = plan.steps.indexOfLast { step -> step.items.any { it < count } }
        return (first..last).all { index -> plan.steps[index].items.all { it < count } }
    }

    private fun report(frame: SceneFrameState, hearts: List<ScenePart<HeartBlackHoleSceneEntry>>, outcome: PartOutcome) {
        for (part in hearts) frame.outcomes[part.key] = outcome
    }

    /** [parts] are the Hearts followed by the native parts. */
    fun drawLayered(
        frame: SceneFrameState,
        drawContext: SceneDrawContext,
        hearts: List<ScenePart<HeartBlackHoleSceneEntry>>,
        parts: List<ScenePart<*>>,
        plan: SceneOrder.Plan,
        index: Int
    ) {
        val items = plan.steps[index].items
        val stepHearts = items.filter { it < hearts.size }.map { hearts[it] }
        val members = items
            .filter { it >= hearts.size }
            .map { parts[it] }
            .sortedWith(compareBy({ it.key.instance }, { it.key.part }))
        drawContext.begin(stepHearts[0].features, null)
        // Every Layered step of a combined run holds only Hearts.
        if (mode == Mode.COMBINED) return draw(drawContext, stepHearts)
        if (mode == Mode.LAYERS) {
            val camera = Vec3(frame.cameraX, frame.cameraY, frame.cameraZ)
            val heartBounds = stepHearts.map { SceneOrder.volumes(it.bounds)[0].box }
            val boxes = members.flatMap { member -> SceneOrder.volumes(member.bounds).map { it.box } }
            val rect = HeartSceneCompositor.region(
                boxes,
                heartBounds,
                frame.modelView,
                frame.projection,
                camera,
                frame.viewportWidth,
                frame.viewportHeight
            )
            if (rect == null) {
                // No pixel is shared, so plain draws are exact.
                draw(drawContext, stepHearts)
                for (part in stepHearts) frame.outcomes[part.key] = PartOutcome.DRAWN
                for (member in members) drawAlone(frame, drawContext, member, true)
                return
            }

            if (HeartBlackHoleShaders.layerDepthShader != null && HeartBlackHoleShaders.layerResolveShader != null) {
                val bins = heart(frame, hearts, plan, index) {
                    val images = HeartSceneCompositor.layers(stepHearts.map { it.key.instance })
                    images to HeartSceneCompositor.ensureTargets(rect[2], rect[3], images)
                }

                if (bins != null) {
                    val (images, local) = bins
                    HeartBlackHoleRenderState.withSavedState {
                        HeartSceneCompositor.compose(drawContext.target, images, local, frame.projection, rect) { base, cropped ->
                            for (member in members) {
                                // The compositor owns the bins, viewport and scissor here.
                                drawContext.select(member.features)
                                @Suppress("UNCHECKED_CAST")
                                (member.renderer as LayeredPartRenderer<Any>).drawLayered(
                                    drawContext,
                                    member as ScenePart<Any>,
                                    local.depths,
                                    images.count,
                                    base,
                                    rect,
                                    cropped
                                )
                            }
                        }
                    }

                    return
                }
            } else {
                draw(drawContext, stepHearts)
                for (part in stepHearts) frame.outcomes[part.key] = PartOutcome.DRAWN_UNORDERED
            }
        }

        for (member in members) drawAlone(frame, drawContext, member, false)
    }

    /** [exact] when it shares no pixel with the Hearts, otherwise a reported approximation. */
    private fun drawAlone(frame: SceneFrameState, drawContext: SceneDrawContext, member: ScenePart<*>, exact: Boolean) {
        // Written first, so the renderer's own report overrides it.
        frame.outcomes[member.key] = if (exact) PartOutcome.DRAWN else PartOutcome.DRAWN_UNORDERED
        drawContext.begin(member)
        @Suppress("UNCHECKED_CAST")
        (member.renderer as PartRenderer<Any>).draw(drawContext, listOf(member as ScenePart<Any>))
    }

    private fun <T> heart(
        frame: SceneFrameState,
        hearts: List<ScenePart<HeartBlackHoleSceneEntry>>,
        plan: SceneOrder.Plan,
        from: Int,
        action: () -> T
    ): T? =
        try {
            HeartBlackHoleRenderState.withSavedState(action)
        } catch (error: Exception) {
            mode = Mode.NONE
            pending = null

            for (step in plan.steps.drop(from)) {
                for (item in step.items) if (item < hearts.size) frame.outcomes[hearts[item].key] = PartOutcome.FAILED
            }

            if (!failureReported) {
                failureReported = true
                GTLAdditions.LOGGER.error("Heart of the Universe rendering failed; skipping this frame's remaining Hearts", error)
            }

            null
        }
}

internal interface LayeredPartRenderer<P : Any> : PartRenderer<P> {
    fun layered(part: ScenePart<P>): Boolean

    /**
     * [local]: the cropped MRT pass over [rect], otherwise the direct pass outside it. The compositor owns the targets,
     * viewport and scissor, so [context] must not rebind them.
     */
    fun drawLayered(context: PartDrawContext, part: ScenePart<P>, depth: Int, count: Int, base: Int, rect: IntArray, local: Boolean)
}