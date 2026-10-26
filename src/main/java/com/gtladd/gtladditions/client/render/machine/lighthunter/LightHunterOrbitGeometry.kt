package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.common.data.CircularMotionParams
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Coordinates relative to the star. */
object LightHunterOrbitGeometry {
    const val TUBE_RADIUS = 0.025f
    private val segments = intArrayOf(128, 64, 32)
    private val tubeSides = intArrayOf(6, 4, 4)

    fun detailLevel(distanceToCenter: Double, outerRadius: Float): Int = when {
        distanceToCenter - outerRadius < 64.0 -> 0
        distanceToCenter - outerRadius < 160.0 -> 1
        else -> 2
    }

    fun segments(level: Int): Int = segments[level]

    fun tubeSides(level: Int): Int = tubeSides[level]

    fun bounds(center: Vec3, radius: Double): AABB = AABB(
        center.x - radius,
        center.y - radius,
        center.z - radius,
        center.x + radius,
        center.y + radius,
        center.z + radius
    )

    fun write(consumer: VertexConsumer, orbits: List<CircularMotionParams>, level: Int) {
        val count = segments(level)
        val sides = tubeSides(level)
        val point = Vector3f()
        val previous = Vector3f()
        var firstStrip = true

        for (orbit in orbits) {
            val rotation = Quaternionf().fromAxisAngleDeg(Vector3f(orbit.tiltDirection).normalize(), orbit.tiltAngle)

            for (side in 0 until sides) {
                val crossAngle1 = 2.0 * PI * side / sides
                val crossAngle2 = if (side + 1 == sides) 0.0 else 2.0 * PI * (side + 1) / sides
                val radius1 = orbit.radius + TUBE_RADIUS * cos(crossAngle1).toFloat()
                val radius2 = orbit.radius + TUBE_RADIUS * cos(crossAngle2).toFloat()
                val height1 = TUBE_RADIUS * sin(crossAngle1).toFloat()
                val height2 = TUBE_RADIUS * sin(crossAngle2).toFloat()
                if (!firstStrip) {
                    // Disconnect longitudinal strips, including those belonging to different orbits.
                    vertex(consumer, previous)
                    rotation.transform(point.set(radius1, height1, 0f))
                    vertex(consumer, point)
                }

                for (segment in 0..count) {
                    // Both the orbit and tube cross-section close exactly at their seams.
                    val angle = if (segment == count) 0.0 else 2.0 * PI * segment / count
                    val x = cos(angle).toFloat()
                    val z = sin(angle).toFloat()
                    rotation.transform(point.set(radius1 * x, height1, radius1 * z))
                    vertex(consumer, point)
                    rotation.transform(point.set(radius2 * x, height2, radius2 * z))
                    vertex(consumer, point)
                }

                previous.set(point)
                firstStrip = false
            }
        }
    }

    private fun vertex(consumer: VertexConsumer, point: Vector3f) {
        consumer.vertex(point.x.toDouble(), point.y.toDouble(), point.z.toDouble())
            .color(255, 255, 255, 200)
            .endVertex()
    }
}