package com.gtladd.gtladditions.client.render.machine.rrf

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

enum class RrfEffectKind { ARRAY, CATALYTIC, FOCUS, STASIS, BOOST }

/** Controller-centred, with straight (not premultiplied) RGBA. */
class RrfEffectMesh(initialCapacity: Int = 32768) {
    var vertices = FloatArray(initialCapacity)
        private set
    var size = 0
        private set
    private var strokeVertices: RrfEffectMesh? = null

    fun clear() {
        size = 0
    }

    fun vertex(x: Double, y: Double, z: Double, color: RrfColor, alpha: Double, edge: Double, cap: Double) {
        reserve(RrfEffectGeometry.STRIDE)
        vertices[size++] = x.toFloat()
        vertices[size++] = y.toFloat()
        vertices[size++] = z.toFloat()
        vertices[size++] = color.red
        vertices[size++] = color.green
        vertices[size++] = color.blue
        vertices[size++] = alpha.toFloat()
        vertices[size++] = edge.toFloat()
        vertices[size++] = cap.toFloat()
    }

    internal fun copyVertex(offset: Int) {
        copyVertex(this, offset)
    }

    internal fun sharedStrokeVertices(): RrfEffectMesh = strokeVertices ?: RrfEffectMesh(8192).also { strokeVertices = it }

    internal fun copyVertex(source: RrfEffectMesh, offset: Int) {
        reserve(RrfEffectGeometry.STRIDE)
        source.vertices.copyInto(vertices, size, offset, offset + RrfEffectGeometry.STRIDE)
        size += RrfEffectGeometry.STRIDE
    }

    internal fun appendContour(source: FloatArray, alpha: Float) {
        reserve(source.size)
        source.copyInto(vertices, size)
        val start = size
        size += source.size
        for (i in start + 6 until size step RrfEffectGeometry.STRIDE) vertices[i] = alpha
    }

    private fun reserve(additional: Int) {
        val required = size + additional
        if (required > vertices.size) vertices = vertices.copyOf(max(vertices.size * 2, required))
    }

    fun copyLongFace(start: Int, end: Int, quarterTurn: Int) {
        reserve(end - start)
        vertices.copyInto(vertices, size, start, end)

        for (i in start until end step RrfEffectGeometry.STRIDE) {
            val x = vertices[i]
            val y = vertices[i + 1]
            val target = size + i - start
            when (quarterTurn) {
                1 -> {
                    vertices[target] = y
                    vertices[target + 1] = -x
                }
                2 -> {
                    vertices[target] = -x
                    vertices[target + 1] = -y
                }
                3 -> {
                    vertices[target] = -y
                    vertices[target + 1] = x
                }
                else -> error("Expected a longitudinal quarter turn")
            }
        }

        size += end - start
    }
}

data class RrfColor(val red: Float, val green: Float, val blue: Float)

object RrfEffectGeometry {
    const val STRIDE = 9
    private const val TAU = PI * 2
    private const val CIRCLE_SEGMENTS = 320
    private val purple = RrfColor(173f / 255, 123f / 255, 249f / 255)
    private val blue = RrfColor(51f / 255, 156f / 255, 1f)
    private val white = RrfColor(1f, 1f, 1f)
    private val frontOutline = purple
    private val frontSilver = white

    private data class P(val x: Double, val y: Double)
    private val unitCircle = List(CIRCLE_SEGMENTS + 1) {
        val angle = TAU * it / CIRCLE_SEGMENTS
        P(cos(angle), sin(angle))
    }

    private data class V(val x: Double, val y: Double, val z: Double) {
        operator fun plus(other: V) = V(x + other.x, y + other.y, z + other.z)

        operator fun minus(other: V) = V(x - other.x, y - other.y, z - other.z)

        operator fun times(scale: Double) = V(x * scale, y * scale, z * scale)
    }

    private class Plane(val origin: V, val u: V, val v: V, val frontPalette: Boolean = false) {
        fun tint(color: RrfColor): RrfColor = if (!frontPalette) {
            color
        } else {
            when (color) {
                purple -> frontOutline
                white -> frontSilver
                else -> color
            }
        }
    }

