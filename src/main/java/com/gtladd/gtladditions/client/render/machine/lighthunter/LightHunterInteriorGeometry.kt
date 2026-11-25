package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.common.machine.multiblock.structure.StructureResourceLoader
import com.gtladd.gtladditions.utils.LightHunterInteriorLayout
import com.gtladd.gtladditions.utils.module.LightHunterSpaceStationPosHelper
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin

/** EAST-facing, controller-centred frame. */
object LightHunterInteriorGeometry {
    const val HOUSING = 0
    const val FAN_FRAME = 16
    const val RECESS = 32
    const val WHITE = 64
    const val FAN_MARKING = 80
    const val FAN_MOUNT = 84
    const val FAN_TRIM = 88
    const val DIFFUSER = 96
    const val FAN_BLADE = 112
    const val FAN_HUB = 116
    const val FAN_GUARD = 128
    const val DISPLAY_HOUSING = 132
    const val DISPLAY_GLASS = 136
    const val DISPLAY_FACE = 140
    const val DISPLAY_OUTLINE = 144
    const val DISPLAY_LABEL = 148
    const val DISPLAY_MARKER = 152
    const val DISPLAY_TRIM = 156
    const val FAN_WIDTH = 2f
    const val DISPLAY_X = -30.5f
    private const val DISPLAY_Y = -0.05f
    private const val DISPLAY_WIDTH = 4.56f
    private const val DISPLAY_HEIGHT = 1.70f

    // Blue floor: source aisles 72..150. The lower corridor continues through 22..151.
    const val FACTORY_MIN_X = LightHunterInteriorLayout.FACTORY_MIN_X
    const val FACTORY_MAX_X = LightHunterInteriorLayout.FACTORY_MAX_X
    const val CORRIDOR_MIN_X = LightHunterInteriorLayout.CORRIDOR_MIN_X
    const val CORRIDOR_MAX_X = LightHunterInteriorLayout.CORRIDOR_MAX_X
    const val RAIL_WIDTH = LightHunterInteriorLayout.RAIL_WIDTH
    private const val FAN_SCALE = FAN_WIDTH / (0.96f + 2 * 0.012f)

    data class Fixture(val x: Float, val y: Float, val z: Float, val length: Float, val height: Float, val width: Float, val material: Int)

    val fanPositions = List(2) { FACTORY_MIN_X + (FACTORY_MAX_X - FACTORY_MIN_X) * (it + 1) / 3 }

    data class HullEdge(val a0: Int, val c0: Int, val a1: Int, val c1: Int)

    data class HullProjection(val length: Int, val width: Int, val edges: List<HullEdge>)

    val hullProjection: HullProjection by lazy {
        val aisles = StructureResourceLoader.loadAisles("multiblock/light_hunter_space_station.bin", "light_hunter_space_station")
        val length = aisles.size
        val width = aisles[0][0].length
        val occupied = Array(length) { a -> BooleanArray(width) { c -> aisles[a].any { row -> row[c] != ' ' } } }
        fun present(a: Int, c: Int) = a in 0 until length && c in 0 until width && occupied[a][c]
        fun runs(size: Int, visible: (Int) -> Boolean, emit: (Int, Int) -> Unit) {
            var start = -1

            for (i in 0..size) {
                if (i < size && visible(i)) {
                    if (start < 0) start = i
                } else if (start >= 0) {
                    emit(start, i)
                    start = -1
                }
            }
        }

        val edges = buildList {
            for (a in 0..length) runs(width, { c -> present(a - 1, c) != present(a, c) }) { lo, hi -> add(HullEdge(a, lo, a, hi)) }
            for (c in 0..width) runs(length, { a -> present(a, c - 1) != present(a, c) }) { lo, hi -> add(HullEdge(lo, c, hi, c)) }
        }

        HullProjection(length, width, edges)
    }

