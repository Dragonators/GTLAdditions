package com.gtladd.gtladditions.client.render.machine

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartDrawContext
import com.gtladd.gtladditions.api.client.scene.PartKey
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import com.gtladd.gtladditions.api.client.scene.PartPass
import com.gtladd.gtladditions.api.client.scene.PartRenderer
import com.gtladd.gtladditions.api.client.scene.ScenePart
import com.gtladd.gtladditions.api.client.scene.SceneRendering
import com.gtladd.gtladditions.client.RenderMode
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristBeamRenderer
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristRenderProfile
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristStarRenderer
import com.gtladd.gtladditions.client.render.withPose
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object SubspaceCorridorHubBeamRenderer {
    private val STAR_TEXTURE = GTLAdditions.id("textures/effect/star/star_layer_1.png")
    private const val STAR_RADIUS = 17.02f
    private const val HEIGHT = 460.0f
    private const val BOTTOM_RADIUS = 12.615f
    private const val TOP_RADIUS = 1.1f
    private const val BEAM_ALPHA = 1.0f
    private const val BASE_STELLAR_COLOR = 0xF37F06
    private val BASE_STELLAR_COLOR_R = ((BASE_STELLAR_COLOR shr 16) and 0xFF) / 255.0f
    private val BASE_STELLAR_COLOR_G = ((BASE_STELLAR_COLOR shr 8) and 0xFF) / 255.0f
    private val BASE_STELLAR_COLOR_B = (BASE_STELLAR_COLOR and 0xFF) / 255.0f
    private val provider = SceneRendering.register(GTLAdditions.id("subspace_hub_star"))

    fun enqueue(blockEntity: BlockEntity, tick: Double, starPos: Vec3, beamBasePos: Vec3) {
        val frame = SceneRendering.frame() ?: return
        val entry = Entry(
            blockEntity,
            AntichristRenderProfile(
                tick = tick,
                isWorking = true,
                facing = Direction.NORTH,
                renderMode = RenderMode.NORMAL,
                starPos = starPos,
                colorR = BASE_STELLAR_COLOR_R,
                colorG = BASE_STELLAR_COLOR_G,
                colorB = BASE_STELLAR_COLOR_B,
                starRadius = STAR_RADIUS,
                beamAlpha = BEAM_ALPHA,
                starRotationDegrees = (tick % 360.0).toFloat()
            ),
            AntichristBeamRenderer.VerticalTaperBeamProfile(
                tick = tick,
                basePos = beamBasePos,
                height = HEIGHT,
                bottomRadius = BOTTOM_RADIUS,
                topRadius = TOP_RADIUS,
                colorR = BASE_STELLAR_COLOR_R,
                colorG = BASE_STELLAR_COLOR_G,
                colorB = BASE_STELLAR_COLOR_B,
                beamAlpha = BEAM_ALPHA
            )
        )
        val pos = blockEntity.blockPos
        frame.submit(
            ScenePart.builder(PartKey.block(provider, pos, 0), Star, entry)
                .pass(PartPass.OPAQUE)
                .bounds(PartBounds.sphere(pos.x + starPos.x, pos.y + starPos.y, pos.z + starPos.z, STAR_RADIUS.toDouble()))
                .build()
        )
        frame.submit(
            ScenePart.builder(PartKey.block(provider, pos, 1), Beam, entry)
                .blend(PartBlend.STRAIGHT)
                .bounds(PartBounds.box(AntichristBeamRenderer.verticalTaperBounds(entry.beamProfile, pos)))
                .build()
        )
    }

    private class Entry(
        val blockEntity: BlockEntity,
        val starProfile: AntichristRenderProfile,
        val beamProfile: AntichristBeamRenderer.VerticalTaperBeamProfile
    )

    private object Star : PartRenderer<Entry> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Entry>>) {
            val frame = context.frame

            for (part in parts) {
                val pos = part.payload.blockEntity.blockPos
                context.poseStack.withPose {
                    translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
                    if (!AntichristStarRenderer.drawOriginalColor(part.payload.starProfile, this, STAR_TEXTURE)) {
                        context.report(part, PartOutcome.RESOURCE_UNAVAILABLE)
                    }
                }
            }
        }
    }

    private object Beam : PartRenderer<Entry> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Entry>>) {
            val frame = context.frame
            val camera = Vec3(frame.cameraX, frame.cameraY, frame.cameraZ)

            for (part in parts) {
                val pos = part.payload.blockEntity.blockPos
                context.poseStack.withPose {
                    translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
                    if (!AntichristBeamRenderer.drawVerticalTaper(part.payload.beamProfile, this, pos, camera)) {
                        context.report(part, PartOutcome.RESOURCE_UNAVAILABLE)
                    }
                }
            }
        }
    }
}
