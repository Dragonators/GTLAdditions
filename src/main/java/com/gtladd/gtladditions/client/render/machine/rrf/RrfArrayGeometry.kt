package com.gtladd.gtladditions.client.render.machine.rrf

import kotlin.math.*

class RrfArrayMesh(initialCapacity: Int = 262144) {
    var vertices = FloatArray(initialCapacity)
        private set
    var size = 0
        private set
    private var tubeSection: RrfArrayMesh? = null

    fun clear() {
        size = 0
    }

    internal fun sharedTubeVertices(): RrfArrayMesh = tubeSection ?: RrfArrayMesh(4096).also { tubeSection = it }

    internal fun copyVertex(source: RrfArrayMesh, offset: Int) {
        if (size + RrfArrayGeometry.STRIDE > vertices.size) vertices = vertices.copyOf(vertices.size * 2)
        source.vertices.copyInto(vertices, size, offset, offset + RrfArrayGeometry.STRIDE)
        size += RrfArrayGeometry.STRIDE
    }

    fun vertex(
        x: Double,
        y: Double,
        z: Double,
        color: RrfColor,
        emission: Double,
        yaw: Double,
        pitch: Double,
        nx: Double,
        ny: Double,
        nz: Double
    ) {
        if (size + 12 > vertices.size) vertices = vertices.copyOf(vertices.size * 2)
        vertices[size++] = x.toFloat()
        vertices[size++] = y.toFloat()
        vertices[size++] = z.toFloat()
        vertices[size++] = color.red
        vertices[size++] = color.green
        vertices[size++] = color.blue
        vertices[size++] = emission.toFloat()
        vertices[size++] = yaw.toFloat()
        vertices[size++] = pitch.toFloat()
        vertices[size++] = nx.toFloat()
        vertices[size++] = ny.toFloat()
        vertices[size++] = nz.toFloat()
    }
}

object RrfArrayGeometry {
    const val STRIDE = 12
    const val BASE_HEIGHT = 1.0
    const val HEIGHT_RANGE = 1.5
    const val ROTATION_PERIOD_SECONDS = 180.0
    private const val RING_RADIUS = 0.12
    private const val SIDES = 12
    private val purple = RrfColor(173f / 255, 123f / 255, 249f / 255)
    private val blue = RrfColor(51f / 255, 156f / 255, 1f)
    private val white = RrfColor(1f, 1f, 1f)
    private val radialCos = DoubleArray(SIDES) { cos(it * PI * 2 / SIDES) }
    private val radialSin = DoubleArray(SIDES) { sin(it * PI * 2 / SIDES) }
    private val scales = doubleArrayOf(1.0, 1.25, 1.65)
    private val strengths = doubleArrayOf(0.84, 0.06, 0.018)

    private data class V(val x: Double, val y: Double, val z: Double) {
        operator fun plus(v: V) = V(x + v.x, y + v.y, z + v.z)

        operator fun minus(v: V) = V(x - v.x, y - v.y, z - v.z)
        operator fun times(k: Double) = V(x * k, y * k, z * k)
        fun length() = sqrt(x * x + y * y + z * z)
        fun normalized(): V = this * (1 / length().coerceAtLeast(1e-9))
        fun cross(v: V) = V(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x)
        fun dot(v: V) = x * v.x + y * v.y + z * v.z
    }

    private val sphereNormals = Array(9) { latitude ->
        Array(17) { longitude ->
            val polar = latitude * PI / 8
            val angle = longitude * PI * 2 / 16
            V(sin(polar) * cos(angle), cos(polar), sin(polar) * sin(angle))
        }
    }

    fun build(seconds: Double, mesh: RrfArrayMesh) {
        mesh.clear()
        val angle = rotationAngle(seconds)
        val rings = ArrayList<Pair<Double, List<V>>>(5)
        for (i in 0..4) {
            val phase = fraction(seconds * 0.17 + i / 5.0)
            val radius = 26 * (1 - phase).pow(1.25) + 1.2
            val height = BASE_HEIGHT + phase * HEIGHT_RANGE
            val corners = octagon(radius, height, angle)
            val envelope = sin(PI * phase).pow(2)
            tube(corners, true, RING_RADIUS, if (i % 2 == 1) blue else purple, 0.9 * envelope, mesh)
            rings.add(envelope to corners)
        }
        val ordered = rings.sortedBy { it.second[0].y }
        for (corner in 0..6 step 2) {
            val points = ordered.map { it.second[corner] }
            tube(points, false, 0.045, purple, 0.2, mesh, ordered.map { it.first })
        }
        val phase = fraction(seconds * 0.25)
        val radius = 1 + phase * 27
        tube(octagon(radius, BASE_HEIGHT, angle), true, RING_RADIUS, blue, 0.7 * sin(PI * phase).pow(2), mesh)
        compressedCore(V(0.0, BASE_HEIGHT + HEIGHT_RANGE * 0.875, 0.0), seconds, mesh)
    }

    private fun fraction(t: Double) = t - floor(t)

    /** Zero follows the structure's X/Z edges. */
    fun rotationAngle(seconds: Double): Double = fraction(seconds / ROTATION_PERIOD_SECONDS) * PI * 2

