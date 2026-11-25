package com.gtladd.gtladditions.client.render.machine.antichrist

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
import com.gtladd.gtladditions.client.render.withPose
import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
    modid = GTLAdditions.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = [Dist.CLIENT]
)
object AntichristDeferredRenderer {
    private val provider = SceneRendering.register(GTLAdditions.id("antichrist_star"))

    // The outer ring reaches 63.5 blocks along its axis and 63.55 radially when tilted.
    private val ringBounds = AABB(-64.0, -64.0, -64.0, 64.0, 64.0, 64.0)
    private val ringEntries = Long2ObjectLinkedOpenHashMap<Entry>()
    private val translucentDraws = ArrayList<RingTransparency.Instance>()

    fun enqueue(blockEntity: BlockEntity, profile: AntichristRenderProfile, renderRings: Boolean, renderEffects: Boolean) {
        val entry = Entry(blockEntity, profile)
        if (renderRings) ringEntries.put(blockEntity.blockPos.asLong(), entry)
        if (!renderEffects) return
        val frame = SceneRendering.frame() ?: return
        val pos = blockEntity.blockPos
        val star = profile.starPos
        frame.submit(
            ScenePart.builder(PartKey.block(provider, pos, 0), StarSurface, entry)
                .pass(PartPass.OPAQUE)
                .bounds(PartBounds.sphere(pos.x + star.x, pos.y + star.y, pos.z + star.z, profile.starRadius.toDouble()))
                .build()
        )
        frame.submit(
            ScenePart.builder(PartKey.block(provider, pos, 1), Glow, entry)
                .blend(PartBlend.STRAIGHT)
                .bounds(PartBounds.sphere(pos.x + star.x, pos.y + star.y, pos.z + star.z, profile.starRadius * 1.04))
                .build()
        )
        if (profile.shouldRenderBeam) {
            frame.submit(
                ScenePart.builder(PartKey.block(provider, pos, 2), Beam, entry)
                    .blend(PartBlend.STRAIGHT)
                    .bounds(PartBounds.box(AntichristBeamRenderer.bounds(profile, pos)))
                    .build()
            )
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage == RenderLevelStageEvent.Stage.AFTER_SKY || event.stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            ringEntries.clear()
            return
        }

        val translucent = when (event.stage) {
            RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES -> false
            RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS -> true
            else -> return
        }

        if (ringEntries.isEmpty()) return

        val camera = event.camera.position
        try {
            ringEntries.values.forEach { entry ->
                renderRings(entry, event.poseStack, camera, event.frustum, translucent)
            }
            // Oculus has now copied opaque depth and finished deferred. All FOA solids are present.
            if (translucent) RingTransparency.render(translucentDraws)
        } finally {
            if (translucent) {
                ringEntries.clear()
                translucentDraws.clear()
            }
        }
    }

    private fun renderRings(
        entry: Entry,
        poseStack: PoseStack,
        camera: Vec3,
        frustum: Frustum,
        translucent: Boolean
    ) {
        val blockEntity = entry.blockEntity
        if (blockEntity.isRemoved) return
        val pos = blockEntity.blockPos
        val star = entry.profile.starPos
        if (!frustum.isVisible(ringBounds.move(pos.x + star.x, pos.y + star.y, pos.z + star.z))) return
        poseStack.withPose {
            translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z)
            if (translucent) {
                RingStructureVertexBuffer.collectTranslucent(entry.profile, this, translucentDraws)
            } else {
                RingStructureVertexBuffer.renderTerrainBatched(entry.profile, this)
            }
        }
    }

    /** False from [draw] reports the missing program. */
    private inline fun drawEach(context: PartDrawContext, parts: List<ScenePart<Entry>>, draw: (Entry, PoseStack, Vec3) -> Boolean) {
        val frame = context.frame
        val camera = Vec3(frame.cameraX, frame.cameraY, frame.cameraZ)

        for (part in parts) {
            val pos = part.payload.blockEntity.blockPos
            context.poseStack.withPose {
                translate(pos.x - frame.cameraX, pos.y - frame.cameraY, pos.z - frame.cameraZ)
                if (!draw(part.payload, this, camera)) context.report(part, PartOutcome.RESOURCE_UNAVAILABLE)
            }
        }
    }

    private object StarSurface : PartRenderer<Entry> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Entry>>) =
            drawEach(context, parts) { entry, pose, _ -> AntichristStarRenderer.drawOpaque(entry.profile, pose) }
    }

    private object Glow : PartRenderer<Entry> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Entry>>) =
            drawEach(context, parts) { entry, pose, _ -> AntichristStarRenderer.drawTransparent(entry.profile, pose) }
    }

    private object Beam : PartRenderer<Entry> {
        override fun draw(context: PartDrawContext, parts: List<ScenePart<Entry>>) =
            drawEach(context, parts) { entry, pose, camera ->
                AntichristBeamRenderer.draw(entry.profile, pose, entry.blockEntity.blockPos, camera)
            }
    }

    private data class Entry(val blockEntity: BlockEntity, val profile: AntichristRenderProfile)
}
