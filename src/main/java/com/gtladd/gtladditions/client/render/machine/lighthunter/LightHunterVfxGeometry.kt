package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** Controller-centred, EAST-facing coordinates, in blocks. */
object LightHunterVfxGeometry {
    // Source pattern: [aisle][row][column], controller [188][36][36], LEFT/UP/FRONT.
    // EAST-facing block centres are (aisle - 188, row - 36, 36 - column).
    // All five glass faces are at aisle 8; the small openings are 13 blocks off-axis.
    const val NOZZLE_X = -180.55f

    // Keep equal to PLASMA_MAIN_LENGTH in light_hunter_plasma.glsl.
    const val MAIN_LENGTH = 136f
    const val AUX_LENGTH = 25f
    const val AUX_OFFSET = 13f
    const val MAIN_RADIUS_INCREASE = 0.75f

    // Bounds only: the shader integrates a world-space light field inside each box.
    const val RING_BOUNDS_RADIUS = 1.8f
    const val CHART_X = -18f // centre of the large ring: aisle 170
    const val CHART_RADIUS = 28.5f
    const val SHIELD_TOP_GAP = 5f
    const val SHIELD_FOLD_HEIGHT = 6f
    const val LAYERS = 7
    const val PLUMES = 0
    const val RINGS = 1
    const val SHIELDS = 2
    const val CHART_LINES = 3
    const val CHART_STARS = 4
    const val ROOTS = 5
    const val CHART_FIELD = 6
    const val ALL_LAYERS = 127
    const val CHART_LAYERS = 88
    private const val TAU = (PI * 2).toFloat()
    val nozzleCenters: List<Vector3f>
        get() = listOf(
            Vector3f(NOZZLE_X, 0f, 0f),
            Vector3f(NOZZLE_X, AUX_OFFSET, 0f),
            Vector3f(NOZZLE_X, -AUX_OFFSET, 0f),
            Vector3f(NOZZLE_X, 0f, AUX_OFFSET),
            Vector3f(NOZZLE_X, 0f, -AUX_OFFSET)
        )

    fun transform(facing: Direction): Matrix4f {
        val angle = when (facing) {
            Direction.EAST -> 0f
            Direction.SOUTH -> -PI.toFloat() / 2
            Direction.WEST -> PI.toFloat()
            Direction.NORTH -> PI.toFloat() / 2
            else -> error("Light Hunter requires a horizontal facing")
        }
        return Matrix4f().translation(0.5f, 0.5f, 0.5f).rotateY(angle)
    }

    fun bounds(facing: Direction): AABB = bounds(facing, NOZZLE_X - MAIN_LENGTH - 2, 7f, 38f)

    fun bounds(facing: Direction, section: Section): AABB = bounds(facing, section.minX, section.maxX, section.extent)

    private fun bounds(facing: Direction, minX: Float, maxX: Float, extent: Float): AABB {
        val matrix = transform(facing)
        val a = matrix.transformPosition(Vector3f(minX, -extent, -extent))
        val b = matrix.transformPosition(Vector3f(maxX, extent, extent))
        return AABB(a.x.toDouble(), a.y.toDouble(), a.z.toDouble(), b.x.toDouble(), b.y.toDouble(), b.z.toDouble())
    }

    /** [extent] is the y/z half extent. */
    enum class Section(val layers: Int, val minX: Float, val maxX: Float, val extent: Float) {
        // The chart plane also carries the fusion volume and the paradox window.
        FRONT(CHART_LAYERS, LightHunterFusionGeometry.MIN_X, LightHunterFusionGeometry.MAX_X, LightHunterFusionGeometry.EXTENT),

        SHIELD(1 shl SHIELDS, -164f, -32f, 34f),

        EXHAUST((1 shl PLUMES) or (1 shl RINGS) or (1 shl ROOTS), NOZZLE_X - MAIN_LENGTH - 2, NOZZLE_X + 0.05f, 15f)
    }

