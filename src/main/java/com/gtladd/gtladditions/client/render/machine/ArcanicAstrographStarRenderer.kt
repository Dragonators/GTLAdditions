package com.gtladd.gtladditions.client.render.machine

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.GTLAddRenderTypes
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.mixin.minecraft.client.BufferSourceAccessor
import com.gtladd.gtladditions.utils.RenderUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.function.Supplier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTLAdditions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ArcanicAstrographStarRenderer {
    const val RADIUS = 1.4f
    const val SPACE_RADIUS = 12.95f
    private const val SLICES = 64
    private const val STACKS = 64
    private const val VERTEX_STRIDE = 5

    private var surfaceShader: ShaderInstance? = null
    private var glowShader: ShaderInstance? = null

    private class Material(texturePath: String, val additive: Boolean, argb: Int) {
        val red = argb ushr 16 and 255
        val green = argb ushr 8 and 255
        val blue = argb and 255
        val alpha = argb ushr 24 and 255
        val renderType = GTLAddRenderTypes.createAstrographStarLayer(
            GTLAdditions.id(texturePath),
            additive,
            Supplier { shader() }
        )

        fun shader(): ShaderInstance? = if (additive) glowShader else surfaceShader
    }

    private data class StarLayer(
        val material: Material,
        val radius: Float,
        val phaseDegrees: Float,
        val degreesPerTick: Float,
        val axis: Vector3f
    )

    // Match the byte-quantized vertex colors used by the buffered renderer.
    private val starLayers = arrayOf(
        StarLayer(
            Material("textures/effect/star/star_layer_0.png", false, 0xFFFF660C.toInt()),
            RADIUS * 0.9025f,
            130f,
            1.5f,
            Vector3f(0f, 1f, 1f).normalize()
        ),
        StarLayer(
            Material("textures/effect/star/star_layer_1.png", true, 0x66FF660C),
            RADIUS * 0.95f,
            -49f,
            1.2f,
            Vector3f(1f, 1f, 0f).normalize()
        ),
        StarLayer(
            Material("textures/effect/star/star_layer_2.png", true, 0x33FF660C),
            RADIUS,
            67f,
            1.6f,
            Vector3f(1f, 0f, 1f).normalize()
        )
    )
    private val spaceMaterial = Material("textures/effect/astrograph/space_layer.png", false, -1)
    private val modelRotation = Quaternionf(0f, 1f, 0f, 0f)
    private val layerRotation = Quaternionf()
    private val modelView = Matrix4f()
    private val sphere = buildSphere()
    private val meshBuilder by lazy { BufferBuilder(524288) }
    private var sphereBuffer: VertexBuffer? = null

    @SubscribeEvent
    @JvmStatic
    fun registerShaders(event: RegisterShadersEvent) {
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_astrograph_surface"),
            DefaultVertexFormat.POSITION_COLOR_TEX
        )
        ) { surfaceShader = it }
        event.registerShader(
            ShaderInstance(event.resourceProvider,
            GTLAdditions.id("gtladditions_astrograph_glow"),
            DefaultVertexFormat.POSITION_COLOR_TEX)
        ) { glowShader = it }
    }

    @SubscribeEvent
    @JvmStatic
    fun registerReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(ResourceManagerReloadListener { releaseBuffers() })
    }

    fun releaseBuffers() {
        if (RenderSystem.isOnRenderThread()) {
            sphereBuffer?.close()
            sphereBuffer = null
        } else {
            RenderSystem.recordRenderCall { releaseBuffers() }
        }
    }

    /** One immutable unit-sphere upload, reused by the sky and all three stellar surfaces. */
    fun renderCached(poseStack: PoseStack, tick: Float) {
        if (surfaceShader == null || glowShader == null) return
        val buffer = getSphereBuffer()
        RenderUtils.withSavedRenderState {
            RenderUtils.withBoundBuffer(buffer) {
                poseStack.withPose {
                    scale(-SPACE_RADIUS, SPACE_RADIUS, SPACE_RADIUS)
                    drawCachedSurface(buffer, this, spaceMaterial)
                }
                forEachStarLayer(poseStack, tick) { pose, material ->
                    drawCachedSurface(buffer, pose, material)
                }
            }
        }
    }

    private fun getSphereBuffer(): VertexBuffer = sphereBuffer ?: run {
        val builder = meshBuilder
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX)
        var vertex = 0

        while (vertex < sphere.size) {
            builder.vertex(sphere[vertex].toDouble(), sphere[vertex + 1].toDouble(), sphere[vertex + 2].toDouble())
                .color(255, 255, 255, 255)
                .uv(sphere[vertex + 3], sphere[vertex + 4])
                .endVertex()
            vertex += VERTEX_STRIDE
        }

        RenderUtils.uploadStaticBuffer(builder).also { sphereBuffer = it }
    }

    private fun drawCachedSurface(buffer: VertexBuffer, pose: PoseStack, material: Material) {
        val shader = material.shader() ?: return
        val color = RenderSystem.getShaderColor()
        val red = color[0]
        val green = color[1]
        val blue = color[2]
        val alpha = color[3]
        try {
            RenderSystem.setShaderColor(
                red * (material.red / 255f),
                green * (material.green / 255f),
                blue * (material.blue / 255f),
                alpha * (material.alpha / 255f)
            )
            RenderUtils.withRenderType(material.renderType) {
                // Buffered vertices already contain PoseStack; BufferUploader applies this global matrix afterward.
                modelView.set(RenderSystem.getModelViewMatrix()).mul(pose.last().pose())
                buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), shader)
            }
        } finally {
            RenderSystem.setShaderColor(red, green, blue, alpha)
        }
    }

    /** Resolve each layer separately: breaking overlays forward stellar layers without modifying their vertices. */
    fun render(poseStack: PoseStack, buffer: MultiBufferSource, tick: Float) {
        if (surfaceShader == null || glowShader == null) return
        forEachStarLayer(poseStack, tick) { pose, material ->
            renderSurface(pose, buffer, material)
        }
    }

    fun renderSpaceShell(poseStack: PoseStack, buffer: MultiBufferSource) {
        if (surfaceShader == null) return
        poseStack.withPose {
            scale(-SPACE_RADIUS, SPACE_RADIUS, SPACE_RADIUS)
            renderSurface(this, buffer, spaceMaterial)
        }
    }

    private fun renderSurface(pose: PoseStack, source: MultiBufferSource, material: Material) {
        // Ask the caller once, preserving custom consumers and render-type substitutions.
        val consumer = source.getBuffer(material.renderType)
        val mainBuffers = cachedBufferSource(consumer, material.renderType)
        if (mainBuffers == null) {
            emitSphere(pose, consumer, material)
            return
        }

        // Finish pending opaque models and this layer before switching to an immediate VBO draw.
        mainBuffers.endBatch(RenderType.solid())
        mainBuffers.endBatch(material.renderType)
        val mesh = getSphereBuffer()
        RenderUtils.withSavedRenderState {
            RenderUtils.withBoundBuffer(mesh) {
                drawCachedSurface(mesh, pose, material)
            }
        }
    }

    private fun cachedBufferSource(consumer: VertexConsumer, type: RenderType): MultiBufferSource.BufferSource? {
        val source = Minecraft.getInstance()?.renderBuffers()?.bufferSource() ?: return null
        val access = source as? BufferSourceAccessor ?: return null
        return source.takeIf { access.activeRenderType.orElse(null) === type && access.getRawBuffer(type) === consumer }
    }

    private inline fun forEachStarLayer(
        poseStack: PoseStack,
        tick: Float, draw: (PoseStack, Material) -> Unit
    ) {
        poseStack.withPose {
            mulPose(modelRotation)

            for ((material, radius, phaseDegrees, degreesPerTick, axis) in starLayers) {
                withPose {
                    val angle = Math.toRadians((phaseDegrees + tick.toDouble() * degreesPerTick) % 360.0).toFloat()
                    mulPose(layerRotation.rotationAxis(angle, axis))
                    scale(radius, radius, radius)
                    draw(this, material)
                }
            }
        }
    }

    private fun emitSphere(poseStack: PoseStack, consumer: VertexConsumer, material: Material) {
        val pose = poseStack.last().pose()
        var vertex = 0

        while (vertex < sphere.size) {
            consumer.vertex(pose, sphere[vertex], sphere[vertex + 1], sphere[vertex + 2])
                .color(material.red, material.green, material.blue, material.alpha)
                .uv(sphere[vertex + 3], sphere[vertex + 4])
                .endVertex()
            vertex += VERTEX_STRIDE
        }
    }

    private fun buildSphere(): FloatArray {
        val vertices = FloatArray(SLICES * STACKS * 4 * VERTEX_STRIDE)
        var cursor = 0
        fun vertex(stack: Int, slice: Int) {
            val v = stack.toDouble() / STACKS
            val u = slice.toDouble() / SLICES
            val latitude = PI / 2 - v * PI
            val longitude = u * 2 * PI
            vertices[cursor++] = (cos(latitude) * cos(longitude)).toFloat()
            vertices[cursor++] = sin(latitude).toFloat()
            vertices[cursor++] = (cos(latitude) * sin(longitude)).toFloat()
            vertices[cursor++] = (1 - u).toFloat()
            vertices[cursor++] = v.toFloat()
        }

        for (stack in 0 until STACKS) {
            for (slice in 0 until SLICES) {
                vertex(stack, slice)
                vertex(stack, slice + 1)
                vertex(stack + 1, slice + 1)
                vertex(stack + 1, slice)
            }
        }

        return vertices
    }
}