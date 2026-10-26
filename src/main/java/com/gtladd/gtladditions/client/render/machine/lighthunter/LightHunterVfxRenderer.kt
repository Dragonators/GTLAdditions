package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredSceneDepth
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredSceneRenderer
import com.gtladd.gtladditions.client.render.machine.heart.HeartLayerImages
import com.gtladd.gtladditions.client.render.machine.heart.HeartSceneCompositor
import com.gtladd.gtladditions.client.render.machine.heart.HeartSceneEffect
import com.gtladd.gtladditions.common.machine.multiblock.controller.LightHunterSpaceStation
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL12C
import org.lwjgl.opengl.GL13C
import org.lwjgl.opengl.GL14C
import org.lwjgl.opengl.GL30C
import org.lwjgl.opengl.GL33C
import org.lwjgl.system.MemoryUtil
import kotlin.math.floor

@OnlyIn(Dist.CLIENT)
object LightHunterVfxRenderer {
    // Shader-only material selector; the body reuses the existing SHIELDS mesh.
    private const val SHIELD_FIELD_EFFECT = 7
    private val meshes = Meshes()
    private var smoothFrameTick: Float? = null

    internal val supportsLayeredRendering: Boolean
        get() = LightHunterShaders.layers != null

    fun sampleAnimationTick(smoothAnimations: Boolean, sample: () -> Float): Float {
        if (!smoothAnimations) return sample()
        return smoothFrameTick ?: sample().also { smoothFrameTick = it }
    }

    fun beginFrame() {
        smoothFrameTick = null
        meshes.beginFrame()
        LightHunterFusionRenderer.meshes.beginFrame()
        LightHunterParadoxRenderer.meshes.beginFrame()
    }

    fun enqueue(blockEntity: BlockEntity, machine: LightHunterSpaceStation, tick: Float) {
        DeferredMachineRenderer.enqueue(
            Entry(
                blockEntity,
                machine,
                tick,
                if (machine.unlockParadoxical()) LightHunterParadoxAnimation.seconds(machine) else null
            )
        )
    }

    internal fun setResources(resources: ResourceProvider) {
        meshes.setResources(resources)
        LightHunterFusionRenderer.meshes.setResources(resources)
        LightHunterParadoxRenderer.meshes.setResources(resources)
    }

    fun releaseBuffers() {
        val release = {
            smoothFrameTick = null
            meshes.close()
            LightHunterFusionRenderer.meshes.close()
            LightHunterParadoxRenderer.meshes.close()
            LightHunterInteriorRenderer.close()
            HeartSceneCompositor.close()
        }
        if (RenderSystem.isOnRenderThread()) release() else RenderSystem.recordRenderCall { release() }
    }