    fun write(out: VertexConsumer, layer: Int) {
        when (layer) {
            PLUMES -> nozzleCenters.forEachIndexed { index, center -> plume(out, center, index == 0) }
            RINGS -> for (i in 0..2) ring(out, NOZZLE_X - floatArrayOf(16f, 29.2f, 46.25f)[i], 6f + i * 3.5f)
            SHIELDS -> shields(out)
            CHART_LINES -> chartLines(out)
            CHART_FIELD -> chartField(out)
            ROOTS -> nozzleCenters.forEachIndexed { i, p ->
                val radius = if (i == 0) 3.35f + MAIN_RADIUS_INCREASE else 1.40f
                quad(
                    out,
                    Vector3f(p.x - 0.02f, p.y - radius, p.z - radius),
                    Vector3f(p.x - 0.02f, p.y + radius, p.z - radius),
                    Vector3f(p.x - 0.02f, p.y + radius, p.z + radius),
                    Vector3f(p.x - 0.02f, p.y - radius, p.z + radius),
                    floatArrayOf(0.4f, 0.8f, 1f, if (i == 0) 1f else 0.70f)
                )
            }
            CHART_STARS -> stars.forEachIndexed { i, p ->
                val size = if (i == TARGET_STAR) {
                    1.35f
                } else if (i % 5 == 0) {
                    1.0f
                } else {
                    0.65f
                }

                quad(
                    out,
                    Vector3f(CHART_X, p.x - size, p.y - size),
                    Vector3f(CHART_X, p.x + size, p.y - size),
                    Vector3f(CHART_X, p.x + size, p.y + size),
                    Vector3f(CHART_X, p.x - size, p.y + size),
                    if (i == TARGET_STAR) {
                        floatArrayOf(0.28f, 0.94f, 1f, 1f)
                    } else {
                        floatArrayOf(0.48f + (i % 3) * 0.12f, 0.75f, 1f, 0.94f)
                    }
                )
            }
        }
    }

    private fun plume(out: VertexConsumer, center: Vector3f, main: Boolean) {
        val length = if (main) MAIN_LENGTH else AUX_LENGTH
        val sides = if (main) 64 else 32
        val axials = if (main) 32 else 1
        // One closed convex proxy per nozzle. The fragment shader integrates its interior;
        // drawing several translucent shells would accumulate the same volume repeatedly.
        val color = floatArrayOf(0.62f, 0.22f, 1f, if (main) 0.38f else 0.29f)
        fun point(u: Float, v: Float): Vector3f {
            val radius = if (main) 2.48f + MAIN_RADIUS_INCREASE + 9f * (1f - exp(-u * 8f)) else maxOf(0.02f, 1.15f * (1f - u))
            return Vector3f(center.x - length * u, center.y + cos(v * TAU) * radius, center.z + sin(v * TAU) * radius)
        }

        fun emit(p: Vector3f) = vertex(out, p, 0f, 0f, color)
        for (axial in 0 until axials) {
            val u0 = axial.toFloat() / axials
            val u1 = (axial + 1f) / axials

            for (side in 0 until sides) {
                val v0 = side.toFloat() / sides
                val v1 = (side + 1f) / sides
                val p0 = point(u0, v0)
                val p1 = point(u1, v0)
                val p2 = point(u1, v1)
                val p3 = point(u0, v1)
                emit(p0)
                emit(p1)
                emit(p2)
                emit(p3)
                // Degenerate quads keep the shared QUADS format while closing each cap.
                if (axial == 0) {
                    emit(center)
                    emit(p0)
                    emit(p3)
                    emit(p3)
                }

                if (axial == axials - 1) {
                    emit(Vector3f(center.x - length, center.y, center.z))
                    emit(p2)
                    emit(p1)
                    emit(p1)
                }
            }
        }
    }

