package com.gtladd.gtladditions.client.render.scene

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartPass
import com.gtladd.gtladditions.api.client.scene.ProviderHandle
import com.gtladd.gtladditions.api.client.scene.SceneCapabilities
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneProvider
import com.gtladd.gtladditions.client.render.machine.heart.HeartBlackHoleSceneEntry
import com.gtladd.gtladditions.client.render.machine.heart.HeartHandDepthMask
import com.gtladd.gtladditions.client.render.machine.heart.HeartSceneCompositor
import com.gtladd.gtladditions.events.ClientCloudHighlighter
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexSorting
import net.irisshaders.iris.Iris
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.shadows.ShadowRenderingState
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryStack

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
internal object SceneScheduler {
    private val debug = java.lang.Boolean.getBoolean("gtladditions.scene.debug")
    private val providers = ArrayList<ProviderHandle>()
    private val reported = HashSet<Pair<ProviderHandle, PartOutcome>>()
    private val keyOrder = compareBy<PartKey>({ it.provider.id }, { it.instance }, { it.part })
    var frameId = 0L
        private set
    private var frame: SceneFrameState? = null
    private var outcomes: Map<PartKey, PartOutcome> = emptyMap()
    private var resourceGeneration = 0
    private var pipelineGeneration = 0
    private var resourcesChanged = false
    private var pipelineChanged = false
    private var levelChanged = false

    val capabilities: SceneCapabilities by lazy {
        object : SceneCapabilities {
            override val layeredVolumes = false
            override val handStencil = true
            override val glVersion = GL11C.glGetInteger(GL30C.GL_MAJOR_VERSION) * 10 + GL11C.glGetInteger(GL30C.GL_MINOR_VERSION)
            override val computeShaders = GL.getCapabilities().OpenGL43
        }
    }

    fun register(id: ResourceLocation, provider: SceneProvider?) = ProviderHandle(id, provider).also(providers::add)

    fun openFrame(): SceneFrame? = frame?.takeIf { it.accepting && !ShadowRenderingState.areShadowsCurrentlyBeingRendered() }

    fun lastOutcome(key: PartKey): PartOutcome? = outcomes[key]

    fun beginTick() {
        frame?.let(::finish)
        frame = null
        HeartHandDepthMask.beginFrame()
        HeartSceneCompositor.beginFrame()
        SceneTargets.beginTick()
        if (resourcesChanged) {
            resourcesChanged = false
            providers.forEach { it.provider?.onResourcesReloaded(resourceGeneration) }
        }

        if (pipelineChanged) {
            pipelineChanged = false
            providers.forEach { it.provider?.onPipelineChanged(pipelineGeneration) }
        }

        if (levelChanged) {
            levelChanged = false
            providers.forEach { it.provider?.onLevelChanged() }
        }
    }

    fun beginFrame(
        level: ClientLevel,
        camera: Vec3,
        modelView: Matrix4f,
        projection: Matrix4f,
        partialTick: Float,
        renderTicks: Double,
        shaderpackActive: Boolean,
        fabulous: Boolean,
        width: Int,
        height: Int
    ) {
        frame = SceneFrameState(
            ++frameId,
            level,
            camera,
            Matrix4f(modelView),
            Matrix4f(projection),
            width,
            height,
            partialTick,
            renderTicks,
            shaderpackActive,
            fabulous,
            resourceGeneration,
            pipelineGeneration
        )
    }

    fun closeSubmissions() {
        val frame = frame ?: return
        providers.forEach { it.provider?.collect(frame) }
        frame.accepting = false
        frame.visible = frame.parts.values.filter { part ->
            SceneOrder.volumes(part.bounds)
                .any { frame.drawingFrustum.isVisible(it.box) }
                .also { if (!it) frame.outcomes[part.key] = PartOutcome.CULLED }
        }.sortedWith(compareBy(keyOrder) { it.key })

        // OPAQUE parts draw before any snapshot and need none.
        frame.needsDepth = frame.visible.any { it.pass == PartPass.TRANSLUCENT }
        // The composition and the highlight test the hand stencil; OWN_HAND_MASK parts also discard in the OPAQUE pass.
        frame.needsHandMask = frame.shaderpackActive &&
            (
                frame.needsDepth ||
                    frame.visible.any { PartFeature.OWN_HAND_MASK in it.features } ||
                    ClientCloudHighlighter.hasEntries(frame.level)
                )
    }

