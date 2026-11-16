package com.gtladd.gtladditions.client.render.machine

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartPass
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.GTLAddRenderTypes
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.heart.HeartHandDepthMask
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.mixin.minecraft.client.BufferSourceAccessor
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
import net.minecraft.core.Direction
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.GL13C
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
            additive
        ) { shader() }

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
    private val solidLayers = 0..0
    private val glowLayers = 1..2
    private val modelRotation = Quaternionf(0f, 1f, 0f, 0f)
    private val layerRotation = Quaternionf()
    private val modelView = Matrix4f()
    private val sphere = buildSphere()
    private val meshBuilder by lazy { BufferBuilder(524288) }
    private var sphereBuffer: VertexBuffer? = null
    private val provider = SceneRendering.register(GTLAdditions.id("astrograph_star"))

    @SubscribeEvent
    @JvmStatic
    fun registerShaders(event: RegisterShadersEvent) {
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_astrograph_surface"),
            DefaultVertexFormat.POSITION_COLOR_TEX
        ) { surfaceShader = it }
        RenderUtils.registerShader(
            event,
            GTLAdditions.id("gtladditions_astrograph_glow"),
            DefaultVertexFormat.POSITION_COLOR_TEX
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

    /**
     * Under a shaderpack the OPAQUE part draws after the first-person hand, and a camera against the shell or the core puts
     * them in front of it, so the solid part discards the hand's pixels itself.
     */
    fun submit(frame: SceneFrame, blockEntity: BlockEntity, facing: Direction, tick: Double) {
        val pos = blockEntity.blockPos
        val star = Star(pos.x + 0.5 - facing.stepX * 16.0, pos.y + 0.5, pos.z + 0.5 - facing.stepZ * 16.0, tick)
        frame.submit(
            ScenePart.builder(PartKey.block(provider, pos, 0), Solid, star)
                .pass(PartPass.OPAQUE)
                .bounds(PartBounds.sphere(star.x, star.y, star.z, SPACE_RADIUS.toDouble()))
                .features(PartFeature.OWN_HAND_MASK)
                .build()
        )
        frame.submit(
            ScenePart.builder(PartKey.block(provider, pos, 1), Glow, star)
                .blend(PartBlend.ADDITIVE)
                .bounds(PartBounds.sphere(star.x, star.y, star.z, RADIUS.toDouble()))
                .build()
        )
    }

    private fun drawParts(context: PartDrawContext, parts: List<ScenePart<Star>>, solid: Boolean) {
        if ((if (solid) surfaceShader else glowShader) == null) {
            for (part in parts) context.report(part, PartOutcome.RESOURCE_UNAVAILABLE)
            return
        }

        val frame = context.frame
        val buffer = getSphereBuffer()
        val handMask = context.handMaskTexture
        RenderUtils.withSavedRenderState {
            // Back-face culling shows the mirrored shell from inside and each glow layer's near half.
            RenderSystem.enableCull()
            RenderUtils.withBoundBuffer(buffer) {
                for (part in parts) {
                    val star = part.payload
                    context.poseStack.withPose {
                        translate(star.x - frame.cameraX, star.y - frame.cameraY, star.z - frame.cameraZ)
                        if (solid) {
                            withPose {
                                scale(-SPACE_RADIUS, SPACE_RADIUS, SPACE_RADIUS)
                                drawCachedSurface(buffer, this, spaceMaterial, handMask)
                            }
                        }

                        forEachStarLayer(this, star.tick, if (solid) solidLayers else glowLayers) { pose, material ->
                            drawCachedSurface(buffer, pose, material, handMask)
                        }
                    }
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

    /**
     * [handMask] holds only for this draw: the reset to 0 means a later flush of vertices queued through the same program, as
     * the wrapped consumers do, never discards.
     */
    private fun drawCachedSurface(buffer: VertexBuffer, pose: PoseStack, material: Material, handMask: Int) {
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
                // After the render type's setup, whose texture filter call binds on the active unit; unit 0 is active again.
                HeartHandDepthMask.bind(shader, handMask)
                if (handMask != 0) RenderSystem.activeTexture(GL13C.GL_TEXTURE0)
                // Buffered vertices already contain PoseStack; BufferUploader applies this global matrix afterward.
                modelView.set(RenderSystem.getModelViewMatrix()).mul(pose.last().pose())
                buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), shader)
            }
        } finally {
            HeartHandDepthMask.bind(shader, 0)
            RenderSystem.setShaderColor(red, green, blue, alpha)
        }
    }

    /** Resolve each layer separately: breaking overlays forward stellar layers without modifying their vertices. */
    fun render(poseStack: PoseStack, buffer: MultiBufferSource, tick: Double) {
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
                drawCachedSurface(mesh, pose, material, 0)
            }
        }
    }

    private fun cachedBufferSource(consumer: VertexConsumer, type: RenderType): MultiBufferSource.BufferSource? {
        val source = Minecraft.getInstance().renderBuffers().bufferSource()
        val access = source as? BufferSourceAccessor ?: return null
        return source.takeIf { access.lastState.orElse(null) === type && access.invokeGetBuilderRaw(type) === consumer }
    }

    private inline fun forEachStarLayer(
        poseStack: PoseStack,
        tick: Double,
        layers: IntRange = starLayers.indices,
        draw: (PoseStack, Material) -> Unit
    ) {
        poseStack.withPose {
            mulPose(modelRotation)

            for (index in layers) {
                val (material, radius, phaseDegrees, degreesPerTick, axis) = starLayers[index]
                withPose {
                    val angle = Math.toRadians((phaseDegrees + tick * degreesPerTick) % 360.0).toFloat()
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

    private class Star(val x: Double, val y: Double, val z: Double, val tick: Double)

    private object Solid : PartRenderer<Star> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Star>>) = drawParts(context, parts, true)
    }

    private object Glow : PartRenderer<Star> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Star>>) = drawParts(context, parts, false)
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