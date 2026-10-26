package com.gtladd.gtladditions.client.render.machine.heart

import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.sqrt

/** Orders disjoint convex bounds only where their screen projections can meet. */
internal object HeartSceneOrder {
    private const val EPSILON = 0.0001

    data class Bounds(val box: AABB, val sphereCenter: Vec3? = null, val radius: Double = 0.0)

    /** Returns far-to-near indices, or null when conservative bounds cannot establish an order. */
    fun sort(bounds: List<Bounds>, pose: Matrix4f, projection: Matrix4f, camera: Vec3, width: Int, height: Int): IntArray? {
        val count = bounds.size
        val inverse = Matrix4f(pose).invert()
        val origin = inverse.transformPosition(Vector3f())
        val eye = camera.add(origin.x.toDouble(), origin.y.toDouble(), origin.z.toDouble())
        val forward = inverse.transformDirection(Vector3f(0f, 0f, -1f)).normalize()
        val direction = if (abs(projection.m33()) < EPSILON) null else Vec3(forward.x.toDouble(), forward.y.toDouble(), forward.z.toDouble())
        val vp = Matrix4f(projection).mul(pose)
        val rects = bounds.map { HeartSceneCompositor.screenBounds(vp, it.box.move(-camera.x, -camera.y, -camera.z), width, height, 32) }
        val next = Array(count) { ArrayList<Int>() }
        val incoming = IntArray(count)
        for (a in 0 until count) {
            for (b in a + 1 until count) {
                if (!overlap(rects[a], rects[b])) continue
                val relation = relation(bounds[a], bounds[b], eye, direction)
                    ?: depthRelation(bounds[a].box, bounds[b].box, pose, camera)
                    ?: return null
                if (relation == 0) continue
                val far = if (relation > 0) b else a
                val near = if (relation > 0) a else b
                next[far].add(near)
                incoming[near]++
            }
        }
        val result = IntArray(count)
        val visited = BooleanArray(count)
        for (position in result.indices) {
            val index = incoming.indices.firstOrNull { !visited[it] && incoming[it] == 0 } ?: return null
            result[position] = index
            visited[index] = true
            next[index].forEach { incoming[it]-- }
        }
        return result
    }

    private fun overlap(a: IntArray, b: IntArray): Boolean =
        a[2] > 0 &&
            a[3] > 0 &&
            b[2] > 0 &&
            b[3] > 0 &&
            a[0] < b[0] + b[2] &&
            b[0] < a[0] + a[2] &&
            a[1] < b[1] + b[3] &&
            b[1] < a[1] + a[3]

    /** 1 means A is nearer, -1 means B is nearer, 0 means no ray can encounter both. */
    private fun relation(a: Bounds, b: Bounds, eye: Vec3, direction: Vec3?): Int? {
        val ac = a.sphereCenter
        val bc = b.sphereCenter
        if (ac != null && bc != null) {
            return plane(ac, bc, a.radius, b.radius, eye, direction)
        }
        if (ac != null) {
            val closest = Vec3(ac.x.coerceIn(b.box.minX, b.box.maxX), ac.y.coerceIn(b.box.minY, b.box.maxY), ac.z.coerceIn(b.box.minZ, b.box.maxZ))
            return plane(ac, closest, a.radius, 0.0, eye, direction)
        }
        if (bc != null) {
            val closest = Vec3(bc.x.coerceIn(a.box.minX, a.box.maxX), bc.y.coerceIn(a.box.minY, a.box.maxY), bc.z.coerceIn(a.box.minZ, a.box.maxZ))
            return plane(bc, closest, b.radius, 0.0, eye, direction)?.let { -it }
        }
        var order: Int? = null
        for (axis in 0..2) {
            val aMin = minimum(a.box, axis)
            val aMax = maximum(a.box, axis)
            val bMin = minimum(b.box, axis)
            val bMax = maximum(b.box, axis)
            val eyeAxis = coordinate(eye, axis)
            val directionAxis = direction?.let { coordinate(it, axis) }
            val candidate = when {
                aMax + EPSILON < bMin -> separated(aMax, bMin, eyeAxis, directionAxis)
                bMax + EPSILON < aMin -> -separated(bMax, aMin, eyeAxis, directionAxis)
                else -> continue
            }
            if (candidate == 0 || order != null && order != candidate) return 0
            order = candidate
        }
        return order
    }

    private fun plane(a: Vec3, b: Vec3, aRadius: Double, bRadius: Double, eye: Vec3, direction: Vec3?): Int? {
        val delta = b.subtract(a)
        val distance = sqrt(delta.lengthSqr())
        if (distance <= aRadius + bRadius + EPSILON) return null
        val normal = delta.scale(1.0 / distance)
        return separated(aRadius, distance - bRadius, eye.subtract(a).dot(normal), direction?.dot(normal))
    }

    private fun separated(aMax: Double, bMin: Double, eye: Double, direction: Double?): Int =
        if (direction != null) {
            when {
                direction > EPSILON -> 1
                direction < -EPSILON -> -1
                else -> 0
            }
        } else {
            when {
                eye <= aMax -> 1
                eye >= bMin -> -1
                else -> 0
            }
        }

    private fun depthRelation(a: AABB, b: AABB, pose: Matrix4f, camera: Vec3): Int? {
        val first = depths(a, pose, camera)
        val second = depths(b, pose, camera)
        return when {
            first[1] + EPSILON < second[0] -> 1
            second[1] + EPSILON < first[0] -> -1
            else -> null
        }
    }

    private fun depths(box: AABB, pose: Matrix4f, camera: Vec3): DoubleArray {
        var near = Double.POSITIVE_INFINITY
        var far = Double.NEGATIVE_INFINITY
        val point = Vector3f()
        for (corner in 0..7) {
            pose.transformPosition(
                point.set(
                    ((if (corner and 1 == 0) box.minX else box.maxX) - camera.x).toFloat(),
                    ((if (corner and 2 == 0) box.minY else box.maxY) - camera.y).toFloat(),
                    ((if (corner and 4 == 0) box.minZ else box.maxZ) - camera.z).toFloat()
                )
            )
            near = minOf(near, -point.z.toDouble())
            far = maxOf(far, -point.z.toDouble())
        }
        return doubleArrayOf(near, far)
    }

    private fun coordinate(point: Vec3, axis: Int): Double = when (axis) {
        0 -> point.x
        1 -> point.y
        else -> point.z
    }
    private fun minimum(box: AABB, axis: Int): Double = when (axis) {
        0 -> box.minX
        1 -> box.minY
        else -> box.minZ
    }
    private fun maximum(box: AABB, axis: Int): Double = when (axis) {
        0 -> box.maxX
        1 -> box.maxY
        else -> box.maxZ
    }
}