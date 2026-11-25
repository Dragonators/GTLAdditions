package com.gtladd.gtladditions.client.render.scene

import com.gtladd.gtladditions.api.client.scene.PartBlend
import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.gtladd.gtladditions.api.client.scene.PartOutcome
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

internal object SceneOrder {
    private const val EPSILON = 0.0001
    private const val SCREEN_PAD = 32

    class Volume(val box: AABB, val center: Vec3? = null, val radius: Double = 0.0)

    class Item(
        val volumes: List<Volume>,
        val blend: PartBlend,
        val renderer: Any,
        val batchKey: Any?,
        val composite: Boolean = false,
        val volumeSource: Boolean = false,
        val layered: Boolean = false
    )

    sealed interface Step {
        val items: IntArray
    }

    class Draw(override val items: IntArray) : Step

    class Layered(override val items: IntArray) : Step

    class Plan(val steps: List<Step>, val outcomes: Array<PartOutcome>)

    fun volumes(bounds: PartBounds): List<Volume> = bounds.pieces.map { piece ->
        when (piece) {
            is PartBounds.Box -> Volume(AABB(piece.minX, piece.minY, piece.minZ, piece.maxX, piece.maxY, piece.maxZ))
            is PartBounds.Sphere -> Vec3(piece.x, piece.y, piece.z).let { center ->
                Volume(AABB.ofSize(center, piece.radius * 2, piece.radius * 2, piece.radius * 2), center, piece.radius)
            }
        }
    }

    fun order(items: List<Item>, view: Matrix4fc, projection: Matrix4fc, camera: Vec3, width: Int, height: Int): Plan {
        val count = items.size
        val inverse = Matrix4f(view).invert()
        val origin = inverse.transformPosition(Vector3f())
        val eye = camera.add(origin.x.toDouble(), origin.y.toDouble(), origin.z.toDouble())
        val forward = inverse.transformDirection(Vector3f(0f, 0f, -1f)).normalize()
        val direction = if (abs(projection.m33()) < EPSILON) {
            null
        } else {
            Vec3(forward.x.toDouble(), forward.y.toDouble(), forward.z.toDouble())
        }

        val vp = Matrix4f(projection).mul(view)
        val rects = items.map { item ->
            item.volumes.map { screenBounds(vp, it.box.move(-camera.x, -camera.y, -camera.z), width, height, SCREEN_PAD) }
        }

        val conflicts = ArrayList<IntArray>()
        val edges = ArrayList<IntArray>()

        for (a in 0 until count) {
            for (b in a + 1 until count) {
                when (val relation = relation(items[a], items[b], rects[a], rects[b], eye, direction, view, camera)) {
                    null -> if (!commute(items[a], items[b])) conflicts.add(intArrayOf(a, b))
                    0 -> Unit
                    // Edges run far to near.
                    else -> edges.add(if (relation > 0) intArrayOf(b, a) else intArrayOf(a, b))
                }
            }
        }

        val component = components(count, conflicts, edges)
        val componentCount = (component.maxOrNull() ?: -1) + 1
        val members = Array(componentCount) { ArrayList<Int>() }
        for (item in 0 until count) members[component[item]].add(item)

        val outcomes = Array(count) { PartOutcome.DRAWN }
        val layered = BooleanArray(componentCount)
        val sequences = Array(componentCount) { index ->
            val list = members[index]
            when {
                list.size == 1 -> list
                commutes(items, list) -> grouped(items, list)
                list.any { items[it].volumeSource } && list.all { items[it].volumeSource || items[it].layered } -> {
                    layered[index] = true
                    list.forEach { outcomes[it] = PartOutcome.DRAWN_LAYERED }
                    list
                }
                else -> {
                    list.forEach { outcomes[it] = PartOutcome.DRAWN_UNORDERED }
                    list.sortedWith(compareByDescending<Int> { centerDepth(items[it], view, camera) }.thenBy { it })
                }
            }
        }

        val next = Array(componentCount) { HashSet<Int>() }
        val incoming = IntArray(componentCount)

        for ((far, near) in edges) {
            val from = component[far]
            val to = component[near]
            if (from != to && next[from].add(to)) incoming[to]++
        }

        val steps = ArrayList<Step>()
        var draw = ArrayList<Int>()
        fun flush() {
            if (draw.isNotEmpty()) steps.add(Draw(draw.toIntArray()))
            draw = ArrayList()
        }

        val ready = (0 until componentCount).filterTo(ArrayList()) { incoming[it] == 0 }
        while (ready.isNotEmpty()) {
            val last = draw.lastOrNull()
            val chosen = ready.filter { !layered[it] && last != null && batches(items[last], items[sequences[it].first()]) }.minOrNull()
                ?: ready.minBy { sequences[it].first() }
            ready.remove(chosen)
            if (layered[chosen]) {
                flush()
                steps.add(Layered(sequences[chosen].toIntArray()))
            } else {
                for (item in sequences[chosen]) {
                    val previous = draw.lastOrNull()
                    if (previous != null && !batches(items[previous], items[item])) flush()
                    draw.add(item)
                }
            }

            for (target in next[chosen]) if (--incoming[target] == 0) ready.add(target)
        }

        flush()
        return Plan(steps, outcomes)
    }