    private val front = Plane(V(0.0, 0.0, 0.54), V(1.0, 0.0, 0.0), V(0.0, 1.0, 0.0), true)
    private val top = Plane(V(0.0, 3.555, -9.0), V(1.0, 0.0, 0.0), V(0.0, 0.0, 1.0))

    private data class ContourKey(val points: List<P>, val plane: Plane, val color: RrfColor, val width: Double)

    // Only the finite, time-independent contours below enter this cache; animated paths use stroke directly.
    private val contours = ConcurrentHashMap<ContourKey, FloatArray>()

    fun jamProgress(seconds: Double): Double {
        val p = fraction(seconds / 2.4)
        return when {
            p < 0.30 -> smoothStep5(p / 0.30)
            p < 0.68 -> {
                val u = (p - 0.30) / 0.38
                1 - 0.16 * sin(PI * u).pow(2) * (0.5 + 0.5 * sin(TAU * 3 * u))
            }
            p < 0.85 -> 1 - 0.92 * smoothStep5((p - 0.68) / 0.17)
            else -> 0.08 * (1 - smoothStep5((p - 0.85) / 0.15))
        }
    }

    fun longHandAngle(seconds: Double): Double = 0.34 + 0.42 * (1 - jamProgress(seconds))

    fun shortHandAngle(): Double = 5 * PI / 6

    fun timeCursorPosition(seconds: Double): Double = -3.7 + 5.55 * jamProgress(seconds)

    fun module(kind: RrfEffectKind, seconds: Double, mesh: RrfEffectMesh) {
        require(kind != RrfEffectKind.ARRAY)
        mesh.clear()
        val painter = Painter(mesh)
        painter.seal(kind, front, seconds)
        val start = mesh.size
        painter.roof(kind, top, seconds)
        val end = mesh.size
        for (quarterTurn in 1..3) mesh.copyLongFace(start, end, quarterTurn)
    }

    private fun fraction(value: Double): Double = value - floor(value)

    private fun squaredLength(x: Double, y: Double): Double = x * x + y * y

    private fun smoothStep5(x: Double): Double = x * x * x * (x * (x * 6 - 15) + 10)

    private fun glow(phase: Double, sharpness: Double = 5.0): Double = exp(sharpness * (cos(TAU * phase) - 1))

    private fun path(vararg xy: Double): List<P> = List(xy.size / 2) { P(xy[it * 2], xy[it * 2 + 1]) }

    private fun oct(r: Double, cy: Double = 0.0, cx: Double = 0.0): List<P> {
        val k = sqrt(2.0) - 1.0
        return path(-k, -1.0, k, -1.0, 1.0, -k, 1.0, k, k, 1.0, -k, 1.0, -1.0, k, -1.0, -k, -k, -1.0)
            .map { P(it.x * r + cx, it.y * r + cy) }
    }

    private fun diamond(r: Double, cx: Double = 0.0, cy: Double = 0.0) = path(cx, cy - r, cx + r, cy, cx, cy + r, cx - r, cy, cx, cy - r)

    private fun arc(r: Double, start: Double = 0.0, end: Double = TAU, steps: Int = 64, cx: Double = 0.0, cy: Double = 0.0): List<P> {
        // Fixed angular resolution keeps animated radii continuous and error below 0.0001 block.
        val segments = max(steps, ceil(abs(end - start) / TAU * CIRCLE_SEGMENTS).toInt())
        if (start == 0.0 && end == TAU && segments == CIRCLE_SEGMENTS) return unitCircle.map { P(cx + it.x * r, cy + it.y * r) }
        return List(segments + 1) {
            val a = start + (end - start) * it / segments
            P(cx + cos(a) * r, cy + sin(a) * r)
        }
    }

    private fun bezier(a: P, b: P, c: P, steps: Int = 24): List<P> {
        val segments = max(steps, 128)
        return List(segments + 1) {
            val s = it.toDouble() / segments
            val q = 1 - s
            P(q * q * a.x + 2 * q * s * b.x + s * s * c.x, q * q * a.y + 2 * q * s * b.y + s * s * c.y)
        }
    }

    private class Painter(private val mesh: RrfEffectMesh) {
        private data class Node(val point: P, val opacity: Double)

        private data class Join(val leftIn: P, val leftOut: P, val rightIn: P, val rightOut: P, val bevel: Int = 0)

        private data class Mark(val point: P, val opacity: Double, val edge: Double, val cap: Double)