    val fixtures: List<Fixture> = buildList {
        LightHunterSpaceStationPosHelper.calculateModulePositions(BlockPos.ZERO, Direction.EAST).forEach { pos ->
            val side = if (pos.z < 0) -1f else 1f
            add(Fixture(pos.x.toFloat(), 2.40f, side * 7.1f, 2.3f, 0.12f, 0.46f, HOUSING))
            add(Fixture(pos.x.toFloat(), 2.32f, side * 7.1f, 2.0f, 0.025f, 0.26f, WHITE))
        }

        for (side in floatArrayOf(-1f, 1f)) {
            add(
                Fixture(
                    (CORRIDOR_MIN_X + CORRIDOR_MAX_X) / 2,
                    -3.5f + RAIL_WIDTH / 2,
                    side * (2.5f - RAIL_WIDTH / 2),
                    CORRIDOR_MAX_X - CORRIDOR_MIN_X,
                    RAIL_WIDTH,
                    RAIL_WIDTH,
                    DIFFUSER
                )
            )
        }
    }

    private val boundsByFacing = Direction.Plane.HORIZONTAL.associateWith { facing ->
        val matrix = LightHunterVfxGeometry.transform(facing)
        val a = matrix.transformPosition(Vector3f(CORRIDOR_MIN_X - 0.1f, -3.6f, -9.6f))
        val b = matrix.transformPosition(Vector3f(DISPLAY_X + 2.5f, 3.6f, 9.6f))
        AABB(a.x.toDouble(), a.y.toDouble(), a.z.toDouble(), b.x.toDouble(), b.y.toDouble(), b.z.toDouble())
    }

    fun bounds(facing: Direction): AABB = boundsByFacing.getValue(facing)

    fun write(out: VertexConsumer) {
        for (f in fixtures) {
            val x0 = f.x - f.length / 2
            val x1 = f.x + f.length / 2
            val y0 = f.y - f.height / 2
            val y1 = f.y + f.height / 2
            val z0 = f.z - f.width / 2
            val z1 = f.z + f.width / 2
            face(out, f.material, arrayOf(Vector3f(x0, y0, z1), Vector3f(x1, y0, z1), Vector3f(x1, y1, z1), Vector3f(x0, y1, z1)))
            face(out, f.material, arrayOf(Vector3f(x1, y0, z0), Vector3f(x0, y0, z0), Vector3f(x0, y1, z0), Vector3f(x1, y1, z0)))
            face(out, f.material, arrayOf(Vector3f(x0, y1, z1), Vector3f(x1, y1, z1), Vector3f(x1, y1, z0), Vector3f(x0, y1, z0)))
            face(out, f.material, arrayOf(Vector3f(x0, y0, z0), Vector3f(x1, y0, z0), Vector3f(x1, y0, z1), Vector3f(x0, y0, z1)))
            face(out, f.material, arrayOf(Vector3f(x1, y0, z1), Vector3f(x1, y0, z0), Vector3f(x1, y1, z0), Vector3f(x1, y1, z1)))
            face(out, f.material, arrayOf(Vector3f(x0, y0, z0), Vector3f(x0, y0, z1), Vector3f(x0, y1, z1), Vector3f(x0, y1, z0)))
        }

        fanPositions.forEach { FanWriter(out, it).write() }
        for (side in floatArrayOf(-1f, 1f)) WallDisplayWriter(out, side).write()
    }

    private fun face(out: VertexConsumer, material: Int, vertices: Array<Vector3f>, uvs: Array<FloatArray>? = null) {
        val normal = Vector3f(vertices[1]).sub(vertices[0]).cross(Vector3f(vertices[2]).sub(vertices[0])).normalize()
        val shade = (0.70f - 0.18f * normal.y + 0.07f * normal.x + 0.05f * normal.z).coerceIn(0.45f, 1f)
        val light = (255 * shade).toInt()
        vertices.forEachIndexed { i, p ->
            val u = if (uvs != null) {
                uvs[i][0]
            } else if (material >= DISPLAY_HOUSING) {
                (sign(p.z) * (DISPLAY_X - p.x) + DISPLAY_WIDTH / 2) / DISPLAY_WIDTH
            } else if (i == 1 || i == 2) {
                1f
            } else {
                0f
            }

            val v = if (uvs != null) {
                uvs[i][1]
            } else if (material >= DISPLAY_HOUSING) {
                (p.y - DISPLAY_Y + DISPLAY_HEIGHT / 2) / DISPLAY_HEIGHT
            } else if (i >= 2) {
                1f
            } else {
                0f
            }

            out.vertex(p.x.toDouble(), p.y.toDouble(), p.z.toDouble())
                .uv(u, v)
                .color(light, light, light, material)
                .endVertex()
        }
    }