    internal data class Entry(
        override val blockEntity: BlockEntity,
        val machine: LightHunterSpaceStation,
        val tick: Float,
        val paradoxSeconds: Float? = null
    ) : DeferredRenderEntry,
        HeartSceneEffect {
        override val bounds
            get() = LightHunterVfxGeometry.bounds(machine.frontFacing).move(blockEntity.blockPos)
        override val supportsLayeredRendering: Boolean
            get() = LightHunterVfxRenderer.supportsLayeredRendering

        override val depthCaptureKey: Any
            get() = LightHunterVfxRenderer

        override fun captureSceneDepth(): Boolean = captureLayeredSceneDepth()

        private var opaqueWindowPrepared = false

        override fun isVisible(context: DeferredRenderContext): Boolean =
            !blockEntity.isRemoved &&
                (
                    (machine.recipeLogic.isWorking && context.frustum?.isVisible(LightHunterVfxGeometry.bounds(machine.frontFacing).move(blockEntity.blockPos)) != false) ||
                        (machine.isFormed && context.frustum?.isVisible(LightHunterInteriorGeometry.bounds(machine.frontFacing).move(blockEntity.blockPos)) != false)
                    )

        override fun render(context: DeferredRenderContext) {
            // DeferredMachineRenderer has one entry per controller position; submit both effects here.
            LightHunterInteriorRenderer.render(context, blockEntity, machine, tick)
            if (machine.recipeLogic.isWorking && !DeferredSceneRenderer.tryDeferEffect(this)) renderDirect(context)
        }

        override fun renderOpaque(context: DeferredRenderContext) {
            val seconds = paradoxSeconds ?: return
            if (!machine.recipeLogic.isWorking) return
            val shader = LightHunterShaders.paradox ?: return
            val model = LightHunterShaders.paradoxModel ?: return
            context.renderRelativeToCamera(blockEntity) {
                val modelView = Matrix4f(last().pose()).mul(LightHunterVfxGeometry.transform(machine.frontFacing))
                LightHunterParadoxRenderer.drawOpaque(
                    LightHunterParadoxRenderer.meshes,
                    shader,
                    model,
                    modelView,
                    RenderSystem.getProjectionMatrix(),
                    seconds,
                    tick,
                    blockEntity
                )
            }
            opaqueWindowPrepared = true
        }

        override fun renderDirect(context: DeferredRenderContext) {
            val activeShader = LightHunterShaders.material ?: return
            val activeFieldShader = LightHunterShaders.chartField ?: return
            context.renderRelativeToCamera(blockEntity) {
                val modelView = Matrix4f(last().pose()).mul(LightHunterVfxGeometry.transform(machine.frontFacing))
                draw(meshes, activeShader, activeFieldShader, modelView, RenderSystem.getProjectionMatrix(), tick, paradoxSeconds = paradoxSeconds ?: -1f)
                LightHunterShaders.fusion?.let { fusion ->
                    // The exterior volume pass has just captured this frame's unchanged opaque depth.
                    paradoxSeconds?.let { seconds -> LightHunterFusionRenderer.draw(LightHunterFusionRenderer.meshes, meshes, fusion, modelView, RenderSystem.getProjectionMatrix(), seconds, tick, false) }
                }
                val paradox = LightHunterShaders.paradox
                val model = LightHunterShaders.paradoxModel
                if (paradoxSeconds != null && paradox != null && model != null) {
                    LightHunterParadoxRenderer.draw(
                        LightHunterParadoxRenderer.meshes, paradox, model, modelView,
                        RenderSystem.getProjectionMatrix(), paradoxSeconds, tick, blockEntity, false, true,
                        if (opaqueWindowPrepared) LightHunterParadoxRenderer.WINDOW_TRANSLUCENT else LightHunterParadoxRenderer.WINDOW_COMPLETE
                    )
                }
            }
        }

        override fun renderLayered(context: DeferredRenderContext, depth: Int, count: Int, base: Int, rect: IntArray, local: Boolean, handMask: Int) {
            val active = LightHunterShaders.layers ?: return
            HeartSceneEffect.bindHandMask(active, handMask)
            context.renderRelativeToCamera(blockEntity) {
                val modelView = Matrix4f(last().pose()).mul(LightHunterVfxGeometry.transform(machine.frontFacing))
                drawLayered(meshes, active, modelView, RenderSystem.getProjectionMatrix(), tick, depth, count, base, rect, local, paradoxSeconds = paradoxSeconds ?: -1f)
                LightHunterShaders.fusionLayers?.let { fusion ->
                    HeartSceneEffect.bindHandMask(fusion, handMask)
                    paradoxSeconds?.let { seconds -> LightHunterFusionRenderer.drawLayered(LightHunterFusionRenderer.meshes, meshes, fusion, modelView, RenderSystem.getProjectionMatrix(), seconds, tick, depth, count, base, rect, local) }
                }
                val paradox = LightHunterShaders.paradoxLayers
                val model = LightHunterShaders.paradoxModel
                if (paradoxSeconds != null && paradox != null && model != null) {
                    HeartSceneEffect.bindHandMask(paradox, handMask)
                    meshes.bindSceneDepth(paradox)
                    HeartLayerImages.bindArray(3, depth)
                    paradox.getUniform("LayerDepths")?.set(3)
                    paradox.getUniform("LayerCount")?.set(count)
                    paradox.getUniform("LayerBase")?.set(base)
                    paradox.getUniform("LayerMode")?.set(if (local) 1 else 0)
                    paradox.getUniform("LayerOrigin")?.set(if (local) rect[0].toFloat() else 0f, if (local) rect[1].toFloat() else 0f)
                    paradox.getUniform("LayerRect")?.set(rect[0].toFloat(), rect[1].toFloat(), rect[2].toFloat(), rect[3].toFloat())
                    LightHunterParadoxRenderer.draw(
                        LightHunterParadoxRenderer.meshes, paradox, model, modelView,
                        RenderSystem.getProjectionMatrix(), paradoxSeconds, tick, blockEntity, true, local,
                        if (opaqueWindowPrepared) LightHunterParadoxRenderer.WINDOW_TRANSLUCENT else LightHunterParadoxRenderer.WINDOW_COMPLETE
                    )
                }
            }
        }
    }