    fun components(count: Int, conflicts: List<IntArray>, edges: List<IntArray>): IntArray {
        val parent = IntArray(count) { it }
        fun find(item: Int): Int {
            var root = item
            while (parent[root] != root) root = parent[root]
            return root
        }

        for ((a, b) in conflicts) parent[find(a)] = find(b)
        val roots = IntArray(count) { find(it) }
        val next = Array(count) { HashSet<Int>() }
        for ((far, near) in edges) if (roots[far] != roots[near]) next[roots[far]].add(roots[near])

        val index = IntArray(count) { -1 }
        val low = IntArray(count)
        val onStack = BooleanArray(count)
        val stack = ArrayList<Int>()
        val scc = IntArray(count) { -1 }
        var counter = 0
        var sccCount = 0
        fun visit(root: Int) {
            index[root] = counter
            low[root] = counter++
            stack.add(root)
            onStack[root] = true

            for (target in next[root]) {
                if (index[target] < 0) {
                    visit(target)
                    low[root] = minOf(low[root], low[target])
                } else if (onStack[target]) {
                    low[root] = minOf(low[root], index[target])
                }
            }

            if (low[root] == index[root]) {
                while (true) {
                    val member = stack.removeAt(stack.lastIndex)
                    onStack[member] = false
                    scc[member] = sccCount
                    if (member == root) break
                }
                sccCount++
            }
        }

        for (item in 0 until count) if (roots[item] == item && index[item] < 0) visit(item)

        // Number components by their first item so the result does not depend on traversal order.
        val numbering = IntArray(sccCount) { -1 }
        var assigned = 0
        return IntArray(count) { item ->
            val id = scc[roots[item]]
            if (numbering[id] < 0) numbering[id] = assigned++
            numbering[id]
        }
    }

    /** 1 means A is nearer, -1 means B is nearer, 0 means no ray meets both, null means the order cannot be proved. */
    private fun relation(
        a: Item,
        b: Item,
        aRects: List<IntArray>,
        bRects: List<IntArray>,
        eye: Vec3,
        direction: Vec3?,
        view: Matrix4fc,
        camera: Vec3
    ): Int? {
        var result = 0

        for (i in a.volumes.indices) {
            for (j in b.volumes.indices) {
                if (!overlap(aRects[i], bRects[j])) continue
                val pair = relation(a.volumes[i], b.volumes[j], eye, direction)
                    ?: depthRelation(a.volumes[i].box, b.volumes[j].box, view, camera)
                    ?: return null
                if (pair == 0) continue
                if (result == 0) {
                    result = pair
                } else if (result != pair) {
                    return null
                }
            }
        }

        return result
    }

    private fun commutes(items: List<Item>, members: List<Int>): Boolean {
        val first = items[members.first()]
        return members.all { commute(first, items[it]) }
    }

    private fun commute(a: Item, b: Item): Boolean =
        a.blend == b.blend &&
            (a.blend == PartBlend.ADDITIVE || a.blend == PartBlend.SCREEN) &&
            !a.composite &&
            !b.composite &&
            !a.volumeSource &&
            !b.volumeSource