    private abstract class SurfaceWriter(private val out: VertexConsumer) {
        protected abstract fun point(u: Float, v: Float, depth: Float): Vector3f

        protected fun emit(material: Int, vararg points: Vector3f) = face(out, material, arrayOf(*points))

        protected fun emitMapped(material: Int, points: Array<Vector3f>, uvs: Array<FloatArray>) = face(out, material, points, uvs)

        protected fun outline(w: Float, h: Float, cut: Float, d: Float) = arrayOf(
            point(-w / 2 + cut, -h / 2, d),
            point(w / 2 - cut, -h / 2, d),
            point(w / 2, -h / 2 + cut, d),
            point(w / 2, h / 2 - cut, d),
            point(w / 2 - cut, h / 2, d),
            point(-w / 2 + cut, h / 2, d),
            point(-w / 2, h / 2 - cut, d),
            point(-w / 2, -h / 2 + cut, d)
        )

        protected fun cap(points: Array<Vector3f>, material: Int, reverse: Boolean = false) {
            val center = Vector3f()
            points.forEach { center.add(it) }
            center.div(points.size.toFloat())

            for (i in points.indices) {
                val next = (i + 1) % points.size
                val a = points[if (reverse) next else i]
                val b = points[if (reverse) i else next]
                emit(material, center, a, b, b)
            }
        }

        protected fun join(a: Array<Vector3f>, b: Array<Vector3f>, material: Int) {
            for (i in a.indices) {
                val next = (i + 1) % a.size
                emit(material, a[i], a[next], b[next], b[i])
            }
        }

        protected fun plate(w: Float, h: Float, cut: Float, back: Float, thickness: Float, bevel: Float, material: Int) {
            val rings = arrayOf(
                outline(w, h, cut, back - bevel),
                outline(w + 2 * bevel, h + 2 * bevel, cut + bevel, back),
                outline(w + 2 * bevel, h + 2 * bevel, cut + bevel, back + thickness),
                outline(w, h, cut, back + thickness + bevel)
            )
            cap(rings.first(), material, true)
            for (layer in 0..2) join(rings[layer], rings[layer + 1], material)
            cap(rings.last(), material)
        }
    }

    /** Local u/v face into the corridor on either wall; depth starts at the wall surface. */
    private class WallDisplayWriter(out: VertexConsumer, val side: Float) : SurfaceWriter(out) {
        override fun point(u: Float, v: Float, depth: Float) = Vector3f(DISPLAY_X - side * u, DISPLAY_Y + v, side * (2.5f - depth))

        private fun rect(u: Float, v: Float, w: Float, h: Float, depth: Float, material: Int) {
            emit(
                material,
                point(u - w / 2, v - h / 2, depth),
                point(u + w / 2, v - h / 2, depth),
                point(u + w / 2, v + h / 2, depth),
                point(u - w / 2, v + h / 2, depth)
            )
        }

        private fun pixelU(x: Float) = (x / 960f - 0.5f) * DISPLAY_WIDTH

        private fun pixelV(y: Float) = (0.5f - y / 360f) * DISPLAY_HEIGHT

        private fun line(x0: Float, y0: Float, x1: Float, y1: Float, width: Float, material: Int = DISPLAY_OUTLINE) {
            val u0 = pixelU(x0)
            val v0 = pixelV(y0)
            val u1 = pixelU(x1)
            val v1 = pixelV(y1)
            val length = hypot(u1 - u0, v1 - v0)
            if (length == 0f) return
            val du = -(v1 - v0) / length * width * DISPLAY_WIDTH / 1920f
            val dv = (u1 - u0) / length * width * DISPLAY_WIDTH / 1920f
            emit(
                material,
                point(u0 - du, v0 - dv, 0.0725f),
                point(u1 - du, v1 - dv, 0.0725f),
                point(u1 + du, v1 + dv, 0.0725f),
                point(u0 + du, v0 + dv, 0.0725f)
            )
        }

