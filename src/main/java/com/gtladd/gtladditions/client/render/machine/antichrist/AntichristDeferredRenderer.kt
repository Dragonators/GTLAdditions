package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import net.minecraft.world.level.block.entity.BlockEntity
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
    private val translucentDraws = ArrayList<RingTransparency.Instance>()

    fun enqueue(blockEntity: BlockEntity, profile: AntichristRenderProfile, renderRings: Boolean, renderEffects: Boolean) {
        val entry = Entry(blockEntity, profile)
        if (renderRings) ringEntries.put(blockEntity.blockPos.asLong(), entry)
        if (renderEffects) DeferredMachineRenderer.enqueue(entry)
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

        val context = DeferredRenderContext(event.camera.position, event.poseStack, event.frustum)
        try {
            ringEntries.values.forEach { entry ->
                renderRings(entry, context, translucent)
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
        context: DeferredRenderContext,
        translucent: Boolean
    ) {
        context.renderRelativeToCamera(entry.blockEntity) {
            if (translucent) {
                RingStructureVertexBuffer.collectTranslucent(entry.profile, this, translucentDraws)
            } else {
                RingStructureVertexBuffer.renderTerrainBatched(entry.profile, this)
            }
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