    private fun ring(out: VertexConsumer, x: Float, radius: Float) {
        val color = floatArrayOf(0.62f, 0.25f, 1f, 0.90f)
        val extent = radius + RING_BOUNDS_RADIUS
        val points = arrayOf(
            Vector3f(x - RING_BOUNDS_RADIUS, -extent, -extent),
            Vector3f(x + RING_BOUNDS_RADIUS, -extent, -extent),
            Vector3f(x - RING_BOUNDS_RADIUS, extent, -extent),
            Vector3f(x + RING_BOUNDS_RADIUS, extent, -extent),
            Vector3f(x - RING_BOUNDS_RADIUS, -extent, extent),
            Vector3f(x + RING_BOUNDS_RADIUS, -extent, extent),
            Vector3f(x - RING_BOUNDS_RADIUS, extent, extent),
            Vector3f(x + RING_BOUNDS_RADIUS, extent, extent)
        )

        // A convex box supplies one exit surface per ray, including when the camera is inside.
        for (face in arrayOf(
            intArrayOf(0, 4, 6, 2),
            intArrayOf(1, 3, 7, 5),
            intArrayOf(0, 1, 5, 4),
            intArrayOf(2, 6, 7, 3),
            intArrayOf(0, 2, 3, 1),
            intArrayOf(4, 5, 7, 6)
        )) {
            for (index in face) vertex(out, points[index], x, radius, color)
        }
    }

    private fun shields(out: VertexConsumer) {
        for (side in floatArrayOf(-1f, 1f)) {
            shieldFlank(out, side, -32f, -112f, 16f, 21f, 20.5f, 25.5f, false)
            shieldFlank(out, side, -105f, -164f, 21f, 17f, 25.5f, 18.5f, true)
        }

        val stations = arrayOf(
            floatArrayOf(-39f, -80.5f, 23f, 28f, 14f, 23f),
            floatArrayOf(-81f, -131.5f, 28f, 25f, 23f, 21f),
            floatArrayOf(-128f, -164f, 25f, 19f, 21f, 12f)
        )
        for (vertical in floatArrayOf(-1f, 1f)) {
            for ((index, p) in stations.withIndex()) {
                shieldTop(out, vertical, p, index, stations[1])
            }
        }
    }

    private fun shieldTop(out: VertexConsumer, vertical: Float, station: FloatArray, panel: Int, middle: FloatArray) {
        // UV [6,7] selects the top plate's longer nose and deeper rear notch in the shared shader.
        val outline = arrayOf(
            floatArrayOf(0f, 0.5f),
            floatArrayOf(0.28f, 0f),
            floatArrayOf(1f, 0f),
            floatArrayOf(0.76f, 0.5f),
            floatArrayOf(1f, 1f),
            floatArrayOf(0.28f, 1f)
        )
        val triangles = arrayOf(intArrayOf(3, 0, 1), intArrayOf(3, 1, 2), intArrayOf(3, 4, 5), intArrayOf(3, 5, 0))
        val color = floatArrayOf(0.30f, 0.40f, 1f, 0.66f)
        fun point(profile: FloatArray, index: Int): Vector3f {
            val (u, v) = outline[index]
            val width = profile[5] + (profile[4] - profile[5]) * u
            return Vector3f(
                profile[1] + (profile[0] - profile[1]) * u,
                vertical * (profile[3] + (profile[2] - profile[3]) * u),
                (1f - 2f * v) * width
            )
        }

        fun emit(index: Int) {
            val (u, v) = outline[index]
            val p = when {
                panel == 0 && index in intArrayOf(0, 1, 5) -> {
                    val matching = when (index) {
                        0 -> 3
                        1 -> 2
                        else -> 4
                    }
                    point(middle, matching).add(SHIELD_TOP_GAP, 0f, 0f)
                }
                panel == 2 && index in intArrayOf(2, 3, 4) -> {
                    val matching = when (index) {
                        2 -> 1
                        3 -> 0
                        else -> 5
                    }
                    point(middle, matching).add(-SHIELD_TOP_GAP, 0f, 0f)
                }
                else -> point(station, index)
            }

            // Lift the shared 0-to-3 ridge only after matching the joint edges, so the translated V joints keep their
            // spacing.
            if (index == 0 || index == 3) p.y += vertical * SHIELD_FOLD_HEIGHT
            vertex(out, p, u + 6f, v, color)
        }

        for (triangle in triangles) {
            triangle.forEach(::emit)
            emit(triangle[2])
        }
    }

