package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object HeartDeferredRenderer {
    private const val RENDER_ORDER = 1
    private const val HEART_RENDERER_VIEW_DISTANCE = 384.0

    fun enqueue(blockEntity: BlockEntity, profile: HeartBlackHoleRenderProfile) {
        DeferredMachineRenderer.enqueue(Entry(blockEntity, profile))
    }

    private data class Entry(
        override val blockEntity: BlockEntity,
        override val profile: HeartBlackHoleRenderProfile
    ) : DeferredRenderEntry,
        HeartBlackHoleRenderEntry {
        override val renderOrder: Int = RENDER_ORDER
        override val batchKey: Any = HeartDeferredRenderer

        override fun isVisible(context: DeferredRenderContext): Boolean {
            if (blockEntity.isRemoved) return false

            val worldCenter = worldCenter()
            val cullingRadius = HeartBlackHoleVisualConfig.CPU_CULLING_RADIUS.toDouble()
            val maxDistance = HEART_RENDERER_VIEW_DISTANCE + cullingRadius
            if (context.cameraPosition.distanceToSqr(worldCenter) > maxDistance * maxDistance) return false

            val frustum = context.frustum ?: return true
            val cullingBounds = AABB.ofSize(worldCenter, cullingRadius * 2.0, cullingRadius * 2.0, cullingRadius * 2.0)
            return frustum.isVisible(cullingBounds)
        }

        override fun render(context: DeferredRenderContext) {
            HeartBlackHoleRenderer.render(listOf(this), context.poseStack, context.cameraPosition)
        }

        override fun renderBatch(context: DeferredRenderContext, entries: List<DeferredRenderEntry>) {
            val heartEntries = entries.mapNotNull { entry -> entry as? Entry }
            if (heartEntries.isNotEmpty()) {
                HeartBlackHoleRenderer.render(heartEntries, context.poseStack, context.cameraPosition)
            }
        }

        private fun worldCenter(): Vec3 {
            val blockPos = blockEntity.blockPos
            return Vec3(
                blockPos.x + profile.center.x,
                blockPos.y + profile.center.y,
                blockPos.z + profile.center.z
            )
        }
    }
}