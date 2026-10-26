package com.gtladd.gtladditions.client.render.machine.heart

import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
object HeartBlackHoleVisualConfig {
    // World-space radius for the full black-hole/accretion-disk projection.
    const val BLACK_HOLE_AND_DISK_RADIUS = 29.0f

    // Ratios relative to BLACK_HOLE_AND_DISK_RADIUS.
    // Space is almost solid up to SPACE_SOLID_RADIUS, then fades out at SPACE_FADE_RADIUS.
    const val SPACE_SOLID_RADIUS = 0.7f * 25.0f / 29.0f
    const val SPACE_FADE_RADIUS = 1.45f * 25.0f / 29.0f

    private const val DISTORTION_RADIUS_SCALE = 1.13f
    private const val HEARTBEAT_FREQUENCY = 0.14f
    private const val SPACE_HEARTBEAT_RADIUS_AMPLITUDE = 0.045f

    fun volumeProxyRadius(tick: Float): Float {
        val radius = max(BLACK_HOLE_AND_DISK_RADIUS, 0.001f)
        val pulse = sin(tick * HEARTBEAT_FREQUENCY)
        val distortionRadius = radius * DISTORTION_RADIUS_SCALE *
            (1.0f + pulse * pulse * SPACE_HEARTBEAT_RADIUS_AMPLITUDE)
        return max(distortionRadius, radius * max(SPACE_FADE_RADIUS, 0.001f))
    }

    // CPU-only conservative bounds for deferred pass culling.
    const val CPU_CULLING_MARGIN = 4.0f
    const val CPU_CULLING_RADIUS = BLACK_HOLE_AND_DISK_RADIUS * SPACE_FADE_RADIUS + CPU_CULLING_MARGIN

    // Matches Radiant Event Horizon's default ROTATION_SPEED parameter.
    const val RADIANT_ROTATION_SPEED = 0.3f

    const val BLACK_HOLE_ROTATION_MIN_SPEED = 0.010f
    const val BLACK_HOLE_ROTATION_MAX_SPEED = 0.024f
    const val BLACK_HOLE_VERTICAL_ROTATION_LIMIT = 0.5235987f
}

@OnlyIn(Dist.CLIENT)
data class HeartBlackHoleRenderProfile(
    val tick: Float,
    val center: Vec3,
    val facingX: Float,
    val facingY: Float,
    val facingZ: Float,
    val rotationSpeed: Float = HeartBlackHoleVisualConfig.RADIANT_ROTATION_SPEED
) {
    companion object {
        fun create(
            tick: Float,
            facing: Direction,
            center: Vec3,
            rotationSpeed: Float = HeartBlackHoleVisualConfig.RADIANT_ROTATION_SPEED
        ): HeartBlackHoleRenderProfile {
            val renderFacing = if (facing.axis.isHorizontal) facing else Direction.SOUTH
            val normal = renderFacing.normal
            return HeartBlackHoleRenderProfile(
                tick = tick,
                center = center,
                facingX = normal.x.toFloat(),
                facingY = normal.y.toFloat(),
                facingZ = normal.z.toFloat(),
                rotationSpeed = rotationSpeed
            )
        }
    }
}