    internal fun captureLayeredSceneDepth(): Boolean = LightHunterShaders.layers?.let { meshes.captureSceneDepth(it) } == true

    /** One integration per four adjacent depth bins; the field cache is shared by every group. */
    @JvmOverloads
    fun drawLayered(
        meshes: Meshes,
        shader: ShaderInstance,
        modelView: Matrix4f,
        projection: Matrix4f,
        tick: Float,
        depth: Int,
        count: Int,
        base: Int,
        rect: IntArray,
        local: Boolean,
        layerMask: Int = LightHunterVfxGeometry.ALL_LAYERS,
        paradoxSeconds: Float = -1f
    ) {
        val activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
        val previousField = GL11C.glGetInteger(GL12C.GL_TEXTURE_BINDING_3D)
        try {
            setParadoxMask(shader, modelView, paradoxSeconds)
            val field = if (layerMask and (1 shl LightHunterVfxGeometry.PLUMES) != 0) {
                meshes.preparePlasmaField(shader, tick)
            } else {
                shader.getUniform("PlasmaField")?.set(2)
                shader.getUniform("UsePlasmaField")?.set(0)
                0
            }

            RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
            GL11C.glBindTexture(GL12C.GL_TEXTURE_3D, field)
            HeartLayerImages.bindArray(3, depth)
            shader.getUniform("LayerDepths")?.set(3)
            shader.getUniform("LayerCount")?.set(count)
            shader.getUniform("LayerBase")?.set(base)
            shader.getUniform("LayerMode")?.set(if (local) 1 else 0)
            shader.getUniform("LayerOrigin")?.set(if (local) rect[0].toFloat() else 0f, if (local) rect[1].toFloat() else 0f)
            shader.getUniform("LayerRect")?.set(rect[0].toFloat(), rect[1].toFloat(), rect[2].toFloat(), rect[3].toFloat())
            shader.getUniform("Time")?.set(tick / 20f)
            // ShaderInstance may retain this blend mode after another pass disables blending.
            // Ordered draws must establish the same premultiplied state as the offscreen bins.
            RenderSystem.enableBlend()
            RenderSystem.blendEquation(GL14C.GL_FUNC_ADD)
            RenderSystem.blendFuncSeparate(GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA)
            RenderSystem.depthMask(false)
            RenderSystem.disableCull()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.colorMask(true, true, true, local)
            fun drawLayer(mesh: Int, effect: Int) {
                RenderSystem.disableDepthTest()
                shader.getUniform("Effect")?.set(effect)
                RenderUtils.withBoundBuffer(meshes.buffer(mesh)) { meshes.buffer(mesh).drawWithShader(modelView, projection, shader) }
            }

            if (layerMask and (1 shl LightHunterVfxGeometry.CHART_FIELD) != 0) {
                drawLayer(LightHunterVfxGeometry.CHART_FIELD, LightHunterVfxGeometry.CHART_FIELD)
            }

            if (layerMask and (1 shl LightHunterVfxGeometry.SHIELDS) != 0) drawLayer(LightHunterVfxGeometry.SHIELDS, SHIELD_FIELD_EFFECT)
            for (layer in 0 until LightHunterVfxGeometry.CHART_FIELD) {
                if (layerMask and (1 shl layer) != 0) drawLayer(layer, layer)
            }
        } finally {
            shader.clear()
            RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
            GL11C.glBindTexture(GL12C.GL_TEXTURE_3D, previousField)
            RenderSystem.activeTexture(activeTexture)
        }
    }