    private fun shieldFlank(
        out: VertexConsumer,
        side: Float,
        near: Float,
        far: Float,
        nearHeight: Float,
        farHeight: Float,
        nearRadius: Float,
        farRadius: Float,
        pointedNose: Boolean
    ) {
        // UV x: [0,1] dorsal quad, [2,3] broad forward plate, [4,5] pointed aft plate.
        val shoulder = if (pointedNose) 0.12f else 0f
        val outline = arrayOf(
            floatArrayOf(0f, 0.5f),
            floatArrayOf(shoulder, 0f),
            floatArrayOf(1f, 0f),
            floatArrayOf(0.80f, 0.5f),
            floatArrayOf(1f, 1f),
            floatArrayOf(shoulder, 1f)
        )
        val triangles = arrayOf(intArrayOf(3, 0, 1), intArrayOf(3, 1, 2), intArrayOf(3, 4, 5), intArrayOf(3, 5, 0))
        val color = floatArrayOf(0.30f, 0.40f, 1f, 0.66f)
        fun emit(index: Int) {
            val (u, v) = outline[index]
            val height = nearHeight + (farHeight - nearHeight) * u
            val p = Vector3f(
                near + (far - near) * u,
                (1f - 2f * v) * height,
                side * (nearRadius + (farRadius - nearRadius) * u)
            )
            if (pointedNose) {
                // The forward plate's three-point V edge translated 9 blocks aft, so the joint stays parallel.
                when (index) {
                    0 -> p.set(-105f, 0f, side * 24.5f)
                    1 -> p.set(-121f, 21f, side * 25.5f)
                    3 -> p.x -= 4f // shallower outer V cut, shifted toward the exhaust
                    5 -> p.set(-121f, -21f, side * 25.5f)
                }
            }

            if (index == 0 || index == 3) p.z += side * SHIELD_FOLD_HEIGHT
            vertex(out, p, u + if (pointedNose) 4f else 2f, v, color)
        }

        // One degenerate quad per triangle keeps the shared QUADS batch without filling the notch.
        for (triangle in triangles) {
            triangle.forEach(::emit)
            emit(triangle[2])
        }
    }

    private const val TARGET_STAR = 22
    internal val stars = listOf(
        Vector2f(25f, 7f), Vector2f(23f, 1f), Vector2f(24f, -6f), Vector2f(17f, -10f), Vector2f(19f, -16f), Vector2f(9f, -19f),
        Vector2f(20f, 17f), Vector2f(13f, 22f), Vector2f(7f, 18f), Vector2f(-1f, 23f), Vector2f(-9f, 20f), Vector2f(-13f, 23f),
        Vector2f(-19f, 18f), Vector2f(-24f, 10f), Vector2f(-20f, 3f), Vector2f(-26f, -3f), Vector2f(-18f, -9f), Vector2f(-19f, -17f),
        Vector2f(11f, -24f), Vector2f(3f, -23f), Vector2f(-4f, -18f), Vector2f(-12f, -23f),
        Vector2f(9f, 22f), Vector2f(12f, 5f), Vector2f(8f, 9f), Vector2f(2f, 14f), Vector2f(-6f, 11f), Vector2f(-13f, 6f),
        Vector2f(19f, 3f), Vector2f(8f, -9f), Vector2f(-2f, -8f), Vector2f(-9f, -3f),
        Vector2f(-17f, 12f), Vector2f(2f, 4f), Vector2f(26f, -10f), Vector2f(-4f, 27f)
    )