        private fun label(text: String, x: Float, y: Float, size: Float) {
            val glyphs = mapOf(
                'L' to intArrayOf(16, 16, 16, 16, 16, 16, 31), 'H' to intArrayOf(17, 17, 17, 31, 17, 17, 17),
                'U' to intArrayOf(17, 17, 17, 17, 17, 17, 14), '/' to intArrayOf(1, 1, 2, 4, 8, 16, 16),
                'S' to intArrayOf(15, 16, 16, 14, 1, 1, 30), 'T' to intArrayOf(31, 4, 4, 4, 4, 4, 4),
                'R' to intArrayOf(30, 17, 17, 30, 20, 18, 17), 'C' to intArrayOf(14, 17, 16, 16, 16, 17, 14),
                'E' to intArrayOf(31, 16, 16, 30, 16, 16, 31), 'A' to intArrayOf(14, 17, 17, 31, 17, 17, 17),
                'N' to intArrayOf(17, 25, 21, 19, 17, 17, 17)
            )
            text.forEachIndexed { index, ch ->
                glyphs[ch]?.forEachIndexed { row, bits ->
                    for (column in 0..4) {
                        if (((bits shr (4 - column)) and 1) != 0) {
                            val px = x + (index * 6 + column + 0.5f) * size
                            val py = y + (row + 0.5f) * size
                            rect(pixelU(px), pixelV(py), size * DISPLAY_WIDTH / 960f, size * DISPLAY_HEIGHT / 360f, 0.0726f, DISPLAY_LABEL)
                        }
                    }
                }
            }
        }

        fun write() {
            plate(4.72f, 1.86f, 0.12f, 0.008f, 0.045f, 0.008f, DISPLAY_HOUSING)
            plate(4.64f, 1.78f, 0.09f, 0.050f, 0.015f, 0.004f, DISPLAY_GLASS)
            cap(outline(DISPLAY_WIDTH, DISPLAY_HEIGHT, 0.075f, 0.071f), DISPLAY_FACE)
            rect(0f, 0.864f, 4.18f, 0.009f, 0.073f, DISPLAY_GLASS)
            for (u in floatArrayOf(-2.15f, 2.15f)) rect(u, -0.864f, 0.12f, 0.013f, 0.073f, DISPLAY_TRIM)
            val hull = hullProjection
            val scale = 3.47f
            val ox = (960f - hull.length * scale) / 2
            val oy = (360f - hull.width * scale) / 2 + 12f
            hull.edges.forEach { e -> line(ox + e.a0 * scale, oy + e.c0 * scale, ox + e.a1 * scale, oy + e.c1 * scale, 1.6f) }

            for (a in intArrayOf(45, 75, 115, 170)) {
                val x = ox + a * scale
                val y = oy + 36.5f * scale
                val mat = if (a == 170) DISPLAY_MARKER else DISPLAY_OUTLINE
                line(x - 5, y - 5, x + 5, y - 5, 1.5f, mat)
                line(x + 5, y - 5, x + 5, y + 5, 1.5f, mat)
                line(x + 5, y + 5, x - 5, y + 5, 1.5f, mat)
                line(x - 5, y + 5, x - 5, y - 5, 1.5f, mat)
                line(x, y - 10, x, y - 17, 1.5f, mat)
                line(x, y + 10, x, y + 17, 1.5f, mat)
            }

            label("LH / HULL", 30f, 22f, 2.0f)
            label("STRUCTURE / SCAN", 32f, 321f, 1.5f)
            for (i in 0..3) line(837f + i * 23, 27f, 849f + i * 23, 27f, 2f)
        }
    }

    /** Local u/v span the ceiling; positive depth points down into the room. */
    private class FanWriter(out: VertexConsumer, val x: Float) : SurfaceWriter(out) {
        override fun point(u: Float, v: Float, depth: Float) = Vector3f(x - v * FAN_SCALE, 3.49f - depth, u * FAN_SCALE)

