package com.gtladd.gtladditions.client.render.scene

import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.SceneDepth
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleRenderState
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleSceneEntry
import com.gtladd.gtladditions.client.render.machine.heart.HeartHandDepthMask
import com.gtladd.gtladditions.client.render.machine.heart.HeartLayerImages
import com.gtladd.gtladditions.mixin.minecraft.client.BlendModeAccessor
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL14C
import java.util.EnumSet

@OnlyIn(Dist.CLIENT)
internal object SceneComposition {
    fun render(
        frame: SceneFrameState,
        poseStack: PoseStack,
        hearts: List<ScenePart<HeartBlackHoleSceneEntry>>,
        natives: List<ScenePart<*>>,
        depth: SceneDepth
    ) {
        // Every index here relies on the Hearts coming first.
        val parts = hearts + natives
        val items = parts.map {
            SceneOrder.Item(
                SceneOrder.volumes(it.bounds),
                it.blend,
                it.renderer,
                it.batchKey,
                composite = composite(it),
                volumeSource = it.renderer === SceneVolumes,
                layered = layered(it)
            )
        }

        val plan = SceneOrder.order(
            items,
            frame.modelView,
            frame.projection,
            Vec3(frame.cameraX, frame.cameraY, frame.cameraZ),
            frame.viewportWidth,
            frame.viewportHeight
        )
        plan.outcomes.forEachIndexed { index, outcome -> frame.outcomes[parts[index].key] = outcome }

        val handMask = if (frame.shaderpackActive) HeartHandDepthMask.textureId() else 0
        val target = SceneTargets.composition(Minecraft.getInstance().mainRenderTarget.colorTextureId)
        val drawContext = SceneDrawContext(frame, poseStack, depth, handMask, target)
        HeartBlackHoleRenderState.withSavedState {
            HeartLayerImages.withArrayState {
                SceneVolumes.prepare(frame, poseStack, hearts, plan, depth, handMask)
                prepare(drawContext, natives)

                for ((index, step) in plan.steps.withIndex()) {
                    val first = step.items[0]
                    when {
                        step is SceneOrder.Layered -> SceneVolumes.drawLayered(frame, drawContext, hearts, parts, plan, index)
                        first < hearts.size -> {
                            drawContext.begin(hearts[first].features, null)
                            SceneVolumes.draw(drawContext, step.items.map { hearts[it] })
                        }
                        else -> {
                            val batch = step.items.map { parts[it] }
                            drawContext.begin(batch[0])
                            @Suppress("UNCHECKED_CAST")
                            (batch[0].renderer as PartRenderer<Any>).draw(drawContext, batch as List<ScenePart<Any>>)
                        }
                    }
                }
            }
        }

        SceneVolumes.finish()
    }

    @Suppress("UNCHECKED_CAST")
    private fun layered(part: ScenePart<*>): Boolean = (part.renderer as? LayeredPartRenderer<Any>)?.layered(part as ScenePart<Any>) == true

    @Suppress("UNCHECKED_CAST")
    fun composite(part: ScenePart<*>): Boolean = (part.renderer as? CompositePartRenderer<Any>)?.composite(part as ScenePart<Any>) == true

    private fun prepare(context: SceneDrawContext, natives: List<ScenePart<*>>) {
        for (renderer in renderers(natives)) {
            val own = natives.filter { it.renderer === renderer }
            context.begin(own.flatMapTo(EnumSet.noneOf(PartFeature::class.java)) { it.features }, null)
            @Suppress("UNCHECKED_CAST")
            (renderer as PartRenderer<Any>).prepare(context, own as List<ScenePart<Any>>)
        }
    }

    fun renderOpaque(frame: SceneFrameState, poseStack: PoseStack, parts: List<ScenePart<*>>) {
        val context = OpaqueDrawContext(frame, poseStack, if (frame.shaderpackActive) HeartHandDepthMask.textureId() else 0)

        for (renderer in renderers(parts)) {
            val own = parts.filter { it.renderer === renderer }
            context.select(own.flatMapTo(EnumSet.noneOf(PartFeature::class.java)) { it.features })
            context.bindTarget()
            @Suppress("UNCHECKED_CAST")
            (renderer as PartRenderer<Any>).prepare(context, own as List<ScenePart<Any>>)
        }

        for (part in parts) {
            // Written first, so the renderer's own report overrides it.
            frame.outcomes[part.key] = PartOutcome.DRAWN
            context.select(part.features)
            context.bindTarget()
            @Suppress("UNCHECKED_CAST")
            (part.renderer as PartRenderer<Any>).draw(context, listOf(part as ScenePart<Any>))
        }
    }