    private fun grouped(items: List<Item>, members: List<Int>): List<Int> {
        val result = ArrayList<Int>(members.size)
        val remaining = ArrayList(members)

        while (remaining.isNotEmpty()) {
            val first = remaining.removeAt(0)
            result.add(first)
            val iterator = remaining.iterator()

            while (iterator.hasNext()) {
                val item = iterator.next()
                if (batches(items[first], items[item])) {
                    result.add(item)
                    iterator.remove()
                }
            }
        }

        return result
    }

    private fun batches(a: Item, b: Item): Boolean = a.renderer === b.renderer && a.batchKey != null && a.batchKey == b.batchKey

    private fun centerDepth(item: Item, view: Matrix4fc, camera: Vec3): Double {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY

        for (volume in item.volumes) {
            minX = minOf(minX, volume.box.minX)
            minY = minOf(minY, volume.box.minY)
            minZ = minOf(minZ, volume.box.minZ)
            maxX = maxOf(maxX, volume.box.maxX)
            maxY = maxOf(maxY, volume.box.maxY)
            maxZ = maxOf(maxZ, volume.box.maxZ)
        }

        val center = view.transformPosition(
            Vector3f(
                ((minX + maxX) / 2 - camera.x).toFloat(),
                ((minY + maxY) / 2 - camera.y).toFloat(),
                ((minZ + maxZ) / 2 - camera.z).toFloat()
            )
        )
        return -center.z.toDouble()
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
    fun relation(a: Volume, b: Volume, eye: Vec3, direction: Vec3?): Int? {
        val ac = a.center
        val bc = b.center
        if (ac != null && bc != null) {
            return plane(ac, bc, a.radius, b.radius, eye, direction)
        }

        if (ac != null) {
            val closest = Vec3(
                ac.x.coerceIn(b.box.minX, b.box.maxX),
                ac.y.coerceIn(b.box.minY, b.box.maxY),
                ac.z.coerceIn(b.box.minZ, b.box.maxZ)
            )
            return plane(ac, closest, a.radius, 0.0, eye, direction)
        }

        if (bc != null) {
            val closest = Vec3(
                bc.x.coerceIn(a.box.minX, a.box.maxX),
                bc.y.coerceIn(a.box.minY, a.box.maxY),
                bc.z.coerceIn(a.box.minZ, a.box.maxZ)
            )
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

    fun depthRelation(a: AABB, b: AABB, view: Matrix4fc, camera: Vec3): Int? {
        val first = depths(a, view, camera)
        val second = depths(b, view, camera)
        return when {
            first[1] + EPSILON < second[0] -> 1
            second[1] + EPSILON < first[0] -> -1
            else -> null
        }
    }

    private fun depths(box: AABB, view: Matrix4fc, camera: Vec3): DoubleArray {
        var near = Double.POSITIVE_INFINITY
        var far = Double.NEGATIVE_INFINITY
        val point = Vector3f()

        for (corner in 0..7) {
            view.transformPosition(
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

    /** [bounds] are camera-relative. */
    fun screenBounds(vp: Matrix4fc, bounds: AABB, width: Int, height: Int, pad: Int): IntArray {
        var x0 = width.toFloat()
        var y0 = height.toFloat()
        var x1 = 0f
        var y1 = 0f
        val point = Vector4f()

        for (corner in 0..7) {
            vp.transform(
                point.set(
                    (if (corner and 1 == 0) bounds.minX else bounds.maxX).toFloat(),
                    (if (corner and 2 == 0) bounds.minY else bounds.maxY).toFloat(),
                    (if (corner and 4 == 0) bounds.minZ else bounds.maxZ).toFloat(),
                    1f
                )
            )
            if (point.w <= 0f) return intArrayOf(0, 0, width, height)
            val x = (point.x / point.w * 0.5f + 0.5f) * width
            val y = (point.y / point.w * 0.5f + 0.5f) * height
            x0 = minOf(x0, x)
            y0 = minOf(y0, y)
            x1 = maxOf(x1, x)
            y1 = maxOf(y1, y)
        }

        val left = maxOf(0, floor(x0).toInt() - pad)
        val bottom = maxOf(0, floor(y0).toInt() - pad)
        val right = minOf(width, ceil(x1).toInt() + pad)
        val top = minOf(height, ceil(y1).toInt() + pad)
        return intArrayOf(left, bottom, maxOf(0, right - left), maxOf(0, top - bottom))
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