        private fun emit(mark: Mark, plane: Plane, color: RrfColor, target: RrfEffectMesh = mesh) {
            val p = mark.point
            target.vertex(
                plane.origin.x + plane.u.x * p.x + plane.v.x * p.y,
                plane.origin.y + plane.u.y * p.x + plane.v.y * p.y,
                plane.origin.z + plane.u.z * p.x + plane.v.z * p.y,
                color,
                mark.opacity,
                mark.edge,
                mark.cap
            )
        }

        private fun quad(a: Mark, b: Mark, c: Mark, d: Mark, plane: Plane, color: RrfColor) {
            val start = mesh.size
            emit(a, plane, color)
            emit(b, plane, color)
            emit(c, plane, color)
            mesh.copyVertex(start)
            mesh.copyVertex(start + 2 * STRIDE)
            emit(d, plane, color)
        }

        private fun cachedStroke(points: List<P>, plane: Plane, color: RrfColor, alpha: Double, width: Double) {
            val contour = contours.getOrPut(ContourKey(points, plane, color, width)) {
                val prepared = RrfEffectMesh()
                Painter(prepared).stroke(points, plane, color, 1.0, width)
                prepared.vertices.copyOf(prepared.size)
            }
            mesh.appendContour(contour, alpha.toFloat())
        }

