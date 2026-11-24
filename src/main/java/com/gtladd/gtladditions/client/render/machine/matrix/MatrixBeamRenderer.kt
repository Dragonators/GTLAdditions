package com.gtladd.gtladditions.client.render.machine.matrix

import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartFeature
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.SceneFrame
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.client.RenderUtils
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristBeamRenderer
import com.gtladd.gtladditions.client.render.machine.heart.HeartHandDepthMask
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Vector3f

@OnlyIn(Dist.CLIENT)
internal object MatrixBeamRenderer : PartRenderer<MatrixBeam> {
    private const val RADIUS = MatrixEffectGeometry.BEAM_RADIUS * MatrixIrisStarRenderer.BEAM_HALO_RADIUS_SCALE

    fun submit(frame: SceneFrame, blockEntity: BlockEntity, eye: MatrixEyeAnimation.Frame, yaw: Float) {
        val pos = blockEntity.blockPos
        MatrixIrisStarRenderer.beamOrigins(eye.time, yaw).forEachIndexed { index, origin ->
            val x = pos.x + origin.x.toDouble()
            val y = pos.y + origin.y.toDouble()
            val z = pos.z + origin.z.toDouble()
            frame.submit(
                ScenePart.builder(PartKey.block(MatrixEffectRenderer.provider, pos, index), this, MatrixBeam(blockEntity, eye, origin))
                    .blend(PartBlend.SCREEN)
                    .bounds(PartBounds.box(x - RADIUS, y, z - RADIUS, x + RADIUS, y + MatrixEffectGeometry.BEAM_HEIGHT, z + RADIUS))
                    .features(PartFeature.OWN_HAND_MASK)
                    // One draw takes a single machine's eye frame.
                    .batchKey(blockEntity)
                    .build()
            )
        }
    }

    override fun draw(context: PartDrawContext, parts: List<ScenePart<MatrixBeam>>) {
        val shader = MatrixShaders.irisBeamGlow ?: return parts.forEach { context.report(it, PartOutcome.RESOURCE_UNAVAILABLE) }
        val beam = parts[0].payload
        val frame = context.frame
        val pos = beam.blockEntity.blockPos
        RenderUtils.withSavedRenderState {
            HeartHandDepthMask.bind(shader, context.handMaskTexture)
            RenderSystem.disableCull()
            MatrixIrisStarRenderer.configureBeamShader(shader)
            context.poseStack.withPose {
                translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
                AntichristBeamRenderer.renderVerticalLayers(
                    beam.blockEntity,
                    this,
                    beam.eye.time * 20.0,
                    parts.map { it.payload.origin },
                    MatrixEffectGeometry.BEAM_HEIGHT,
                    RADIUS,
                    MatrixIrisStarRenderer.beamAlpha(beam.eye),
                    shader,
                    Vec3(frame.cameraX, frame.cameraY, frame.cameraZ)
                )
            }
        }
    }
}

internal class MatrixBeam(val blockEntity: BlockEntity, val eye: MatrixEyeAnimation.Frame, val origin: Vector3f)