    private fun octagon(radius: Double, height: Double, angle: Double): List<V> {
        val k = 0.4142
        val x = doubleArrayOf(-k, k, 1.0, 1.0, k, -k, -1.0, -1.0)
        val z = doubleArrayOf(-1.0, -1.0, -k, k, 1.0, 1.0, k, -k)
        return List(8) { V((x[it] * cos(angle) - z[it] * sin(angle)) * radius, height, (x[it] * sin(angle) + z[it] * cos(angle)) * radius) }
    }

    private fun tube(
        points: List<V>,
        closed: Boolean,
        radius: Double,
        color: RrfColor,
        opacity: Double,
        mesh: RrfArrayMesh,
        coverage: List<Double>? = null,
        tints: List<RrfColor>? = null
    ) {
        val tangents = points.indices.map { i ->
            val previous = if (i == 0 && !closed) points[0] else points[(i + points.size - 1) % points.size]
            val next = if (i == points.lastIndex && !closed) points.last() else points[(i + 1) % points.size]
            ((points[i] - previous).normalized() + (next - points[i]).normalized()).normalized()
        }
        val normals = tangents.map { tangent ->
            val reference = if (abs(tangent.y) > 0.999) V(1.0, 0.0, 0.0) else V(0.0, 1.0, 0.0)
            val side = tangent.cross(reference).normalized()
            val up = tangent.cross(side).normalized()
            Array(SIDES) { side * radialCos[it] + up * radialSin[it] }
        }
        val miterScales = points.indices.map { i ->
            val incoming = when {
                i > 0 -> (points[i] - points[i - 1]).normalized()
                closed -> (points[0] - points.last()).normalized()
                else -> tangents[0]
            }
            DoubleArray(SIDES) { radial ->
                // Intersection of the cylinder with the tangent-bisector plane; no bevel arc.
                val along = normals[i][radial].dot(incoming)
                1 / sqrt((1 - along * along).coerceAtLeast(0.25))
            }
        }
        val yaw = tangents.map { atan2(it.z, it.x) }
        val pitch = tangents.map { asin(it.y.coerceIn(-1.0, 1.0)) }
        val shared = mesh.sharedTubeVertices()
        fun vertex(point: Int, radial: Int, width: Double, emission: Double) {
            val n = normals[point][radial]
            val p = points[point]
            val extent = width * miterScales[point][radial]
            shared.vertex(p.x + n.x * extent, p.y + n.y * extent, p.z + n.z * extent, tints?.get(point) ?: color, emission * (coverage?.get(point) ?: 1.0), yaw[point], pitch[point], n.x, n.y, n.z)
        }
        for (layer in scales.indices) {
            val width = radius * scales[layer]
            val emission = opacity * strengths[layer]
            // Adjacent triangles use the same section vertices up to six times. Evaluate each once.
            shared.clear()
            for (point in points.indices) for (radial in 0 until SIDES) vertex(point, radial, width, emission)
            val count = if (closed) points.size else points.size - 1

            for (i in 0 until count) {
                val next = (i + 1) % points.size
                for (j in 0 until SIDES) {
                    val around = (j + 1) % SIDES
                    val a = (i * SIDES + j) * STRIDE
                    val b = (i * SIDES + around) * STRIDE
                    val c = (next * SIDES + around) * STRIDE
                    val d = (next * SIDES + j) * STRIDE
                    mesh.copyVertex(shared, a)
                    mesh.copyVertex(shared, b)
                    mesh.copyVertex(shared, c)
                    mesh.copyVertex(shared, a)
                    mesh.copyVertex(shared, c)
                    mesh.copyVertex(shared, d)
                }
            }
        }
    }

    private fun compressedCore(center: V, seconds: Double, mesh: RrfArrayMesh) {
        val scale = 1.7
        val pulse = (1 - cos(fraction(seconds * 0.17) * PI * 2)) * 0.5
        sphere(center, 0.29 * scale, purple, 0.16, mesh)
        sphere(center, 0.17 * scale, blue, 0.36, mesh)
        sphere(center, 0.065 * scale, white, 0.80, mesh)
        val distances = doubleArrayOf(0.0, 0.04, 0.09, 0.14, 0.19, 0.24, 0.295)
        val coverage = distances.map { if (it == 0.0) 0.0 else ((0.295 - it) / 0.255).pow(2) }
        val tints = List(distances.size) { if (it <= 1) white else blue }

        for (axis in listOf(V(1.0, 0.0, 0.0), V(0.0, 1.0, 0.0), V(0.0, 0.0, 1.0))) {
            for (sign in doubleArrayOf(-1.0, 1.0)) {
                val points = distances.map { center + axis * (it * sign * scale) }
                tube(points, false, 0.006 * scale, blue, 0.26 + 0.14 * pulse, mesh, coverage, tints)
            }
        }
    }

    private fun sphere(center: V, radius: Double, color: RrfColor, emission: Double, mesh: RrfArrayMesh) {
        fun vertex(latitude: Int, longitude: Int) {
            val n = sphereNormals[latitude][longitude]
            mesh.vertex(center.x + n.x * radius, center.y + n.y * radius, center.z + n.z * radius, color, emission, 8.0, 0.0, n.x, n.y, n.z)
        }
        for (lat in 0 until 8) {
            for (lon in 0 until 16) {
                vertex(lat, lon)
                vertex(lat, lon + 1)
                vertex(lat + 1, lon + 1)
                vertex(lat, lon)
                vertex(lat + 1, lon + 1)
                vertex(lat + 1, lon)
            }
        }
    }
}