        fun stroke(
            points: List<P>,
            plane: Plane,
            color: RrfColor = purple,
            alpha: Double = 0.8,
            width: Double = 0.022,
            weights: DoubleArray? = null
        ) {
            val nodes = ArrayList<Node>(points.size + 1)

            for ((index, p) in points.withIndex()) {
                val previous = nodes.lastOrNull()?.point
                if (previous == null || squaredLength(p.x - previous.x, p.y - previous.y) > 1e-20) {
                    nodes.add(Node(p, alpha * (weights?.get(index) ?: 1.0)))
                }
            }

            if (nodes.size < 2) return
            val closed = squaredLength(nodes.first().point.x - nodes.last().point.x, nodes.first().point.y - nodes.last().point.y) < 1e-18
            if (closed) nodes.removeAt(nodes.lastIndex)
            if (!closed && nodes.size == 2) {
                val a = nodes[0]
                val b = nodes[1]
                nodes.add(1, Node(P((a.point.x + b.point.x) / 2, (a.point.y + b.point.y) / 2), (a.opacity + b.opacity) / 2))
            }

            val count = if (closed) nodes.size else nodes.size - 1
            val directions = List(count) { index ->
                val a = nodes[index].point
                val b = nodes[(index + 1) % nodes.size].point
                val length = hypot(b.x - a.x, b.y - a.y)
                P((b.x - a.x) / length, (b.y - a.y) / length)
            }

            val normals = directions.map { P(-it.y, it.x) }
            fun negative(p: P) = P(-p.x, -p.y)
            val joins = nodes.indices.map { index ->
                val previous = normals[if (index == 0) if (closed) normals.lastIndex else 0 else index - 1]
                val next = normals[if (index == nodes.lastIndex && !closed) normals.lastIndex else index % count]
                val divisor = 1 + previous.x * next.x + previous.y * next.y
                val miter = if (divisor > 1e-8) P((previous.x + next.x) / divisor, (previous.y + next.y) / divisor) else next
                if (squaredLength(miter.x, miter.y) <= 9.0) {
                    Join(miter, miter, negative(miter), negative(miter))
                } else {
                    val turn = previous.x * next.y - previous.y * next.x
                    if (turn > 0) {
                        Join(miter, miter, negative(previous), negative(next), -1)
                    } else {
                        Join(previous, next, negative(miter), negative(miter), 1)
                    }
                }
            }

            val beveled = joins.any { it.bevel != 0 }
            if (closed && weights == null && beveled && convexStroke(nodes, normals, plane, color, width)) return
            // The stroke spans the line core and its glow envelope, five line widths across.
            val extent = width * 2.5
            val tint = plane.tint(color)
            fun mark(index: Int, offset: P, side: Double): Mark {
                val node = nodes[index]
                val cap = if (!closed && (index == 0 || index == nodes.lastIndex)) 0.0 else 1.0
                return Mark(P(node.point.x + offset.x * extent, node.point.y + offset.y * extent), node.opacity, side, cap)
            }

            if (!beveled) {
                val shared = mesh.sharedStrokeVertices()
                shared.clear()

                for (index in nodes.indices) {
                    emit(mark(index, joins[index].leftOut, 5.0), plane, tint, shared)
                    emit(mark(index, joins[index].rightOut, -5.0), plane, tint, shared)
                }

                for (index in 0 until count) {
                    val next = (index + 1) % nodes.size
                    val a = index * 2 * STRIDE
                    val b = next * 2 * STRIDE
                    mesh.copyVertex(shared, a)
                    mesh.copyVertex(shared, b)
                    mesh.copyVertex(shared, b + STRIDE)
                    mesh.copyVertex(shared, a)
                    mesh.copyVertex(shared, b + STRIDE)
                    mesh.copyVertex(shared, a + STRIDE)
                }
            } else {
                for (index in 0 until count) {
                    val next = (index + 1) % nodes.size
                    val leftStart = mark(index, joins[index].leftOut, 5.0)
                    val leftEnd = mark(next, joins[next].leftIn, 5.0)
                    val rightEnd = mark(next, joins[next].rightIn, -5.0)
                    val rightStart = mark(index, joins[index].rightOut, -5.0)
                    if (joins[index].bevel != 0 || joins[next].bevel != 0) {
                        // Pin zero distance to the original path even when a sharp outside corner is beveled.
                        val centerStart = mark(index, P(0.0, 0.0), 0.0)
                        val centerEnd = mark(next, P(0.0, 0.0), 0.0)
                        quad(leftStart, leftEnd, centerEnd, centerStart, plane, tint)
                        quad(centerStart, centerEnd, rightEnd, rightStart, plane, tint)
                    } else {
                        quad(leftStart, leftEnd, rightEnd, rightStart, plane, tint)
                    }
                }
            }

            for (index in nodes.indices) {
                val join = joins[index]
                if (join.bevel > 0) {
                    emit(mark(index, join.leftIn, 5.0), plane, tint)
                    emit(mark(index, join.leftOut, 5.0), plane, tint)
                    emit(mark(index, P(0.0, 0.0), 0.0), plane, tint)
                } else if (join.bevel < 0) {
                    emit(mark(index, join.rightIn, -5.0), plane, tint)
                    emit(mark(index, join.rightOut, -5.0), plane, tint)
                    emit(mark(index, P(0.0, 0.0), 0.0), plane, tint)
                }
            }

            if (!closed) {
                for (end in intArrayOf(0, nodes.lastIndex)) {
                    val direction = if (end == 0) negative(directions.first()) else directions.last()
                    val left = mark(end, joins[end].leftIn, 5.0)
                    val right = mark(end, joins[end].rightIn, -5.0)
                    fun cap(mark: Mark) =
                        mark.copy(point = P(mark.point.x + direction.x * extent, mark.point.y + direction.y * extent), cap = -5.0)
                    quad(cap(left), left, right, cap(right), plane, tint)
                }
            }
        }

        /** Partition thin convex interiors by their nearest edge; widening an inset would fold over itself. */
        private fun convexStroke(nodes: List<Node>, normals: List<P>, plane: Plane, color: RrfColor, width: Double): Boolean {
            val points = nodes.map { it.point }
            val area = points.indices.sumOf { i ->
                val a = points[i]
                val b = points[(i + 1) % points.size]
                a.x * b.y - b.x * a.y
            }

            if (abs(area) < 1e-10) return false
            val winding = if (area > 0) 1.0 else -1.0

            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                val c = points[(i + 2) % points.size]
                if (((b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)) * winding < -1e-10) return false
            }