    private fun chartField(out: VertexConsumer) {
        val color = floatArrayOf(1f, 1f, 1f, 1f)

        for (i in 0 until 192) {
            val a = i * TAU / 192
            val b = (i + 1) * TAU / 192
            val first = Vector3f(CHART_X, cos(a) * CHART_RADIUS, sin(a) * CHART_RADIUS)
            val second = Vector3f(CHART_X, cos(b) * CHART_RADIUS, sin(b) * CHART_RADIUS)
            vertex(out, Vector3f(CHART_X, 0f, 0f), 0.5f, 0.5f, color)
            vertex(out, first, 0.5f + cos(a) * 0.5f, 0.5f + sin(a) * 0.5f, color)
            vertex(out, second, 0.5f + cos(b) * 0.5f, 0.5f + sin(b) * 0.5f, color)
            vertex(out, second, 0.5f + cos(b) * 0.5f, 0.5f + sin(b) * 0.5f, color)
        }
    }

    private fun chartLines(out: VertexConsumer) {
        val chains = arrayOf(
            intArrayOf(0, 1, 2, 3, 4, 5),
            intArrayOf(6, 7, 8, 9, 10, 11),
            intArrayOf(12, 13, 14, 15, 16, 17),
            intArrayOf(18, 19, 20, 21),
            intArrayOf(23, 24, 25, 26, 27)
        )

        for (chain in chains) {
            for (i in 0 until chain.lastIndex) {
                val start = stars[chain[i]]
                val end = stars[chain[i + 1]]
                line(out, start.x, start.y, end.x, end.y, 0.13f, 0.78f)
            }
        }

        for (radius in floatArrayOf(CHART_RADIUS, 23.8f)) {
            for (i in 0 until 144) {
                if (radius < 28 && i % 36 > 17) continue
                val a = i * TAU / 144
                val b = (i + 1) * TAU / 144
                line(out, cos(a) * radius, sin(a) * radius, cos(b) * radius, sin(b) * radius, 0.10f, 0.62f)
            }
        }

        for (i in 0 until 72) {
            val a = i * TAU / 72
            val r = CHART_RADIUS - if (i % 6 == 0) 1.1f else 0.5f
            line(out, cos(a) * r, sin(a) * r, cos(a) * CHART_RADIUS, sin(a) * CHART_RADIUS, 0.09f, 0.8f)
        }

        val target = stars[TARGET_STAR]
        for (i in 0 until 48) {
            if (i % 12 > 8) continue
            val a = i * TAU / 48
            val b = (i + 1) * TAU / 48
            line(
                out,
                target.x + cos(a) * 2.4f,
                target.y + sin(a) * 2.4f,
                target.x + cos(b) * 2.4f,
                target.y + sin(b) * 2.4f,
                0.14f,
                0.95f
            )
        }
    }

    private fun line(out: VertexConsumer, y0: Float, z0: Float, y1: Float, z1: Float, width: Float, alpha: Float) {
        val normal = Vector3f(0f, z0 - z1, y1 - y0).normalize().mul(width)
        val a = Vector3f(CHART_X, y0, z0)
        val b = Vector3f(CHART_X, y1, z1)
        quad(
            out,
            Vector3f(a).sub(normal),
            Vector3f(b).sub(normal),
            Vector3f(b).add(normal),
            Vector3f(a).add(normal),
            floatArrayOf(0.27f, 0.68f, 1f, alpha)
        )
    }

    private fun quad(out: VertexConsumer, a: Vector3f, b: Vector3f, c: Vector3f, d: Vector3f, color: FloatArray) {
        vertex(out, a, 0f, 0f, color)
        vertex(out, b, 1f, 0f, color)
        vertex(out, c, 1f, 1f, color)
        vertex(out, d, 0f, 1f, color)
    }

    private fun vertex(out: VertexConsumer, p: Vector3f, u: Float, v: Float, color: FloatArray) {
        out.vertex(p.x.toDouble(), p.y.toDouble(), p.z.toDouble()).uv(u, v).color(color[0], color[1], color[2], color[3]).endVertex()
    }
}