    /** The offline harness invokes this exact game draw path, including material and depth state. */
    @JvmOverloads
    fun draw(
        meshes: Meshes,
        shader: ShaderInstance,
        fieldShader: ShaderInstance,
        modelView: Matrix4f,
        projection: Matrix4f,
        tick: Float,
        layerMask: Int = LightHunterVfxGeometry.ALL_LAYERS,
        paradoxSeconds: Float = -1f
    ) {
        val activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE)
        RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
        val textureZero = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        RenderSystem.activeTexture(GL13C.GL_TEXTURE1)
        val textureOne = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D)
        RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
        val textureTwo3D = GL11C.glGetInteger(GL12C.GL_TEXTURE_BINDING_3D)
        val hasSamplers = GL.getCapabilities().OpenGL33
        val samplerTwo = if (hasSamplers) GL11C.glGetInteger(GL33C.GL_SAMPLER_BINDING) else 0
        try {
            setParadoxMask(shader, modelView, paradoxSeconds)
            setParadoxMask(fieldShader, modelView, paradoxSeconds)
            RenderUtils.withSavedRenderState {
                RenderSystem.enableDepthTest()
                RenderSystem.depthFunc(GL11C.GL_LEQUAL)
                RenderSystem.depthMask(false)
                RenderSystem.disableCull()
                RenderSystem.enableBlend()
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                val volumeMask = (1 shl LightHunterVfxGeometry.PLUMES) or (1 shl LightHunterVfxGeometry.RINGS)
                val depthCaptured = layerMask and volumeMask != 0 && meshes.captureSceneDepth(shader)
                val drawPlumes = layerMask and (1 shl LightHunterVfxGeometry.PLUMES) != 0 && depthCaptured
                if (drawPlumes) {
                    val field = meshes.preparePlasmaField(shader, tick)
                    RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
                    GL11C.glBindTexture(GL12C.GL_TEXTURE_3D, field)
                    if (hasSamplers) GL33C.glBindSampler(2, 0)
                }
                // Transmitting blue film first; emissive chart marks then remain legible over its tint.
                if (layerMask and (1 shl LightHunterVfxGeometry.CHART_FIELD) != 0) {
                    RenderSystem.defaultBlendFunc()
                    fieldShader.getUniform("Time")?.set(tick / 20f)
                    fieldShader.getUniform("Effect")?.set(LightHunterVfxGeometry.CHART_FIELD)
                    val film = meshes.buffer(LightHunterVfxGeometry.CHART_FIELD)
                    RenderUtils.withBoundBuffer(film) { film.drawWithShader(modelView, projection, fieldShader) }
                }
                // Reuse the transmitting material for a blue shield body below the emissive lattice.
                if (layerMask and (1 shl LightHunterVfxGeometry.SHIELDS) != 0) {
                    RenderSystem.defaultBlendFunc()
                    fieldShader.getUniform("Time")?.set(tick / 20f)
                    fieldShader.getUniform("Effect")?.set(SHIELD_FIELD_EFFECT)
                    val shields = meshes.buffer(LightHunterVfxGeometry.SHIELDS)
                    RenderUtils.withBoundBuffer(shields) { shields.drawWithShader(modelView, projection, fieldShader) }
                }
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE)
                shader.getUniform("Time")?.set(tick / 20f)

                for (layer in 0 until LightHunterVfxGeometry.LAYERS) {
                    if (layer == LightHunterVfxGeometry.CHART_FIELD || layerMask and (1 shl layer) == 0) continue
                    if (layer == LightHunterVfxGeometry.PLUMES || layer == LightHunterVfxGeometry.RINGS) {
                        if (!depthCaptured) continue
                        // The ray is clipped against copied scene depth in the shader. Testing the
                        // proxy's exit surface here would erase gas in front of an opaque object.
                        RenderSystem.disableDepthTest()
                    } else {
                        RenderSystem.enableDepthTest()
                    }

                    shader.getUniform("Effect")?.set(layer)
                    val mesh = meshes.buffer(layer)
                    RenderUtils.withBoundBuffer(mesh) { mesh.drawWithShader(modelView, projection, shader) }
                }
            }
        } finally {
            RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
            GL11C.glBindTexture(GL12C.GL_TEXTURE_3D, textureTwo3D)
            if (hasSamplers) GL33C.glBindSampler(2, samplerTwo)
            RenderSystem.activeTexture(GL13C.GL_TEXTURE1)
            GlStateManager._bindTexture(textureOne)
            RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
            GlStateManager._bindTexture(textureZero)
            RenderSystem.activeTexture(activeTexture)
        }
    }

    private fun setParadoxMask(shader: ShaderInstance, modelView: Matrix4f, seconds: Float) {
        val state = LightHunterParadoxTiming.window(seconds)
        shader.getUniform("ParadoxSignal")?.set(state.reveal, state.glitch, state.frame)
        shader.getUniform("ParadoxPlane")?.set(LightHunterParadoxTiming.WINDOW_X + LightHunterParadoxModels.windowView(modelView).depthBias)
    }

    class Meshes @JvmOverloads constructor(resources: ResourceProvider? = null) : AutoCloseable {
        private val builder by lazy { BufferBuilder(262144) }
        private val buffers = arrayOfNulls<VertexBuffer>(LightHunterVfxGeometry.LAYERS)
        private val sceneDepth = DeferredSceneDepth()
        private val plasmaNoise = PlasmaNoise()
        private val plasmaField = LightHunterPlasmaFieldCache(resources)

        fun setResources(resources: ResourceProvider) = plasmaField.setResources(resources)

        fun beginFrame() = plasmaField.beginFrame()

        fun captureSceneDepth(shader: ShaderInstance): Boolean = sceneDepth.capture(shader)

        fun bindSceneDepth(shader: ShaderInstance) = sceneDepth.bind(shader)

        fun bindPlasmaNoise(shader: ShaderInstance) {
            plasmaNoise.bind(shader)
        }

        /** Bind the returned 3D texture on unit 2 while consuming it. */
        fun preparePlasmaField(shader: ShaderInstance, tick: Float): Int {
            val noise = plasmaNoise.bind(shader)
            val useField = shader.getUniform("UsePlasmaField") ?: return 0
            val field = plasmaField.prepare(noise, tick)
            shader.getUniform("PlasmaField")?.set(2)
            useField.set(if (field != 0) 1 else 0)

            return field
        }

        fun buffer(layer: Int): VertexBuffer = buffers[layer] ?: run {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
            LightHunterVfxGeometry.write(builder, layer)
            RenderUtils.uploadStaticBuffer(builder).also { buffers[layer] = it }
        }

        override fun close() {
            sceneDepth.close()
            plasmaField.close()
            plasmaNoise.close()

            for (index in buffers.indices) {
                buffers[index]?.close()
                buffers[index] = null
            }
        }
    }

    /** Z slices in an RG atlas: each texel holds slices z and z + 1. */
    private class PlasmaNoise : AutoCloseable {
        private var texture = 0

        fun bind(shader: ShaderInstance): Int {
            if (texture == 0) {
                val values = MemoryUtil.memAllocFloat(520 * 520 * 2)
                try {
                    for (z in 0 until 64) {
                        for (y in 0..64) {
                            for (x in 0..64) {
                                // The extra row and column wrap XY inside the tile, so filtering
                                // never leaks into an unrelated slice at an atlas boundary.
                                val index = ((z / 8 * 65 + y) * 520 + z % 8 * 65 + x) * 2
                                values.put(index, hash(x % 64, y % 64, z))
                                values.put(index + 1, hash(x % 64, y % 64, (z + 1) % 64))
                            }
                        }
                    }

                    texture = GlStateManager._genTexture()
                    RenderSystem.activeTexture(GL13C.GL_TEXTURE1)
                    GlStateManager._bindTexture(texture)
                    RenderUtils.withTightlyPackedPixels {
                        GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_RG16F, 520, 520, 0, GL30C.GL_RG, GL11C.GL_FLOAT, values)
                    }

                    GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR)
                    GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR)
                    GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE)
                    GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE)
                } finally {
                    MemoryUtil.memFree(values)
                }
            }

            shader.setSampler("PlasmaNoise", texture)
            return texture
        }

        private fun hash(x: Int, y: Int, z: Int): Float {
            fun fract(value: Float) = value - floor(value)
            val a = fract(x * 0.1031f)
            val b = fract(y * 0.1031f)
            val c = fract(z * 0.1031f)
            val d = a * (b + 33.33f) + b * (c + 33.33f) + c * (a + 33.33f)

            return fract(((a + d) + (b + d)) * (c + d))
        }

        override fun close() {
            if (texture != 0) GlStateManager._deleteTexture(texture)
            texture = 0
        }
    }
}

@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object LightHunterVfxLifecycle {
    @SubscribeEvent
    @JvmStatic
    fun onRenderTick(event: TickEvent.RenderTickEvent) {
        if (event.phase == TickEvent.Phase.START) LightHunterVfxRenderer.beginFrame()
    }

    @SubscribeEvent
    @JvmStatic
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level.isClientSide) {
            LightHunterParadoxAnimation.clear()
            LightHunterVfxRenderer.releaseBuffers()
        }
    }
}