            val inward = normals.map { P(it.x * winding, it.y * winding) }
            val extent = width * 2.5
            val tint = plane.tint(color)
            val opacity = nodes.first().opacity
            fun mark(p: P, distance: Double) = Mark(p, opacity, distance, 1.0)
            fun outer(p: P, n: P) = P(p.x - n.x * extent, p.y - n.y * extent)
            fun clip(polygon: List<P>, nx: Double, ny: Double, limit: Double): List<P> {
                val result = ArrayList<P>(polygon.size + 1)
                fun add(p: P) {
                    val last = result.lastOrNull()
                    if (last == null || squaredLength(last.x - p.x, last.y - p.y) > 1e-18) result.add(p)
                }

                for (i in polygon.indices) {
                    val a = polygon[i]
                    val b = polygon[(i + 1) % polygon.size]
                    val da = nx * a.x + ny * a.y - limit
                    val db = nx * b.x + ny * b.y - limit
                    val insideA = da <= 1e-10
                    val insideB = db <= 1e-10
                    if (insideA) add(a)
                    if (insideA != insideB) {
                        val t = (da / (da - db)).coerceIn(0.0, 1.0)
                        add(P(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
                    }
                }

                if (result.size > 1 && squaredLength(result.first().x - result.last().x, result.first().y - result.last().y) < 1e-18) {
                    result.removeAt(result.lastIndex)
                }

                return result
            }

            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                val normal = inward[i]
                quad(mark(a, 0.0), mark(b, 0.0), mark(outer(b, normal), 5.0), mark(outer(a, normal), 5.0), plane, tint)
                val previous = inward[(i + points.size - 1) % points.size]
                val divisor = 1 + previous.x * normal.x + previous.y * normal.y
                val miter = if (divisor > 1e-8) P((previous.x + normal.x) / divisor, (previous.y + normal.y) / divisor) else normal
                val left = mark(outer(a, previous), 5.0)
                val right = mark(outer(a, normal), 5.0)
                val center = mark(a, 0.0)
                if (squaredLength(miter.x, miter.y) <= 9.0) {
                    val tip = mark(outer(a, miter), 5.0)
                    emit(center, plane, tint)
                    emit(left, plane, tint)
                    emit(tip, plane, tint)
                    emit(center, plane, tint)
                    emit(tip, plane, tint)
                    emit(right, plane, tint)
                } else {
                    emit(center, plane, tint)
                    emit(left, plane, tint)
                    emit(right, plane, tint)
                }

                var cell = points
                for (j in points.indices) {
                    if (i == j) continue
                    val other = inward[j]
                    val p = points[j]
                    cell = clip(
                        cell,
                        normal.x - other.x,
                        normal.y - other.y,
                        normal.x * a.x + normal.y * a.y - other.x * p.x - other.y * p.y
                    )
                }

                fun interior(p: P) = mark(p, -max(0.0, normal.x * (p.x - a.x) + normal.y * (p.y - a.y)) * 2 / width)
                for (j in 1 until cell.lastIndex) {
                    emit(interior(cell[0]), plane, tint)
                    emit(interior(cell[j]), plane, tint)
                    emit(interior(cell[j + 1]), plane, tint)
                }
            }

            return true
        }

        fun light(p: P, plane: Plane, color: RrfColor = blue, radius: Double = 0.045, alpha: Double = 1.0) {
            val extent = radius * 3
            fun mark(x: Double, y: Double) = Mark(P(p.x + x * extent, p.y + y * extent), alpha, x * 3, 32 + y * 3)
            quad(mark(-1.0, -1.0), mark(1.0, -1.0), mark(1.0, 1.0), mark(-1.0, 1.0), plane, plane.tint(color))
        }

        private fun trail(points: List<P>, phase: Double, plane: Plane, color: RrfColor = blue, radius: Double = 0.055) {
            val distances = DoubleArray(points.size)

            for (i in 1 until points.size) {
                distances[i] = distances[i - 1] + hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
            }

            val total = distances.last()
            if (total <= 1e-10) return
            val fractions = java.util.TreeSet<Double>()
            for (i in 0..48) fractions.add(i / 48.0)
            for (distance in distances) fractions.add(distance / total)
            val trace = ArrayList<P>(fractions.size)
            val weights = DoubleArray(fractions.size)
            var segment = 1

            for ((index, fraction) in fractions.withIndex()) {
                val distance = fraction * total
                while (segment < distances.lastIndex && distances[segment] < distance) segment++
                val divisor = distances[segment] - distances[segment - 1]
                val f = if (divisor == 0.0) 0.0 else (distance - distances[segment - 1]) / divisor
                val a = points[segment - 1]
                val b = points[segment]
                trace.add(P(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f))
                weights[index] = 0.9 * sin(PI * fraction).pow(2) * glow(fraction - phase, 12.0)
            }

            stroke(trace, plane, color, 1.0, radius, weights)
        }

