package com.gtladd.gtladditions.client.render.machine.nexus

import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.sqrt

internal class NexusBillboardView(camera: Vector3dc, private val center: Vector3dc) {
    val normal: Vector3d = Vector3d(camera).sub(center)
    val distance = normal.length()
    val right: Vector3d
    val up: Vector3d

    init {
        if (distance > 1e-9) normal.div(distance) else normal.set(0.0, -1.0, 0.0)
        // The Nexus uses Z as world up. Keep this pole fallback identical to the sprite shaders.
        right = Vector3d(-normal.y, normal.x, 0.0)
        if (right.lengthSquared() > 1e-12) right.normalize() else right.set(1.0, 0.0, 0.0).fma(-normal.x, normal).normalize()
        up = Vector3d(normal).cross(right).normalize()
    }

    fun outside(radius: Double) = distance > radius + 1e-6

    fun limbCenter(radius: Double): Vector3d = Vector3d(center).fma(if (outside(radius)) radius * radius / distance else 0.0, normal)

    fun limbRadius(radius: Double): Double = if (outside(radius)) radius * sqrt(1 - (radius / distance) * (radius / distance)) else 0.0

    /** [lift] is measured from the centre toward the observer. */
    fun radiusAtLift(radius: Double, lift: Double): Double = if (outside(radius)) {
        maxOf(0.0, distance - lift) * radius / sqrt((distance - radius) * (distance + radius))
    } else {
        0.0
    }
}