    /** Decided when submissions close, before the shaderpack draws the hand. */
    fun needsHandMask(): Boolean = frame?.needsHandMask == true

    /** Vanilla Fast/Fancy `D_o`: the main depth right before chunk translucency, after the late opaque batch flushes. */
    fun beforeTranslucentTerrain() {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) return
        val frame = frame ?: return
        if (!frame.needsDepth || frame.shaderpackActive || frame.fabulous) return
        val main = Minecraft.getInstance().mainRenderTarget
        SceneTargets.ensure(frame.viewportWidth, frame.viewportHeight, main.depthTextureId, frame.shaderpackActive)
        SceneTargets.copyDepth(main.frameBufferId, SceneTargets.opaqueDepth)
    }

    /** Right before rain and snow, so falling rain stays in front. */
    fun composeBeforeWeather() {
        val frame = frame ?: return
        if (frame.shaderpackActive || frame.visible.isEmpty()) return
        withFrameMatrices(frame) { pose -> compose(frame, pose) }
    }

    fun resourcesReloaded() {
        resourceGeneration++
        resourcesChanged = true
    }

    fun pipelineChanged() {
        pipelineGeneration++
        pipelineChanged = true
    }

    private fun finish(frame: SceneFrameState) {
        frame.accepting = false
        for (key in frame.parts.keys) frame.outcomes.putIfAbsent(key, PartOutcome.DROPPED)
        outcomes = frame.outcomes
        if (debug) {
            for ((key, outcome) in outcomes) {
                if (reported.add(key.provider to outcome)) {
                    GTLAdditions.LOGGER.info("Scene provider {} first reported {} in frame {}", key.provider, outcome, frame.id)
                }
            }
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderTick(event: TickEvent.RenderTickEvent) {
        if (event.phase == TickEvent.Phase.START) beginTick()
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_SKY) return
        val client = Minecraft.getInstance()
        val level = client.level ?: return
        beginFrame(
            level,
            event.camera.position,
            event.poseStack.last().pose(),
            event.projectionMatrix,
            event.partialTick,
            RenderUtils.getSmoothTickDouble(),
            IrisApi.getInstance().isShaderPackInUse,
            // The Fabulous transparency chain exists only when it actually loaded.
            client.levelRenderer.translucentTarget != null,
            client.mainRenderTarget.viewWidth,
            client.mainRenderTarget.viewHeight
        )
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @JvmStatic
    fun onOpaqueStageEnd(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return
        closeSubmissions()
        val frame = frame ?: return
        // Solid surfaces go before chunk translucency, so glass and water blend over them.
        if (frame.shaderpackActive) return
        val opaque = opaqueParts(frame)
        if (opaque.isNotEmpty()) {
            withFrameMatrices(frame) { pose ->
                withSavedTargetState { SceneComposition.renderOpaque(frame, pose, opaque) }
            }
        }
    }

    /**
     * With a shaderpack the composition follows its final pass, where the hand mask is complete; the direct main-target pass
     * first resets the per-buffer blend state shaderpack programs leave behind.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    @JvmStatic
    fun onLevelEnd(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_LEVEL) return
        val frame = frame ?: return
        // Every frame, so expired entries go even while the composition has work.
        val highlights = ClientCloudHighlighter.hasEntries(frame.level)
        val composes = frame.shaderpackActive && frame.visible.isNotEmpty()
        if (!composes && !highlights) return
        val camera = Vec3(frame.cameraX, frame.cameraY, frame.cameraZ)
        val main = Minecraft.getInstance().mainRenderTarget
        withFrameMatrices(frame) { pose ->
            if (!frame.shaderpackActive) {
                main.bindWrite(true)
                return@withFrameMatrices ClientCloudHighlighter.render(pose, frame.level, camera)
            }

            OculusCompat.withDirectMainTargetPass {
                if (composes) {
                    val opaque = opaqueParts(frame)
                    if (opaque.isNotEmpty()) withSavedTargetState { SceneComposition.renderOpaque(frame, pose, opaque) }
                }

                if (frame.needsDepth || highlights) {
                    withStencilScope {
                        val composed = frame.needsDepth && compose(frame, pose)
                        if (highlights) {
                            if (!composed) writeHandStencil(frame)
                            if (frame.handStencil) SceneTargets.bindTarget(main.colorTextureId) else main.bindWrite(true)
                            SceneTargets.testHandStencil(frame.handStencil)
                            ClientCloudHighlighter.render(pose, frame.level, camera)
                        }
                    }
                }
            }
        }
    }

    /** `D_t`: the depth right after chunk translucency, before any listener draws machine translucency on top. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @JvmStatic
    fun onTranslucentTerrainEnd(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS ||
            ShadowRenderingState.areShadowsCurrentlyBeingRendered()
        ) {
            return
        }

        val frame = frame ?: return
        if (!frame.needsDepth || frame.fabulous) return
        val main = Minecraft.getInstance().mainRenderTarget
        SceneTargets.ensure(frame.viewportWidth, frame.viewportHeight, main.depthTextureId, frame.shaderpackActive)
        SceneTargets.copyDepth(main.frameBufferId, SceneTargets.translucentDepth)
    }

    @SubscribeEvent
    @JvmStatic
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level.isClientSide) levelChanged = true
    }

    /** Submission order: a blended surface that writes depth depends on what is already there. */
    private fun opaqueParts(frame: SceneFrameState) =
        frame.parts.values.filter { it.pass == PartPass.OPAQUE && frame.outcomes[it.key] != PartOutcome.CULLED }

    private fun compose(frame: SceneFrameState, pose: PoseStack): Boolean {
        @Suppress("UNCHECKED_CAST")
        val hearts = frame.visible.filter { it.renderer === SceneVolumes } as List<ScenePart<HeartBlackHoleSceneEntry>>
        // OPAQUE parts already drew into main.
        val natives = frame.visible.filter { it.renderer !== SceneVolumes && it.pass == PartPass.TRANSLUCENT }
        if (hearts.isEmpty() && natives.isEmpty()) return false
        val shader = SceneShaders.compose ?: run {
            (hearts + natives).forEach { frame.outcomes[it.key] = PartOutcome.RESOURCE_UNAVAILABLE }
            return false
        }

        val main = Minecraft.getInstance().mainRenderTarget
        SceneTargets.ensure(frame.viewportWidth, frame.viewportHeight, main.depthTextureId, frame.shaderpackActive)
        // With a shaderpack D_o is depthtex1, which Iris copies before translucents, after the solid hand.
        val opaque = if (frame.shaderpackActive) {
            (Iris.getPipelineManager().pipelineNullable as IrisPipelineBridge).opaqueDepthTexture
        } else {
            SceneTargets.opaqueDepth
        }

        val depth = SceneTargets.compose(
            shader,
            main.depthTextureId,
            opaque,
            SceneTargets.translucentDepth,
            !frame.fabulous,
            frame.projection,
            frame.shaderpackActive
        )
        // The stencil test is off until here, so the composed depth covers every pixel.
        if (frame.shaderpackActive) writeHandStencil(frame)
        SceneComposition.render(frame, pose, hearts, natives, depth)
        return true
    }

    /** Mask 0: no hand drew this frame, so the stencil is cleared without the program. */
    private fun writeHandStencil(frame: SceneFrameState) {
        if (!HeartHandDepthMask.isReady()) return
        val mask = HeartHandDepthMask.textureId()
        val program = SceneShaders.handStencil
        if (mask != 0 && program == null) return
        SceneTargets.ensure(frame.viewportWidth, frame.viewportHeight, Minecraft.getInstance().mainRenderTarget.depthTextureId, true)
        SceneTargets.writeHandStencil(mask, program)
        frame.handStencil = true
    }

    private inline fun withStencilScope(action: () -> Unit) {
        val state = StencilState()
        GL11C.glDisable(GL11C.GL_STENCIL_TEST)
        try {
            action()
        } finally {
            state.restore()
            Minecraft.getInstance().mainRenderTarget.bindWrite(true)
        }
    }

    private inline fun withSavedTargetState(action: () -> Unit) {
        MemoryStack.stackPush().use { stack ->
            val viewport = stack.mallocInt(4)
            val colorMask = stack.malloc(4)
            GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport)
            GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, colorMask)
            val read = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING)
            val draw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING)
            try {
                RenderUtils.withSavedRenderState(action)
            } finally {
                GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, read)
                GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, draw)
                RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
                RenderSystem.colorMask(
                    colorMask[0] != 0.toByte(),
                    colorMask[1] != 0.toByte(),
                    colorMask[2] != 0.toByte(),
                    colorMask[3] != 0.toByte()
                )
            }
        }
    }

    /** Before weather the model-view already holds the camera rotation; participants draw with the frame's own matrices. */
    private inline fun withFrameMatrices(frame: SceneFrameState, action: (PoseStack) -> Unit) {
        val projection = Matrix4f(RenderSystem.getProjectionMatrix())
        val sorting = RenderSystem.getVertexSorting()
        val modelView = RenderSystem.getModelViewStack()
        modelView.pushPose()
        modelView.setIdentity()
        RenderSystem.applyModelViewMatrix()
        RenderSystem.setProjectionMatrix(frame.projection, VertexSorting.DISTANCE_TO_ORIGIN)
        try {
            action(PoseStack().apply { mulPoseMatrix(frame.modelView) })
        } finally {
            RenderSystem.setProjectionMatrix(projection, sorting)
            modelView.popPose()
            RenderSystem.applyModelViewMatrix()
        }
    }
}