        private fun baseSeal(plane: Plane) {
            cachedStroke(oct(2.12), plane, purple, 0.5, 0.025)
            cachedStroke(oct(1.97), plane, purple, 0.20, 0.012)
        }

        fun seal(kind: RrfEffectKind, plane: Plane, t: Double) {
            when (kind) {
                RrfEffectKind.CATALYTIC -> catalytic(plane, t)
                RrfEffectKind.FOCUS -> focus(plane, t)
                RrfEffectKind.STASIS -> {
                    baseSeal(plane)
                    clock(plane, t)
                }
                RrfEffectKind.BOOST -> boost(plane, t)
                RrfEffectKind.ARRAY -> error("Array has no module seal")
            }
        }

        private fun catalytic(plane: Plane, t: Double) {
            baseSeal(plane)

            for (i in 0..3) {
                cachedStroke(
                    oct(0.45 + i * 0.5),
                    plane,
                    if (i % 2 == 1) blue else purple,
                    0.18 + 0.68 * glow(t / 3 - i / 4.0, 4.0),
                    0.028
                )
            }

            for (i in 0..3) {
                val angle = i * PI / 2

                for (side in intArrayOf(-1, 1)) {
                    val route = path(0.35, 0.0, 0.95, 0.0, 1.45, side * 0.55, 1.8, side * 0.55)
                        .map { P(it.x * cos(angle) - it.y * sin(angle), it.x * sin(angle) + it.y * cos(angle)) }
                    cachedStroke(route, plane, blue, 0.40, 0.021)
                    trail(route, t / 3, plane, blue, 0.065)
                }
            }
        }

        private fun focus(plane: Plane, t: Double) {
            baseSeal(plane)
            val phase = TAU * t / 3
            val throat = 0.61 + 0.31 * cos(phase)
            val twist = 0.58 + 0.20 * sin(phase)
            cachedStroke(arc(1.83), plane, purple, 0.35, 0.018)
            fun polar(r: Double, a: Double) = P(cos(a) * r, sin(a) * r)

            for (i in 0..7) {
                val a = i * TAU / 8
                val inner = polar(throat, a + twist)
                stroke(
                    bezier(polar(1.80, a), polar(1.24, a + 0.62), inner),
                    plane,
                    if (i % 2 == 1) purple else blue,
                    0.53 + 0.14 * cos(phase),
                    0.032
                )
                stroke(bezier(inner, polar(1.15, a + 0.78), polar(1.80, a + 0.48)), plane, if (i % 2 == 1) blue else purple, 0.26, 0.018)
            }

            stroke(arc(throat * 0.72), plane, blue, 0.72, 0.032)
            cachedStroke(diamond(0.22), plane, white, 0.78 - 0.18 * cos(phase), 0.036)
        }

        private fun numeral(text: String, x: Double, y: Double, plane: Plane) {
            for ((index, letter) in text.withIndex()) {
                val strokes = when (letter) {
                    'I' -> listOf(path(0.0, 0.0, 0.0, 1.0))
                    'V' -> listOf(path(-0.32, 1.0, 0.0, 0.0, 0.32, 1.0))
                    'X' -> listOf(path(-0.30, 0.0, 0.30, 1.0), path(-0.30, 1.0, 0.30, 0.0))
                    else -> error("Unsupported clock numeral")
                }

                for (glyph in strokes) {
                    cachedStroke(
                        glyph.map { P(x + (index - (text.length - 1) / 2.0) * 0.12 + it.x * 0.12, y + (it.y - 0.5) * 0.20) },
                        plane,
                        white,
                        0.65,
                        0.015
                    )
                }
            }
        }

