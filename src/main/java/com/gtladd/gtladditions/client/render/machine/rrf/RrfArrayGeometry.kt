package com.gtladd.gtladditions.client.render.machine.rrf

import kotlin.math.*

class RrfArrayMesh(initialCapacity: Int = 262144) {
    var vertices = FloatArray(initialCapacity)
        private set
    var size = 0
        private set
    var indices = IntArray(initialCapacity / 2)
        private set
    var indexCount = 0
        private set
    val vertexCount: Int get() = size / RrfArrayGeometry.STRIDE
    internal val scratch = RrfArrayGeometry.Scratch()
    private var installedTopology: IntArray? = null

    internal fun triangle(a: Int, b: Int, c: Int) {
        installedTopology = null
        if (indexCount + 3 > indices.size) indices = indices.copyOf(maxOf(indexCount + 3, indices.size * 2))
        indices[indexCount++] = a
        indices[indexCount++] = b
        indices[indexCount++] = c
    }

    fun clear() {
        indexCount = 0
        size = 0
        installedTopology = null
    }

    /** Keep a private index buffer per mesh; only the immutable source is shared. */
    internal fun beginFixedGeometry(topology: IntArray) {
        size = 0
        if (installedTopology !== topology) {
            if (indices.size < topology.size) indices = IntArray(maxOf(topology.size, indices.size * 2))
            topology.copyInto(indices)
            installedTopology = topology
        }

        indexCount = topology.size
    }

    internal fun appendVertices(source: RrfArrayMesh): Int {
        val base = vertexCount
        if (size + source.size > vertices.size) vertices = vertices.copyOf(maxOf(size + source.size, vertices.size * 2))
        source.vertices.copyInto(vertices, size, 0, source.size)
        size += source.size
        return base
    }

