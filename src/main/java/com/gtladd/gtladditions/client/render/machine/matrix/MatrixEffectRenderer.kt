package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.heart.HeartLayerImages
import com.gtladd.gtladditions.client.render.scene.CompositePartRenderer
import com.gtladd.gtladditions.client.render.scene.LayeredPartRenderer
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.common.machine.multiblock.controller.ApocalypticWarpQuantumMatrix
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Vector3f
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
object MatrixEffectRenderer {
    // After the six beams and the core.
    private const val FRAME_LIGHTS = 7
    private const val IRIS_STAR = 8
    private const val EYE_MIST = 9
    private var frameLights: List<VertexBuffer>? = null
    internal val provider = SceneRendering.register(GTLAdditions.id("matrix"))

    /** From the cube centre, which every layer turns about rigidly. */
    private val frameLightReach by lazy {
        (0 until MatrixEffectGeometry.ENERGY_CORE_LAYER_INDEX).maxOf { index ->
            MatrixFrameDetails.layer(index).lights.maxOf { quad ->
                val v = quad.vertices
                (0..9 step 3).maxOf { Vector3f(v[it], v[it + 1], v[it + 2]).length() }
            }
        }.toDouble()
    }

    fun enqueue(blockEntity: BlockEntity, machine: ApocalypticWarpQuantumMatrix, ticks: Double, eyeSeconds: Double) {
        MatrixFrameTerrainRenderer.enqueue(blockEntity, machine, ticks)
        // No frame during the shaderpack shadow pass, and no eye sample either.
        val frame = SceneRendering.frame() ?: return
        val eye = MatrixEyeRenderer.sample(blockEntity, eyeSeconds, machine.matrixEffects.isRunning())
        val entry = Entry(blockEntity, machine, ticks, eye)
        val yaw = facingYaw(machine.frontFacing)
        val pos = blockEntity.blockPos
        MatrixEyeRenderer.submit(frame, blockEntity, eye, yaw)
        frame.submit(
            ScenePart.builder(PartKey.block(provider, pos, EYE_MIST), EyeMist, entry)
                .blend(PartBlend.STRAIGHT)
                .bounds(
                    PartBounds.union(
                        PartBounds.box(MatrixEyeRenderer.bounds(pos, yaw)),
                        MatrixIrisStarRenderer.bounds(frame, pos, yaw, true)
                    )
                )
                .features(PartFeature.SCENE_DEPTH)
                .build()
        )
        // The seal's outer group touches every beam on its plane; both SCREEN, they need no order between them.
        frame.submit(
            ScenePart.builder(PartKey.block(provider, pos, IRIS_STAR), IrisStar, entry)
                .blend(PartBlend.SCREEN)
                .bounds(MatrixIrisStarRenderer.bounds(frame, pos, yaw, false))
                .build()
        )
        MatrixBeamRenderer.submit(frame, blockEntity, eye, yaw)
        // The lights' sphere holds the core's around the same centre: an unordered pair whose tie goes by part number, the core first.
        val center = MatrixFrameTransforms.cubeCenter(pos, yaw)
        frame.submit(
            ScenePart.builder(PartKey.block(provider, pos, FRAME_LIGHTS), FrameLights, entry)
                .blend(PartBlend.ADDITIVE)
                .bounds(PartBounds.sphere(center.x, center.y, center.z, frameLightReach))
                // In the Heart bins the lights test their own depth against the scene.
                .features(PartFeature.SCENE_DEPTH)
                .build()
        )
        MatrixEnergyCoreRenderer.submit(frame, blockEntity, ticks, yaw)
    }

    private fun facingYaw(facing: Direction): Float = when (facing) {
        Direction.WEST -> -90f
        Direction.NORTH -> 180f
        Direction.EAST -> 90f
        else -> 0f
    }

    fun releaseBuffers() {
        MatrixEnergyCoreRenderer.releaseBuffers()
        MatrixEyeRenderer.releaseBuffers()
        MatrixIrisStarRenderer.releaseBuffers()
        MatrixFrameTerrainRenderer.releaseBuffers()
        RenderUtils.releaseBuffers(frameLights)
        frameLights = null
    }