        private fun clock(plane: Plane, t: Double) {
            val phase = TAU * t / 12
            cachedStroke(arc(1.83), plane, purple, 0.6 + 0.1 * cos(phase), 0.026)
            cachedStroke(arc(1.60), plane, blue, 0.35, 0.014)

            for (i in 0 until 60) {
                val a = PI / 2 - i * TAU / 60
                val major = i % 5 == 0
                val inner = if (major) 1.57 else 1.69
                cachedStroke(
                    path(cos(a) * inner, sin(a) * inner, cos(a) * 1.77, sin(a) * 1.77),
                    plane,
                    if (major) white else blue,
                    if (major) 0.72 else 0.38,
                    if (major) 0.032 else 0.016
                )
            }

            numeral("XII", 0.0, 1.35, plane)
            numeral("III", 1.34, 0.0, plane)
            numeral("VI", 0.0, -1.34, plane)
            numeral("IX", -1.34, 0.0, plane)
            fun hand(angle: Double, length: Double, width: Double, color: RrfColor) {
                val x = cos(angle)
                val y = sin(angle)
                stroke(
                    path(x * -0.19, y * -0.19, -y * width, x * width, x * length, y * length, y * width, -x * width, x * -0.19, y * -0.19),
                    plane,
                    color,
                    0.90,
                    0.025
                )
            }

            val push = jamProgress(t)
            hand(longHandAngle(t), 1.20, 0.035, blue)
            hand(shortHandAngle(), 1.02, 0.065, purple)
            light(P(0.0, 0.0), plane, white, 0.065, 0.9)
            val pinAngle = 0.34 - 0.045
            cachedStroke(
                path(cos(pinAngle) * 1.10, sin(pinAngle) * 1.10, cos(pinAngle) * 1.38, sin(pinAngle) * 1.38),
                plane,
                white,
                0.65 + 0.28 * push,
                0.048
            )
            light(P(cos(pinAngle) * 1.38, sin(pinAngle) * 1.38), plane, purple, 0.048, 0.85)
            val wheelY = -0.82
            val angle = 0.38 * sin(phase * 4)
            cachedStroke(arc(0.30, steps = 40, cy = wheelY), plane, blue, 0.72, 0.022)
            cachedStroke(arc(0.23, steps = 40, cy = wheelY), plane, purple, 0.50, 0.014)

            for (i in 0..2) {
                val a = angle + i * TAU / 3
                stroke(path(0.0, wheelY, cos(a) * 0.28, wheelY + sin(a) * 0.28), plane, white, 0.65, 0.018)
            }

            light(P(0.0, wheelY), plane, white, 0.035, 0.7)
        }

        private fun boost(plane: Plane, t: Double) {
            baseSeal(plane)

            for (ring in 0..1) {
                val radius = 1.1 + ring * 0.57

                for (i in 0 until 12) {
                    val a = i * PI / 6
                    val head = i / 12.0 - t * if (ring == 1) 1.2 else 1.5
                    cachedStroke(arc(radius, a, a + 6 * 0.057, 6), plane, if (ring == 1) purple else blue, 0.25 + 0.7 * glow(head), 0.045)
                    val q = a + 0.34
                    cachedStroke(
                        path(
                            cos(q - 0.10) * (radius - 0.12),
                            sin(q - 0.10) * (radius - 0.12),
                            cos(q) * radius,
                            sin(q) * radius,
                            cos(q - 0.10) * (radius + 0.12),
                            sin(q - 0.10) * (radius + 0.12)
                        ),
                        plane,
                        blue,
                        0.7,
                        0.026
                    )
                }
            }

            cachedStroke(diamond(0.42), plane, white, 0.8, 0.025)
        }

        fun roof(kind: RrfEffectKind, plane: Plane, t: Double) {
            cachedStroke(path(-1.25, -7.7, -1.25, 7.7, 1.25, 7.7, 1.25, -7.7, -1.25, -7.7), plane, purple, 0.42, 0.025)
            when (kind) {
                RrfEffectKind.CATALYTIC -> catalyticRoof(plane, t)
                RrfEffectKind.FOCUS -> {
                    for (y in doubleArrayOf(-6.0, -3.0, 3.0, 6.0)) {
                        for (side in intArrayOf(-1, 1)) {
                            val route = path(side * 1.12, y, side * 0.55, y * 0.6, 0.0, 0.0)
                            cachedStroke(route, plane, blue, 0.36, 0.022)
                            trail(route, t / 3, plane, blue, 0.075)
                        }
                    }
                    cachedStroke(diamond(0.5), plane, white, 0.55 + 0.35 * glow(t / 3 - 0.85, 4.0), 0.030)
                }
                RrfEffectKind.STASIS -> stasisRoof(plane, t)
                RrfEffectKind.BOOST -> {
                    for (i in 0..7) {
                        val y = -6.5 + i * 1.8
                        cachedStroke(
                            path(-0.95, y - 0.6, 0.0, y + 0.45, 0.95, y - 0.6),
                            plane,
                            if (i % 2 == 1) purple else blue,
                            0.25 + 0.7 * glow(i / 8.0 - t * 0.25),
                            0.07
                        )
                    }
                    for (side in intArrayOf(-1, 1)) trail(path(side * 1.12, -7.4, side * 1.12, 7.4), t * 0.33, plane, blue, 0.08)
                }
                RrfEffectKind.ARRAY -> error("Array has no module roof")
            }
        }

