package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.utils.antichrist.RingStructureVertexBuffer
import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.minecraft.world.level.block.entity.BlockEntity
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
    private val ringEntries = Long2ObjectLinkedOpenHashMap<Entry>()

    fun enqueue(blockEntity: BlockEntity, profile: AntichristRenderProfile) {
        DeferredMachineRenderer.enqueue(Entry(blockEntity, profile))
    }

    fun enqueueRing(blockEntity: BlockEntity, profile: AntichristRenderProfile) {
        ringEntries.put(blockEntity.blockPos.asLong(), Entry(blockEntity, profile))
    }

    @SubscribeEvent
    @JvmStatic
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || ringEntries.isEmpty()) return

        val cameraPosition = event.camera.position
        ringEntries.values.forEach { entry ->
            renderRingEntryRelativeToCamera(entry, cameraPosition, event.poseStack)
        }
        ringEntries.clear()
    }

    private fun renderRingEntryRelativeToCamera(
        entry: Entry,
        cameraPosition: Vec3,
        poseStack: PoseStack
    ) {
        val blockEntity = entry.blockEntity
        if (blockEntity.isRemoved) return

        poseStack.withPose {
            translate(
                blockEntity.blockPos.x - cameraPosition.x,
                blockEntity.blockPos.y - cameraPosition.y,
                blockEntity.blockPos.z - cameraPosition.z
            )
            RingStructureVertexBuffer.renderTerrainBatched(entry.profile, this)
        }
    }

    private fun render(
        profile: AntichristRenderProfile,
        poseStack: PoseStack,
        blockEntity: BlockEntity
    ) {
        AntichristStarRenderer.renderOpaque(profile, poseStack)
        AntichristStarRenderer.renderTransparent(profile, poseStack)
        AntichristBeamRenderer.render(profile, poseStack, blockEntity)
    }

    private data class Entry(
        override val blockEntity: BlockEntity,
        val profile: AntichristRenderProfile
    ) : DeferredRenderEntry {
        override fun render(context: DeferredRenderContext) {
            context.renderRelativeToCamera(blockEntity) {
                render(profile, this, blockEntity)
            }
        }
    }
}