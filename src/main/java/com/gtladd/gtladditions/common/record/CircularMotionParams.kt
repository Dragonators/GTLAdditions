package com.gtladd.gtladditions.common.record

import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

/**
 * Parameters for circular motion around a center point in a tilted plane
 *
 * @param centerPos     Center point of the circular motion
 * @param radius        Radius of the circular motion
 * @param speed         Angular speed in degrees per tick
 * @param angleOffset   Initial angle offset in degrees
 * @param tiltAngle     Tilt angle from horizontal plane in degrees (0 = horizontal, 90 = vertical)
 * @param tiltDirection Direction vector defining the axis of tilt (the "down" direction of the tilted plane)
 */
data class CircularMotionParams(
    @JvmField val centerPos: Vec3,
    @JvmField val radius: Float,
    @JvmField val speed: Float,
    @JvmField val angleOffset: Float = 0f,
    @JvmField val tiltAngle: Float = 0f,
    @JvmField val tiltDirection: Vector3f = Vector3f(1f, 0f, 0f)
) {
    fun getPosition(tick: Float): Vec3 {
        val angle = Math.toRadians((angleOffset + tick * speed).toDouble())

        val localX = radius * cos(angle)
        val localY = 0.0
        val localZ = radius * sin(angle)

        val worldPos = if (tiltAngle != 0f) {
            val tiltAxis = Vector3f(tiltDirection).normalize()
            val rotationQuat = Quaternionf().fromAxisAngleDeg(tiltAxis, tiltAngle)
            val point = Vector3f(localX.toFloat(), localY.toFloat(), localZ.toFloat())
            rotationQuat.transform(point)
            Vec3(point.x.toDouble(), point.y.toDouble(), point.z.toDouble())
        } else {
            Vec3(localX, localY, localZ)
        }

        return centerPos.add(worldPos)
    }

    fun getFacingAngle(tick: Float): Float {
        return (angleOffset + tick * speed) % 360f
    }
}