    private fun frameLightBuffers(): List<VertexBuffer> =
        frameLights ?: (0 until MatrixEffectGeometry.ENERGY_CORE_LAYER_INDEX).map { index ->
            RenderUtils.withStagingBuilder { builder ->
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)

                for (quad in MatrixFrameDetails.layer(index).lights) {
                    for (i in 0..3) {
                        builder.vertex(
                            quad.vertices[i * 3].toDouble(),
                            quad.vertices[i * 3 + 1].toDouble(),
                            quad.vertices[i * 3 + 2].toDouble()
                        )
                            .color(quad.color[0], quad.color[1], quad.color[2], quad.alpha).endVertex()
                    }
                }

                RenderUtils.uploadStaticBuffer(builder)
            }
        }.also { frameLights = it }

    /** The program's SRC_ALPHA / ONE, or in the Heart bins premultiplied colour that covers nothing. */
    private object FrameLights : LayeredPartRenderer<Entry> {
        override fun layered(part: ScenePart<Entry>): Boolean = MatrixShaders.sealLayers != null

        override fun draw(context: PartDrawContext, parts: List<ScenePart<Entry>>) {
            val shader = MatrixShaders.seal ?: return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
            for (part in parts) drawLights(context, part.payload, shader)
        }

        override fun drawLayered(
            context: PartDrawContext,
            part: ScenePart<Entry>,
            depth: Int,
            count: Int,
            base: Int,
            rect: IntArray,
            local: Boolean
        ) {
            val shader = MatrixShaders.sealLayers ?: return
            context.sceneDepth!!.bind(shader)
            HeartLayerImages.bindArray(3, depth)
            shader.getUniform("LayerDepths")?.set(3)
            shader.getUniform("LayerCount")?.set(count)
            shader.getUniform("LayerBase")?.set(base)
            shader.getUniform("LayerMode")?.set(if (local) 1 else 0)
            shader.getUniform("LayerOrigin")?.set(if (local) rect[0].toFloat() else 0f, if (local) rect[1].toFloat() else 0f)
            shader.getUniform("LayerRect")?.set(rect[0].toFloat(), rect[1].toFloat(), rect[2].toFloat(), rect[3].toFloat())
            RenderSystem.colorMask(true, true, true, local)
            drawLights(context, part.payload, shader)
        }

        private fun drawLights(context: PartDrawContext, entry: Entry, shader: ShaderInstance) {
            val frame = context.frame
            val pos = entry.blockEntity.blockPos
            val running = entry.machine.matrixEffects.isRunning()
            RenderUtils.withSavedRenderState {
                RenderSystem.disableCull()
                frameLightBuffers().forEachIndexed { index, buffer ->
                    shader.getUniform("Intensity")?.set(
                        if (running) 0.9f + 0.1f * sin(entry.ticks * 0.045 + index).toFloat() else 0.35f
                    )
                    context.poseStack.withPose {
                        translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
                        MatrixFrameTransforms.apply(this, index, entry.ticks, entry.yaw)
                        RenderUtils.withBoundBuffer(buffer) {
                            buffer.drawWithShader(last().pose(), RenderSystem.getProjectionMatrix(), shader)
                        }
                    }
                }
            }
        }
    }

    private object IrisStar : PartRenderer<Entry> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Entry>>) {
            val frame = context.frame

            for (part in parts) {
                val entry = part.payload
                val pos = entry.blockEntity.blockPos
                context.poseStack.withPose {
                    translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
                    MatrixEyeRenderer.applyTransform(this, entry.yaw)
                    if (!MatrixIrisStarRenderer.render(this, entry.eye, false)) context.report(part, PartOutcome.RESOURCE_UNAVAILABLE)
                }
            }
        }
    }

    private object EyeMist : CompositePartRenderer<Entry> {
        override fun composite(part: ScenePart<Entry>): Boolean = true

        override fun draw(context: PartDrawContext, parts: List<ScenePart<Entry>>) {
            val frame = context.frame

            for (part in parts) {
                val entry = part.payload
                val pos = entry.blockEntity.blockPos
                val outcome = context.poseStack.withPose {
                    translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
                    MatrixEyeRenderer.applyTransform(this, entry.yaw)
                    val mist = MatrixEyeRenderer.renderMist(this, entry.eye, context.sceneDepth!!)
                    if (MatrixIrisStarRenderer.render(this, entry.eye, true)) mist else PartOutcome.RESOURCE_UNAVAILABLE
                }

                if (outcome != null) context.report(part, outcome)
            }
        }
    }

    private class Entry(
        val blockEntity: BlockEntity,
        val machine: ApocalypticWarpQuantumMatrix,
        val ticks: Double,
        val eye: MatrixEyeAnimation.Frame
    ) {
        val yaw: Float
            get() = facingYaw(machine.frontFacing)
    }
}