        private fun catalyticRoof(plane: Plane, t: Double) {
            val routes = arrayListOf(path(0.0, -7.15, 0.0, -5.3) to 0.0)

            for (side in intArrayOf(-1, 1)) {
                routes.add(path(0.0, -5.3, side * 0.55, -3.6) to 0.18)

                for (x in doubleArrayOf(side * 0.30, side * 0.94)) {
                    routes.add(path(side * 0.55, -3.6, x, -1.8) to 0.35)
                    routes.add(path(x, -1.8, x, 6.4) to 0.55)
                }
            }

            for ((route, delay) in routes) {
                cachedStroke(route, plane, blue, 0.37, 0.026)
                trail(route, t / 3 - delay, plane, blue, 0.075)
            }

            val nodes = arrayOf(
                doubleArrayOf(0.0, -5.3, 0.18),
                doubleArrayOf(-0.55, -3.6, 0.35),
                doubleArrayOf(0.55, -3.6, 0.35),
                doubleArrayOf(-0.94, -1.8, 0.55),
                doubleArrayOf(-0.30, -1.8, 0.55),
                doubleArrayOf(0.30, -1.8, 0.55),
                doubleArrayOf(0.94, -1.8, 0.55)
            )
            for ((x, y, delay) in nodes) light(P(x, y), plane, purple, 0.075, 0.22 + 0.65 * glow(t / 3 - delay, 4.0))

            for (x in doubleArrayOf(-0.94, -0.30, 0.30, 0.94)) {
                for (y in doubleArrayOf(0.6, 2.8, 5.0)) {
                    cachedStroke(
                        path(x - 0.11, y - 0.14, x + 0.11, y - 0.14, x + 0.11, y + 0.14, x - 0.11, y + 0.14, x - 0.11, y - 0.14),
                        plane,
                        purple,
                        0.25 + 0.55 * glow(t / 3 - 0.55 - (y + 1.8) / 8.2, 4.0),
                        0.025
                    )
                }
                cachedStroke(path(x - 0.16, 6.55, x + 0.16, 6.55), plane, white, 0.7, 0.04)
            }
        }

        private fun stasisRoof(plane: Plane, t: Double) {
            val push = jamProgress(t)
            val cursorY = timeCursorPosition(t)

            for (side in intArrayOf(-1, 1)) {
                cachedStroke(path(side * 0.72, -7.05, side * 0.72, 7.05), plane, purple, 0.36, 0.022)

                for (i in -14..14) {
                    val y = i * 0.5
                    val major = i % 4 == 0
                    cachedStroke(
                        path(side * if (major) 0.84 else 0.94, y, side * 1.11, y),
                        plane,
                        if (major) white else blue,
                        if (major) 0.55 else 0.3,
                        if (major) 0.026 else 0.016
                    )
                }
            }

            cachedStroke(path(-0.69, 2.12, 0.69, 2.12), plane, white, 0.74 + 0.18 * push, 0.045)
            for (side in intArrayOf(-1, 1)) {
                cachedStroke(path(side * 0.52, 2.48, side * 0.86, 2.48, side * 0.86, 1.82), plane, purple, 0.70, 0.034)
            }

            stroke(
                path(-0.83, cursorY - 0.10, -0.83, cursorY + 0.10, 0.83, cursorY + 0.10, 0.83, cursorY - 0.10, -0.83, cursorY - 0.10),
                plane,
                blue,
                0.92,
                0.035
            )
            stroke(path(0.0, -6.5, 0.0, cursorY - 0.14), plane, blue, 0.32, 0.023)

            for (side in intArrayOf(-1, 1)) {
                stroke(path(side * 0.98, cursorY - 0.28, side * 0.78, cursorY, side * 0.98, cursorY + 0.28), plane, blue, 0.85, 0.027)
            }
        }
    }
}
