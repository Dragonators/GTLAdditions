package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.render.machine.heart.HeartLayerImages
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
    internal val meshes = Meshes()
    private var smoothFrameTick: Float? = null

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
        val frame = SceneRendering.frame() ?: return
        val entry = Entry(
            blockEntity,
            machine,
            tick,
            if (machine.unlockParadoxical()) LightHunterParadoxAnimation.seconds(machine) else null
        )
        LightHunterSolidParts.submit(frame, entry)
        if (machine.recipeLogic.isWorking) LightHunterVfxParts.submit(frame, entry)
    }

    internal fun setResources(resources: ResourceProvider) {
        meshes.setResources(resources)
        LightHunterFusionRenderer.meshes.setResources(resources)
    }

    fun releaseBuffers() {
        val release = {
            smoothFrameTick = null
            meshes.close()
            LightHunterFusionRenderer.meshes.close()
            LightHunterParadoxRenderer.meshes.close()
            LightHunterInteriorRenderer.close()
        }
        if (RenderSystem.isOnRenderThread()) release() else RenderSystem.recordRenderCall { release() }
    }

    internal data class Entry(
        val blockEntity: BlockEntity,
        val machine: LightHunterSpaceStation,
        val tick: Float,
        val paradoxSeconds: Float? = null
    ) {
        // Set by this frame's OPAQUE pass, which runs before the composition draws the window.
        internal var opaqueWindowDrawn = false

        val windowPass: Int
            get() = if (opaqueWindowDrawn) LightHunterParadoxRenderer.WINDOW_TRANSLUCENT else LightHunterParadoxRenderer.WINDOW_COMPLETE
    }

    /** One integration per four adjacent depth bins. The caller binds the scene depth. */
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

    /**
     * The plumes and rings clip their rays against the scene depth the caller bound to [shader]; [fieldShader] draws the
     * chart film and the shield body.
     */
    @JvmOverloads
    fun draw(
        meshes: Meshes,
        shader: ShaderInstance,
        fieldShader: ShaderInstance?,
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
            fieldShader?.let { setParadoxMask(it, modelView, paradoxSeconds) }
            RenderUtils.withSavedRenderState {
                RenderSystem.enableDepthTest()
                RenderSystem.depthFunc(GL11C.GL_LEQUAL)
                RenderSystem.depthMask(false)
                RenderSystem.disableCull()
                RenderSystem.enableBlend()
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                if (layerMask and (1 shl LightHunterVfxGeometry.PLUMES) != 0) {
                    val field = meshes.preparePlasmaField(shader, tick)
                    RenderSystem.activeTexture(GL13C.GL_TEXTURE2)
                    GL11C.glBindTexture(GL12C.GL_TEXTURE_3D, field)
                    if (hasSamplers) GL33C.glBindSampler(2, 0)
                }

                if (fieldShader != null) {
                    // Transmitting blue film first; emissive chart marks then remain legible over its tint.
                    if (layerMask and (1 shl LightHunterVfxGeometry.CHART_FIELD) != 0) {
                        RenderSystem.defaultBlendFunc()
                        fieldShader.getUniform("Time")?.set(tick / 20f)
                        fieldShader.getUniform("Effect")?.set(LightHunterVfxGeometry.CHART_FIELD)
                        val film = meshes.buffer(LightHunterVfxGeometry.CHART_FIELD)
                        RenderUtils.withBoundBuffer(film) { film.drawWithShader(modelView, projection, fieldShader) }
                    }

                    if (layerMask and (1 shl LightHunterVfxGeometry.SHIELDS) != 0) {
                        RenderSystem.defaultBlendFunc()
                        fieldShader.getUniform("Time")?.set(tick / 20f)
                        fieldShader.getUniform("Effect")?.set(SHIELD_FIELD_EFFECT)
                        val shields = meshes.buffer(LightHunterVfxGeometry.SHIELDS)
                        RenderUtils.withBoundBuffer(shields) { shields.drawWithShader(modelView, projection, fieldShader) }
                    }
                }

                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE)
                shader.getUniform("Time")?.set(tick / 20f)

                for (layer in 0 until LightHunterVfxGeometry.LAYERS) {
                    if (layer == LightHunterVfxGeometry.CHART_FIELD || layerMask and (1 shl layer) == 0) continue
                    if (layer == LightHunterVfxGeometry.PLUMES || layer == LightHunterVfxGeometry.RINGS) {
                        // The ray is clipped against the scene depth in the shader. Testing the
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
        private val plasmaNoise = PlasmaNoise()
        private val plasmaField = LightHunterPlasmaFieldCache(resources)

        fun setResources(resources: ResourceProvider) = plasmaField.setResources(resources)

        fun beginFrame() = plasmaField.beginFrame()

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