        private fun prism(vertices: Array<FloatArray>, back: Float, front: Float, material: Int) {
            val a = vertices.map { point(it[0], it[1], back) }.toTypedArray()
            val b = vertices.map { point(it[0], it[1], front) }.toTypedArray()
            cap(a, material, true)
            join(a, b, material)
            cap(b, material)
        }

        private fun disc(u: Float, v: Float, radius: Float, depth: Float, thickness: Float, material: Int, sides: Int = 32) {
            val vertices = Array(sides) { i ->
                val angle = i * 2 * PI / sides
                floatArrayOf(u + radius * cos(angle).toFloat(), v + radius * sin(angle).toFloat())
            }
            prism(vertices, depth - thickness / 2, depth + thickness / 2, material)
        }

        private fun bar(u: Float, v: Float, length: Float, width: Float, depth: Float, thickness: Float, angle: Float, material: Int) {
            val c = cos(angle)
            val s = sin(angle)
            val vertices = arrayOf(
                floatArrayOf(-length / 2, -width / 2),
                floatArrayOf(length / 2, -width / 2),
                floatArrayOf(length / 2, width / 2),
                floatArrayOf(-length / 2, width / 2)
            )
                .map { p -> floatArrayOf(u + p[0] * c - p[1] * s, v + p[0] * s + p[1] * c) }.toTypedArray()
            prism(vertices, depth - thickness / 2, depth + thickness / 2, material)
        }

        private fun piercedFrame() {
            val angles = buildList {
                for (i in 0 until 64) add((i * 2 * PI / 64 - PI).toFloat())

                // Include every chamfer corner, so circular tessellation cannot round the outer frame.
                for (bevel in floatArrayOf(0f, 0.008f)) {
                    for (u in floatArrayOf(-1f, 1f)) {
                        for (v in floatArrayOf(-1f, 1f)) {
                            add(atan2(v * (0.42f + bevel), u * (0.47f - 0.105f)))
                            add(atan2(v * (0.42f - 0.105f), u * (0.47f + bevel)))
                        }
                    }
                }
            }.distinct().sorted()

            fun layer(bevel: Float, depth: Float): Pair<Array<Vector3f>, Array<Vector3f>> {
                val halfWidth = 0.47f + bevel
                val halfHeight = 0.42f + bevel
                val cut = 0.105f + bevel
                val outer = angles.map { a ->
                    val c = cos(a)
                    val s = sin(a)
                    val radius = minOf(halfWidth / abs(c), halfHeight / abs(s), (halfWidth + halfHeight - cut) / (abs(c) + abs(s)))
                    point(radius * c, radius * s, depth)
                }.toTypedArray()

                val inner = angles.map { a -> point((0.370f - bevel) * cos(a), (0.370f - bevel) * sin(a), depth) }.toTypedArray()
                return outer to inner
            }

            val layers = arrayOf(layer(0f, 0.047f), layer(0.008f, 0.055f), layer(0.008f, 0.125f), layer(0f, 0.133f))
            for (i in 0..2) {
                join(layers[i].first, layers[i + 1].first, FAN_FRAME)
                join(layers[i + 1].second, layers[i].second, FAN_FRAME)
            }

            for (i in angles.indices) {
                val next = (i + 1) % angles.size
                val back = layers.first()
                val front = layers.last()
                emit(FAN_FRAME, back.first[next], back.first[i], back.second[i], back.second[next])
                emit(FAN_FRAME, front.first[i], front.first[next], front.second[next], front.second[i])
            }
        }