private class StencilState {
    private val enabled = GL11C.glIsEnabled(GL11C.GL_STENCIL_TEST)
    private val front = query(
        GL11C.GL_STENCIL_FUNC,
        GL11C.GL_STENCIL_REF,
        GL11C.GL_STENCIL_VALUE_MASK,
        GL11C.GL_STENCIL_FAIL,
        GL11C.GL_STENCIL_PASS_DEPTH_FAIL,
        GL11C.GL_STENCIL_PASS_DEPTH_PASS,
        GL11C.GL_STENCIL_WRITEMASK
    )
    private val back = query(
        GL20C.GL_STENCIL_BACK_FUNC,
        GL20C.GL_STENCIL_BACK_REF,
        GL20C.GL_STENCIL_BACK_VALUE_MASK,
        GL20C.GL_STENCIL_BACK_FAIL,
        GL20C.GL_STENCIL_BACK_PASS_DEPTH_FAIL,
        GL20C.GL_STENCIL_BACK_PASS_DEPTH_PASS,
        GL20C.GL_STENCIL_BACK_WRITEMASK
    )
    private val clear = GL11C.glGetInteger(GL11C.GL_STENCIL_CLEAR_VALUE)

    /** RenderSystem sets the front values on both faces and keeps its cache in step; the back face follows directly. */
    fun restore() {
        RenderSystem.stencilFunc(front[0], front[1], front[2])
        RenderSystem.stencilOp(front[3], front[4], front[5])
        RenderSystem.stencilMask(front[6])
        GL20C.glStencilFuncSeparate(GL11C.GL_BACK, back[0], back[1], back[2])
        GL20C.glStencilOpSeparate(GL11C.GL_BACK, back[3], back[4], back[5])
        GL20C.glStencilMaskSeparate(GL11C.GL_BACK, back[6])
        RenderSystem.clearStencil(clear)
        if (enabled) GL11C.glEnable(GL11C.GL_STENCIL_TEST) else GL11C.glDisable(GL11C.GL_STENCIL_TEST)
    }

    private fun query(vararg names: Int) = IntArray(names.size) { GL11C.glGetInteger(names[it]) }
}

@Suppress("unused")
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
internal object SceneSchedulerReloadEvents {
    @SubscribeEvent
    @JvmStatic
    fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(ResourceManagerReloadListener { SceneScheduler.resourcesReloaded() })
    }
}