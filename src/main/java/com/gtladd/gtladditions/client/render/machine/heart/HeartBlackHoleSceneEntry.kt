package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/** Black-hole-specific visibility stays with its geometry, outside the scene coordinator. */
@OnlyIn(Dist.CLIENT)
internal data class HeartBlackHoleSceneEntry(
    override val blockEntity: BlockEntity,
    override val profile: HeartBlackHoleRenderProfile
) : HeartBlackHoleRenderEntry {
    fun isVisible(context: DeferredRenderContext): Boolean {
        if (blockEntity.isRemoved) return false

        val worldCenter = worldCenter()
        val cullingRadius = HeartBlackHoleVisualConfig.CPU_CULLING_RADIUS.toDouble()
        val maxDistance = VIEW_DISTANCE + cullingRadius
        if (context.cameraPosition.distanceToSqr(worldCenter) > maxDistance * maxDistance) return false

        val frustum = context.frustum ?: return true
        val cullingBounds = AABB.ofSize(worldCenter, cullingRadius * 2.0, cullingRadius * 2.0, cullingRadius * 2.0)
        return frustum.isVisible(cullingBounds)
    }

    private fun worldCenter(): Vec3 {
        val blockPos = blockEntity.blockPos
        return Vec3(
            blockPos.x + profile.center.x,
            blockPos.y + profile.center.y,
            blockPos.z + profile.center.z
        )
    }

    private companion object {
        const val VIEW_DISTANCE = 384.0
    }
}