    private fun renderers(parts: List<ScenePart<*>>): List<PartRenderer<*>> {
        val renderers = ArrayList<PartRenderer<*>>()
        for (part in parts) if (renderers.none { it === part.renderer }) renderers.add(part.renderer)
        return renderers
    }
}

internal class SceneDrawContext(
    override val frame: SceneFrameState,
    override val poseStack: PoseStack,
    private val depth: SceneDepth,
    private val handMask: Int,
    val target: RenderTarget
) : PartDrawContext {
    private var features: Set<PartFeature> = emptySet()
    private var blend: PartBlend? = null

    override val sceneDepth: SceneDepth?
        get() = depth.takeIf { PartFeature.SCENE_DEPTH in features }

    override val handMaskTexture: Int
        get() = if (PartFeature.OWN_HAND_MASK in features) handMask else 0

    /** [blend] is a native batch's declaration, null where the step sets its own. */
    fun begin(features: Set<PartFeature>, blend: PartBlend?) {
        select(features)
        this.blend = blend
        bindTarget()
    }

    fun begin(part: ScenePart<*>) = begin(part.features, part.blend.takeUnless { SceneComposition.composite(part) })

    /** Touches no GL state, for callbacks inside a compositor's own targets. */
    fun select(features: Set<PartFeature>) {
        this.features = features
        blend = null
    }

    override fun bindTarget() {
        target.bindWrite(true)
        HeartBlackHoleRenderState.disableScissor()
        if (frame.shaderpackActive) SceneTargets.testHandStencil(frame.handStencil)
        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11C.GL_LEQUAL)
        RenderSystem.depthMask(false)
        RenderSystem.enableCull()
        RenderSystem.colorMask(true, true, true, false)
        GL11C.glFrontFace(GL11C.GL_CCW)
        GlStateManager._glUseProgram(0)
        VertexBuffer.unbind()
        RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
        BlendModeAccessor.setLastApplied(null)
        when (blend) {
            null -> Unit
            PartBlend.OPAQUE -> RenderSystem.disableBlend()
            PartBlend.PREMULTIPLIED -> enableBlend(GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA)
            PartBlend.STRAIGHT -> enableBlend(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA)
            PartBlend.ADDITIVE -> enableBlend(GL11C.GL_ONE, GL11C.GL_ONE, GL11C.GL_ONE, GL11C.GL_ONE)
            PartBlend.SCREEN -> enableBlend(GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_COLOR, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_COLOR)
        }
    }

    override fun report(part: ScenePart<*>, outcome: PartOutcome) {
        frame.outcomes[part.key] = outcome
    }

    private fun enableBlend(sourceRgb: Int, destinationRgb: Int, sourceAlpha: Int, destinationAlpha: Int) {
        RenderSystem.enableBlend()
        RenderSystem.blendEquation(GL14C.GL_FUNC_ADD)
        RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha)
    }
}

/** Under a shaderpack it draws after the hand, so [handMask] is served to OWN_HAND_MASK parts as in the composition. */
internal class OpaqueDrawContext(
    override val frame: SceneFrameState,
    override val poseStack: PoseStack,
    private val handMask: Int
) : PartDrawContext {
    private var features: Set<PartFeature> = emptySet()
    override val sceneDepth: SceneDepth? = null

    override val handMaskTexture: Int
        get() = if (PartFeature.OWN_HAND_MASK in features) handMask else 0

    fun select(features: Set<PartFeature>) {
        this.features = features
    }

    override fun bindTarget() {
        Minecraft.getInstance().mainRenderTarget.bindWrite(true)
        HeartBlackHoleRenderState.disableScissor()
        RenderSystem.enableDepthTest()
        RenderSystem.depthFunc(GL11C.GL_LEQUAL)
        RenderSystem.depthMask(true)
        RenderSystem.enableCull()
        RenderSystem.colorMask(true, true, true, true)
        GL11C.glFrontFace(GL11C.GL_CCW)
        RenderSystem.disableBlend()
        GlStateManager._glUseProgram(0)
        VertexBuffer.unbind()
        RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
        BlendModeAccessor.setLastApplied(null)
    }

    override fun report(part: ScenePart<*>, outcome: PartOutcome) {
        frame.outcomes[part.key] = outcome
    }
}

/** A composite part never commutes, and its plain draws get the step baseline without the declared blend. */
internal interface CompositePartRenderer<P : Any> : PartRenderer<P> {
    fun composite(part: ScenePart<P>): Boolean
}