    internal fun reserveVertexRange(count: Int): Int {
        val start = size
        val end = start + count * RrfArrayGeometry.STRIDE
        if (end > vertices.size) vertices = vertices.copyOf(maxOf(end, vertices.size * 2))
        size = end
        return start
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
        if (size + 12 > vertices.size) vertices = vertices.copyOf(maxOf(size + 12, vertices.size * 2))
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
    private const val RING_COUNT = 5
    private const val CORNERS = 8
    private val purple = RrfColor(173f / 255, 123f / 255, 249f / 255)
    private val blue = RrfColor(51f / 255, 156f / 255, 1f)
    private val white = RrfColor(1f, 1f, 1f)
    private val radialCos = DoubleArray(SIDES) { cos(it * PI * 2 / SIDES) }
    private val radialSin = DoubleArray(SIDES) { sin(it * PI * 2 / SIDES) }
    private val scales = doubleArrayOf(1.0, 1.25, 1.65)
    private val strengths = doubleArrayOf(0.84, 0.06, 0.018)
    private val octagonX = doubleArrayOf(-0.4142, 0.4142, 1.0, 1.0, 0.4142, -0.4142, -1.0, -1.0)
    private val octagonZ = doubleArrayOf(-1.0, -1.0, -0.4142, 0.4142, 1.0, 1.0, 0.4142, -0.4142)

    /** A mesh owns its workspace, so interleaved builds never overwrite another mesh's state. */
    internal class Scratch {
        val rings = DoubleArray(RING_COUNT * CORNERS * 3)
        val envelopes = DoubleArray(RING_COUNT)
        val order = IntArray(RING_COUNT)
        val points = DoubleArray(CORNERS * 3)
        val coverage = DoubleArray(RING_COUNT)
        val tangents = DoubleArray(CORNERS * 3)
        val normals = DoubleArray(CORNERS * SIDES * 3)
        val miter = DoubleArray(CORNERS * SIDES)
        val yaw = DoubleArray(CORNERS)
        val pitch = DoubleArray(CORNERS)
    }

    private val topology by lazy {
        val draft = RrfArrayMesh(STRIDE)
        var base = 0
        repeat(RING_COUNT) { base += tubeIndices(draft, base, CORNERS, true) }
        repeat(CORNERS / 2) { base += tubeIndices(draft, base, RING_COUNT, false) }
        base += tubeIndices(draft, base, CORNERS, true)

        val core = coreTemplate
        for (i in 0 until core.indexCount step 3) {
            draft.triangle(base + core.indices[i], base + core.indices[i + 1], base + core.indices[i + 2])
        }

        draft.indices.copyOf(draft.indexCount)
    }

    private data class V(val x: Double, val y: Double, val z: Double) {
        operator fun plus(v: V) = V(x + v.x, y + v.y, z + v.z)

        operator fun times(k: Double) = V(x * k, y * k, z * k)
    }

    private val sphereNormals = Array(9) { latitude ->
        Array(17) { longitude ->
            val polar = latitude * PI / 8
            val angle = longitude * PI * 2 / 16
            V(sin(polar) * cos(angle), cos(polar), sin(polar) * sin(angle))
        }
    }

    fun build(seconds: Double, mesh: RrfArrayMesh) {
        mesh.beginFixedGeometry(topology)
        val work = mesh.scratch
        val angle = rotationAngle(seconds)

        for (i in 0 until RING_COUNT) {
            val phase = fraction(seconds * 0.17 + i / RING_COUNT.toDouble())
            val radius = 26 * (1 - phase).pow(1.25) + 1.2
            val height = BASE_HEIGHT + phase * HEIGHT_RANGE
            val offset = i * CORNERS * 3
            octagon(work.rings, offset, radius, height, angle)
            val envelope = sin(PI * phase).pow(2)
            tube(work.rings, offset, CORNERS, true, RING_RADIUS, if (i % 2 == 1) blue else purple, 0.9 * envelope, mesh)
            work.envelopes[i] = envelope
            work.order[i] = i
        }

        for (i in 1 until RING_COUNT) {
            val ring = work.order[i]
            var at = i

            while (at > 0 && work.rings[work.order[at - 1] * CORNERS * 3 + 1].compareTo(work.rings[ring * CORNERS * 3 + 1]) > 0) {
                work.order[at] = work.order[at - 1]
                at--
            }

            work.order[at] = ring
        }

        for (corner in 0 until CORNERS step 2) {
            for (i in 0 until RING_COUNT) {
                val ring = work.order[i]
                val offset = ring * CORNERS * 3 + corner * 3
                work.rings.copyInto(work.points, i * 3, offset, offset + 3)
                work.coverage[i] = work.envelopes[ring]
            }
            tube(work.points, 0, RING_COUNT, false, 0.045, purple, 0.2, mesh, work.coverage)
        }

        val phase = fraction(seconds * 0.25)
        val radius = 1 + phase * 27
        octagon(work.points, 0, radius, BASE_HEIGHT, angle)
        tube(work.points, 0, CORNERS, true, RING_RADIUS, blue, 0.7 * sin(PI * phase).pow(2), mesh)

        appendCore(seconds, mesh)
    }

    private fun fraction(t: Double) = t - floor(t)

    /** Zero follows the structure's X/Z edges. */
    fun rotationAngle(seconds: Double): Double = fraction(seconds / ROTATION_PERIOD_SECONDS) * PI * 2

    private fun octagon(points: DoubleArray, offset: Int, radius: Double, height: Double, angle: Double) {
        for (i in 0 until CORNERS) {
            points[offset + i * 3] = (octagonX[i] * cos(angle) - octagonZ[i] * sin(angle)) * radius
            points[offset + i * 3 + 1] = height
            points[offset + i * 3 + 2] = (octagonX[i] * sin(angle) + octagonZ[i] * cos(angle)) * radius
        }
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
        val coordinates = mesh.scratch.points

        for (i in points.indices) {
            coordinates[i * 3] = points[i].x
            coordinates[i * 3 + 1] = points[i].y
            coordinates[i * 3 + 2] = points[i].z
        }

        val base = mesh.vertexCount
        tube(coordinates, 0, points.size, closed, radius, color, opacity, mesh, coverage?.toDoubleArray(), tints)
        tubeIndices(mesh, base, points.size, closed)
    }

    private fun reciprocalLength(x: Double, y: Double, z: Double): Double = 1 / sqrt(x * x + y * y + z * z).coerceAtLeast(1e-9)

    private fun tube(
        points: DoubleArray,
        offset: Int,
        count: Int,
        closed: Boolean,
        radius: Double,
        color: RrfColor,
        opacity: Double,
        mesh: RrfArrayMesh,
        coverage: DoubleArray? = null,
        tints: List<RrfColor>? = null
    ) {
        val work = mesh.scratch

        for (i in 0 until count) {
            val current = offset + i * 3
            val previous = offset + (if (i == 0 && !closed) 0 else (i + count - 1) % count) * 3
            val next = offset + (if (i == count - 1 && !closed) count - 1 else (i + 1) % count) * 3

            var ix = points[current] - points[previous]
            var iy = points[current + 1] - points[previous + 1]
            var iz = points[current + 2] - points[previous + 2]
            var k = reciprocalLength(ix, iy, iz)
            ix *= k
            iy *= k
            iz *= k

            var ox = points[next] - points[current]
            var oy = points[next + 1] - points[current + 1]
            var oz = points[next + 2] - points[current + 2]
            k = reciprocalLength(ox, oy, oz)
            ox *= k
            oy *= k
            oz *= k

            val x = ix + ox
            val y = iy + oy
            val z = iz + oz
            k = reciprocalLength(x, y, z)
            work.tangents[i * 3] = x * k
            work.tangents[i * 3 + 1] = y * k
            work.tangents[i * 3 + 2] = z * k
        }

        for (i in 0 until count) {
            val tx = work.tangents[i * 3]
            val ty = work.tangents[i * 3 + 1]
            val tz = work.tangents[i * 3 + 2]
            val rx = if (abs(ty) > 0.999) 1.0 else 0.0
            val ry = if (abs(ty) > 0.999) 0.0 else 1.0
            val rz = 0.0

            var sx = ty * rz - tz * ry
            var sy = tz * rx - tx * rz
            var sz = tx * ry - ty * rx
            var k = reciprocalLength(sx, sy, sz)
            sx *= k
            sy *= k
            sz *= k

            var ux = ty * sz - tz * sy
            var uy = tz * sx - tx * sz
            var uz = tx * sy - ty * sx
            k = reciprocalLength(ux, uy, uz)
            ux *= k
            uy *= k
            uz *= k

            for (radial in 0 until SIDES) {
                val n = (i * SIDES + radial) * 3
                work.normals[n] = sx * radialCos[radial] + ux * radialSin[radial]
                work.normals[n + 1] = sy * radialCos[radial] + uy * radialSin[radial]
                work.normals[n + 2] = sz * radialCos[radial] + uz * radialSin[radial]
            }
        }

        for (i in 0 until count) {
            var ix: Double
            var iy: Double
            var iz: Double
            if (i > 0 || closed) {
                val current = offset + i * 3
                val previous = offset + (if (i > 0) i - 1 else count - 1) * 3
                ix = points[current] - points[previous]
                iy = points[current + 1] - points[previous + 1]
                iz = points[current + 2] - points[previous + 2]
                val k = reciprocalLength(ix, iy, iz)
                ix *= k
                iy *= k
                iz *= k
            } else {
                ix = work.tangents[0]
                iy = work.tangents[1]
                iz = work.tangents[2]
            }

            for (radial in 0 until SIDES) {
                val n = (i * SIDES + radial) * 3
                val along = work.normals[n] * ix + work.normals[n + 1] * iy + work.normals[n + 2] * iz
                work.miter[i * SIDES + radial] = 1 / sqrt((1 - along * along).coerceAtLeast(0.25))
            }

            work.yaw[i] = atan2(work.tangents[i * 3 + 2], work.tangents[i * 3])
            work.pitch[i] = asin(work.tangents[i * 3 + 1].coerceIn(-1.0, 1.0))
        }

        var write = mesh.reserveVertexRange(scales.size * count * SIDES)
        val output = mesh.vertices

        for (layer in scales.indices) {
            val width = radius * scales[layer]
            val emission = opacity * strengths[layer]

            for (point in 0 until count) {
                for (radial in 0 until SIDES) {
                    val n = (point * SIDES + radial) * 3
                    val p = offset + point * 3
                    val extent = width * work.miter[point * SIDES + radial]
                    val tint = tints?.get(point) ?: color

                    output[write++] = (points[p] + work.normals[n] * extent).toFloat()
                    output[write++] = (points[p + 1] + work.normals[n + 1] * extent).toFloat()
                    output[write++] = (points[p + 2] + work.normals[n + 2] * extent).toFloat()
                    output[write++] = tint.red
                    output[write++] = tint.green
                    output[write++] = tint.blue
                    output[write++] = (emission * (coverage?.get(point) ?: 1.0)).toFloat()
                    output[write++] = work.yaw[point].toFloat()
                    output[write++] = work.pitch[point].toFloat()
                    output[write++] = work.normals[n].toFloat()
                    output[write++] = work.normals[n + 1].toFloat()
                    output[write++] = work.normals[n + 2].toFloat()
                }
            }
        }
    }

    private fun tubeIndices(mesh: RrfArrayMesh, base: Int, pointCount: Int, closed: Boolean): Int {
        for (layer in scales.indices) {
            val start = base + layer * pointCount * SIDES
            val edges = if (closed) pointCount else pointCount - 1

            for (i in 0 until edges) {
                val next = (i + 1) % pointCount

                for (j in 0 until SIDES) {
                    val around = (j + 1) % SIDES
                    val a = start + i * SIDES + j
                    val b = start + i * SIDES + around
                    val c = start + next * SIDES + around
                    val d = start + next * SIDES + j
                    mesh.triangle(a, b, c)
                    mesh.triangle(a, c, d)
                }
            }
        }

        return scales.size * pointCount * SIDES
    }

    private val coreDistances = doubleArrayOf(0.0, 0.04, 0.09, 0.14, 0.19, 0.24, 0.295)

    private val coreCoverage = DoubleArray(coreDistances.size) { i ->
        val distance = coreDistances[i]
        if (distance == 0.0) 0.0 else ((0.295 - distance) / 0.255).pow(2)
    }

    private val coreTemplate by lazy {
        RrfArrayMesh(32768).also { compressedCore(V(0.0, BASE_HEIGHT + HEIGHT_RANGE * 0.875, 0.0), 0.0, it) }
    }

    private fun appendCore(seconds: Double, mesh: RrfArrayMesh) {
        val base = mesh.appendVertices(coreTemplate)
        val pulse = (1 - cos(fraction(seconds * 0.17) * PI * 2)) * 0.5
        val opacity = 0.26 + 0.14 * pulse
        var offset = (base + 3 * 9 * 17) * STRIDE + 6

        repeat(6) {
            for (strength in strengths) {
                val emission = opacity * strength

                for (coverage in coreCoverage) {
                    val value = (emission * coverage).toFloat()

                    repeat(SIDES) {
                        mesh.vertices[offset] = value
                        offset += STRIDE
                    }
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

        val base = mesh.vertexCount
        for (lat in 0..8) for (lon in 0..16) vertex(lat, lon)

        for (lat in 0 until 8) {
            for (lon in 0 until 16) {
                val a = base + lat * 17 + lon
                val b = a + 1
                val c = a + 18
                val d = a + 17
                mesh.triangle(a, b, c)
                mesh.triangle(a, c, d)
            }
        }
    }
}