        private fun blade(angle: Float, back: Float, front: Float) {
            val outline = arrayOf(
                floatArrayOf(0.07f, -0.015f),
                floatArrayOf(0.19f, 0.015f),
                floatArrayOf(0.278f, 0.105f),
                floatArrayOf(0.242f, 0.162f),
                floatArrayOf(0.125f, 0.104f),
                floatArrayOf(0.065f, 0.035f)
            )
                .map { p -> floatArrayOf(1.08f * (p[0] * cos(angle) - p[1] * sin(angle)), 1.08f * (p[0] * sin(angle) + p[1] * cos(angle))) }
            val uvs = arrayOf(
                floatArrayOf(0f, 0f),
                floatArrayOf(0f, 0.5f),
                floatArrayOf(0f, 1f),
                floatArrayOf(1f, 1f),
                floatArrayOf(1f, 0.5f),
                floatArrayOf(1f, 0f)
            )
            fun at(i: Int, depth: Float) = point(outline[i][0], outline[i][1], depth)

            for (strip in arrayOf(intArrayOf(0, 1, 4, 5), intArrayOf(1, 2, 3, 4))) {
                emitMapped(FAN_BLADE, Array(4) { at(strip[3 - it], back) }, Array(4) { uvs[strip[3 - it]] })
                emitMapped(FAN_BLADE, Array(4) { at(strip[it], front) }, Array(4) { uvs[strip[it]] })
            }

            for (i in outline.indices) {
                val next = (i + 1) % outline.size
                emitMapped(
                    FAN_BLADE,
                    arrayOf(at(i, back), at(next, back), at(next, front), at(i, front)),
                    arrayOf(uvs[i], uvs[next], uvs[next], uvs[i])
                )
            }
        }

        private fun hub(radius: Float, depth: Float, thickness: Float, sides: Int) {
            val unit = Array(sides) { i -> floatArrayOf(cos(i * 2 * PI / sides).toFloat(), sin(i * 2 * PI / sides).toFloat()) }
            val center = floatArrayOf(0f, 0f)
            val back = depth - thickness / 2
            val front = depth + thickness / 2
            fun at(i: Int, d: Float) = point(radius * unit[i][0], radius * unit[i][1], d)

            for (i in 0 until sides) {
                val next = (i + 1) % sides
                emitMapped(
                    FAN_HUB,
                    arrayOf(point(0f, 0f, back), at(next, back), at(i, back), at(i, back)),
                    arrayOf(center, unit[next], unit[i], unit[i])
                )
                emitMapped(
                    FAN_HUB,
                    arrayOf(at(i, back), at(next, back), at(next, front), at(i, front)),
                    arrayOf(unit[i], unit[next], unit[next], unit[i])
                )
                emitMapped(
                    FAN_HUB,
                    arrayOf(point(0f, 0f, front), at(i, front), at(next, front), at(next, front)),
                    arrayOf(center, unit[i], unit[next], unit[next])
                )
            }
        }

        private fun ring(radius: Float, tube: Float, depth: Float, sides: Int, segments: Int, material: Int) {
            fun vertex(i: Int, j: Int): Vector3f {
                val a = i * 2 * PI / segments
                val b = j * 2 * PI / sides
                val r = radius + tube * cos(b).toFloat()
                return point(r * cos(a).toFloat(), r * sin(a).toFloat(), depth + tube * sin(b).toFloat())
            }

            for (i in 0 until segments) {
                for (j in 0 until sides) {
                    emit(material, vertex(i, j), vertex(i + 1, j), vertex(i + 1, j + 1), vertex(i, j + 1))
                }
            }
        }

        fun write() {
            plate(0.96f, 0.86f, 0.10f, 0.013f, 0.018f, 0.012f, FAN_MOUNT)
            piercedFrame()
            disc(0f, 0f, 0.376f, 0.054f, 0.012f, RECESS, 48)
            for (i in 0 until 7) blade((i * 2 * PI / 7).toFloat(), 0.072f, 0.084f)
            hub(0.088f, 0.086f, 0.022f, 32)
            ring(0.345f, 0.008f, 0.112f, 5, 48, FAN_GUARD)
            ring(0.190f, 0.005f, 0.112f, 4, 40, FAN_GUARD)

            for (i in 0 until 3) {
                val a = (i * 2 * PI / 3 + PI / 2).toFloat()
                bar(0.194f * cos(a), 0.194f * sin(a), 0.290f, 0.013f, 0.113f, 0.009f, a, FAN_GUARD)
            }

            for (side in floatArrayOf(-1f, 1f)) {
                bar(side * 0.34f, -0.367f, 0.072f, 0.008f, 0.134f, 0.003f, 0f, FAN_MARKING)
                bar(side * 0.425f, 0.245f, 0.008f, 0.070f, 0.134f, 0.003f, 0f, FAN_TRIM)
            }
        }
    }
}
