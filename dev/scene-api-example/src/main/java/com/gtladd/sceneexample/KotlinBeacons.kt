package com.gtladd.sceneexample

import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartPass
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.ProviderHandle
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneProvider
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.sceneexample.Placements.Placement
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.logging.LogUtils
import com.mojang.math.Axis
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.event.RegisterShadersEvent
import java.io.IOException

/**
 * The Kotlin provider, pull style: the scheduler asks for the parts in [collect]. A beacon is an OPAQUE core that keeps the
 * first-person hand in front under a shaderpack, and an ADDITIVE halo; crossing halos commute.
 */
object KotlinBeacons : SceneProvider {
    private const val PART_CORE = 0
    private const val PART_HALO = 1
    private const val CORE_EDGE = 0.8f
    private val logger = LogUtils.getLogger()
    private lateinit var handle: ProviderHandle
    private var coreShader: ShaderInstance? = null
    private var haloShader: ShaderInstance? = null
    private var resourceGeneration = -1
    private var pipelineGeneration = -1

    fun registerShaders(event: RegisterShadersEvent) {
        coreShader = null
        haloShader = null
        try {
            event.registerShader(
                ShaderInstance(event.resourceProvider, ResourceLocation(SceneExample.MOD_ID, "beacon_core"), DefaultVertexFormat.POSITION)
            ) { coreShader = it }
            event.registerShader(
                ShaderInstance(event.resourceProvider, ResourceLocation(SceneExample.MOD_ID, "beacon_halo"), DefaultVertexFormat.POSITION)
            ) { haloShader = it }
        } catch (error: IOException) {
            logger.error("Unable to load the beacon shaders", error)
        }
    }

    /** Once, on the render thread. */
    fun register() {
        handle = SceneRendering.register(ResourceLocation(SceneExample.MOD_ID, "beacon"), this)
    }

    override fun collect(frame: SceneFrame) {
        for (beacon in Placements.BEACONS) {
            frame.submit(
                ScenePart.builder(PartKey(handle, beacon.id(), PART_CORE), Core, beacon)
                    .pass(PartPass.OPAQUE)
                    // Covers the cube while it turns.
                    .bounds(PartBounds.sphere(beacon.x(), beacon.y(), beacon.z(), CORE_EDGE * 0.87))
                    .features(PartFeature.OWN_HAND_MASK)
                    .build()
            )
            frame.submit(
                ScenePart.builder(PartKey(handle, beacon.id(), PART_HALO), Halo, beacon)
                    .blend(PartBlend.ADDITIVE)
                    .bounds(PartBounds.sphere(beacon.x(), beacon.y(), beacon.z(), beacon.radius()))
                    .batchKey(Halo)
                    .build()
            )
        }
    }

    override fun onResourcesReloaded(generation: Int) {
        resourceGeneration = generation
    }

    override fun onPipelineChanged(generation: Int) {
        pipelineGeneration = generation
    }

    fun status(): String {
        val text = StringBuilder("Kotlin beacons: resources $resourceGeneration, pipeline $pipelineGeneration")

        for (beacon in Placements.BEACONS) {
            val core = SceneRendering.lastOutcome(PartKey(handle, beacon.id(), PART_CORE))
            val halo = SceneRendering.lastOutcome(PartKey(handle, beacon.id(), PART_HALO))
            text.append("\n  beacon ${beacon.id()}: core $core, halo $halo")
        }

        return text.toString()
    }

    private fun drawMeshes(
        context: PartDrawContext,
        parts: List<ScenePart<Placement>>,
        mesh: VertexBuffer,
        shader: ShaderInstance,
        scale: (Placement) -> Float,
        turns: Boolean
    ) {
        val frame = context.frame
        val pose = context.poseStack
        mesh.bind()
        try {
            for (part in parts) {
                val beacon = part.payload
                val size = scale(beacon)
                pose.pushPose()
                try {
                    pose.translate(beacon.x() - frame.cameraX, beacon.y() - frame.cameraY, beacon.z() - frame.cameraZ)
                    if (turns) pose.mulPose(Axis.YP.rotation((frame.renderTicks * 0.05).toFloat()))
                    pose.scale(size, size, size)
                    mesh.drawWithShader(pose.last().pose(), RenderSystem.getProjectionMatrix(), shader)
                } finally {
                    pose.popPose()
                }
            }
        } finally {
            VertexBuffer.unbind()
        }
    }

    private object Core : PartRenderer<Placement> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Placement>>) {
            val shader = coreShader ?: return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
            // 0 without a shaderpack, without a hand, or while the mask is not ready: then nothing is discarded.
            val mask = context.handMaskTexture
            shader.setSampler("HandMask", mask)
            shader.safeGetUniform("HandMaskEnabled").set(if (mask != 0) 1 else 0)
            try {
                drawMeshes(context, parts, Meshes.cube(), shader, { CORE_EDGE }, true)
            } finally {
                shader.safeGetUniform("HandMaskEnabled").set(0)
            }
        }
    }

    private object Halo : PartRenderer<Placement> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Placement>>) {
            val shader = haloShader ?: return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
            drawMeshes(context, parts, Meshes.sphere(), shader, { it.radius().toFloat() }, false)
        }
    }
}
