package com.gtladd.gtladditions.client.render.machine.heart

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Vector3d
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Frame: x right, y away from the console, z up, origin at the black hole; [opening] is the innermost shell's opening. */
@OnlyIn(Dist.CLIENT)
class HeartFleetPlan(
    sizes: Map<String, Pair<Double, DoubleArray>>,
    opening: Vector3d,
    val deathStar: Double,
    val starkiller: Double
) {
    class Plane(val n: Vector3d, val u: Vector3d, val v: Vector3d, val up: Vector3d)

    class Pose {
        val p = Vector3d()
        val x = Vector3d()
        val y = Vector3d()
        val up = Vector3d()
        var k = 1.0
    }

    private class Ship(
        val type: String,
        val lane: Double = 0.0,
        val r: Double = 0.0,
        val phase: Double = 0.0,
        val fly: ((Double, Vector3d) -> Double)? = null
    )

    class Line(val radius: Double, val plane: Plane)

    private val lengths = sizes.mapValues { it.value.first }
    private val boxes = sizes.mapValues { it.value.second }
    private val open = Vector3d(opening).normalize()
    private val left = Vector3d(open).cross(0.0, 0.0, 1.0).normalize()
    private val ring = plane(Vector3d(0.0, 0.0, -1.0))

    /** The Death Star is 18 blocks across at k = 1. */
    val k = deathStar / 18.0
    private val ships = ArrayList<Ship>()
    val lines = ArrayList<Line>()
    val count: Int
        get() = ships.size
    private val stations = HashMap<String, DoubleArray>()

    private fun plane(n: Vector3d): Plane {
        val u = Vector3d(left).sub(Vector3d(n).mul(left.dot(n))).normalize()
        val v = Vector3d(n).cross(u).normalize()
        return Plane(Vector3d(n), u, v, Vector3d(n).negate())
    }

    private fun length(type: String) = lengths.getValue(type)

    private fun half(type: String, axis: Int) = boxes.getValue(type)[axis] * length(type) * k / 2

    private fun ball(type: String) = boxes.getValue(type).max() * length(type) * k / 2

    private fun beside(type: String) = max(half(type, 1) + 0.6, ball(type) + 0.3)

    fun type(index: Int): String = ships[index].type

    fun tail(index: Int): Double = half(ships[index].type, 0)

    fun lengthOf(index: Int): Double = length(ships[index].type) * k

    private fun place(out: Vector3d, r: Double, a: Double, pl: Plane): Vector3d =
        out.set(pl.u).mul(cos(a) * r).add(pl.v.x * sin(a) * r, pl.v.y * sin(a) * r, pl.v.z * sin(a) * r)

    private fun smooth(s: Double) = if (s <= 0) {
        0.0
    } else if (s >= 1) {
        1.0
    } else {
        s * s * (3 - 2 * s)
    }

    private fun period(r: Double) = 95 * (r / 69).pow(1.5)

    /** The ring's plane banked [degrees] about the horizontal axis at [azimuth] degrees (90 = front to back). */
    private fun banked(azimuth: Double, degrees: Double): Plane {
        val a = Math.toRadians(azimuth)
        return plane(Vector3d(ring.n).rotateAxis(Math.toRadians(degrees), cos(a), sin(a), 0.0))
    }

    private class Random(seed: Int) {
        private var s = seed

        fun next(): Double {
            s += 0x6D2B79F5
            var t = (s xor (s ushr 15)) * (1 or s)
            t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
            return ((t xor (t ushr 14)).toLong() and 0xffffffffL) / 4294967296.0
        }
    }

    /** A formation's members as (type, blocks behind the leader, blocks to the side). */
    private fun group(r: Double, phase: Double, members: List<Triple<String, Double, Double>>) =
        members.map { (type, back, side) -> Ship(type, r, r + side, phase - back / r) }

    /** Formations spread unevenly round radius [r], all at that radius' pace. */
    private fun lane(random: Random, r: Double, formations: List<List<Triple<String, Double, Double>>>): List<Ship> {
        val off = random.next() * TAU
        return formations.flatMapIndexed { i, members ->
            group(r, off + (i + (random.next() - 0.5) * 0.36) * TAU / formations.size, members)
        }
    }

    /** Per-pass state of one place in a lane's column. */
    private class Lead {
        var rr = 0.0
        var ang = 0.0
        var lift = 0.0
        var turn = 0.0
        var shown = 0.0
        var c = 0.0
    }

    init {
        fun long(t: String) = half(t, 0)

        /**
         * A formation from rows written front to back, separated by "/".
         * - A row is one craft on the lane's line or a pair at +-beside, so a formation stays within its lane's width.
         */
        fun formation(spec: String, gap: Double = 2.0): List<Triple<String, Double, Double>> {
            val out = ArrayList<Triple<String, Double, Double>>()
            var back = 0.0
            var prev = 0.0
            spec.split('/').map { it.trim().split(' ') }.forEachIndexed { i, row ->
                val l = row.maxOf { long(it) }
                if (i > 0) back += prev + l + gap
                prev = l
                val w = row.maxOf { beside(it) }
                row.forEachIndexed { j, t ->
                    out.add(
                        Triple(
                            t,
                            back,
                            if (row.size == 1) {
                                0.0
                            } else if (j == 0) {
                                -w
                            } else {
                                w
                            }
                        )
                    )
                }
            }

            return out
        }

        /** A fighter lane's formation: its rows from the front while they fit FLEET_SIZE craft. */
        fun laneFormation(spec: String): List<Triple<String, Double, Double>> {
            var n = 0
            val kept = spec.split('/').filterIndexed { i, row ->
                val size = row.trim().split(' ').size
                (i == 0 || n + size <= FLEET_SIZE).also { if (it) n += size }
            }

            return formation(kept.joinToString("/"))
        }

        fun depth(members: List<Triple<String, Double, Double>>) = members.maxOf { (t, back, _) -> back + long(t) }

        val fOut = max(
            max(beside("interceptor") + ball("interceptor"), beside("tie") + ball("tie")),
            ball("advanced") + 1
        ) + 0.3
        val hIn = listOf(beside("tie") + ball("tie"), ball("decimator"), ball("gozanti") + 1, ball("phantom")).max() + 0.3
        val edge = listOf(deathStar / 2, starkiller / 2, ball("destroyer")).max()
        val fl = 118.5 + fOut
        val step = 2 * fOut + 0.6
        val s1 = fl
        val s2 = fl + step
        val s3 = fl + 2 * step
        val hv3 = s3 + fOut + hIn + 1
        val e3 = hv3 + hIn + edge + 1.5
        val w3 = TAU / period(e3)
        val lift = 8 * k
        val carrier = mapOf("sd" to 0.0, "ds" to 2.1, "sk" to 4.2)
        stations["ds"] = doubleArrayOf(e3, carrier.getValue("ds"))
        stations["sk"] = doubleArrayOf(e3, carrier.getValue("sk"))

        val liftT = lift + 2 * k
        val clearT = 7 * k + 1

        // Banked alternately about near front-to-back axes, so each lane's front point stays at the ring's height.
        val tilted = mapOf(
            "sd" to (s1 to banked(80.0, 28.0)),
            "ds" to (s2 to banked(100.0, -17.0)),
            "sk" to (s3 to banked(90.0, 6.0))
        )

        val va = Vector3d()
        val vb = Vector3d()
        val vc = Vector3d()

        fun toward(d: Vector3d, pl: Plane, out: Vector3d): Vector3d = out.set(d).sub(Vector3d(pl.n).mul(d.dot(pl.n))).normalize()

        fun slerp(a: Vector3d, b: Vector3d, e: Double, out: Vector3d): Vector3d {
            val th = acos(a.dot(b).coerceIn(-1.0, 1.0))
            if (th < 1e-5) return out.set(b)
            val s = sin(th)
            return out.set(a).mul(sin((1 - e) * th) / s).add(Vector3d(b).mul(sin(e * th) / s))
        }

        fun heightOf(j: String, a: Double): Double {
            place(va, 1.0, a, ring)
            val (r, pl) = tilted.getValue(j)
            return r * toward(va, pl, vb).dot(ring.up)
        }

        fun dropAt(r: Double, rj: Double): Double {
            val e = (e3 - rj) / (e3 - r)
            var lo = 0.0
            var hi = 1.0

            repeat(24) {
                val m = (lo + hi) / 2
                if (smooth(m) < e) lo = m else hi = m
            }

            return lo
        }

        fun bump(s: Double) = min(1.0, min(s * 4, (1 - s) * 4))

        ships.add(Ship("destroyer", e3, e3, carrier.getValue("sd")))

        /*
         * Each fighter lane carries a column of LANE_FLEETS formations at one pace (a lap relative to its carrier in CRUISE seconds),
         * evenly spaced, so none catches another. Every other place holds a standing patrol; the rest are taken in turns by a
         * formation out of the carrier for one pass and left empty the next. Coming out, it drops from the carrier to the lane in
         * the ring's plane over TD seconds, lifted over the lanes it crosses, then turns into the banked plane over TM seconds;
         * going back reverses that. A change waits a pass if any of its craft would cross another lane too close.
         */
        for ((host, specs) in FLEETS) {
            val (r, pl) = tilted.getValue(host)
            val wr = TAU / CRUISE.getValue(host)
            val random = Random(host[1].code * 7 + LANE_FLEETS)

            repeat(LANE_FLEETS) { j ->
                val members = laneFormation(specs[j % specs.size])
                val rho0 = -(j + (random.next() - 0.5) * 0.3) * TAU / LANE_FLEETS
                val patrol = j % 2 == 0

                fun wanted(n: Long) = Math.floorMod(n + j / 2, 2L) == 0L

                fun blocked(m: Long, going: Boolean): Boolean {
                    val tm = (TAU * m - rho0) / wr
                    return members.any { (_, back, side) ->
                        tilted.any { (q, lane) ->
                            val rq = lane.first - side
                            q != host && lane.first > r && rq < e3 && run {
                                val s = if (going) dropAt(r, rq) else 1 - dropAt(r, rq)
                                val tau = s * TD - TD / 2
                                val e = smooth(s)
                                val rel = wr * tau
                                val a = carrier.getValue(host) + w3 * (tm + tau) + (if (going) rel * e else rel * (1 - e)) - back / rq
                                abs(heightOf(q, a) - liftT * bump(s)) < clearT
                            }
                        }
                    }
                }

                // After pass n of this place by its carrier, whether a formation is out in it.
                val memo = HashMap<Long, Boolean>()

                fun out(n: Long): Boolean {
                    if (patrol) return true
                    if (n < FIRST_PASS) return wanted(n)
                    memo[n]?.let { return it }
                    val w = wanted(n)
                    val value = if (blocked(n, w)) out(n - 1) else w
                    if (memo.size > 256) memo.clear()
                    memo[n] = value
                    return value
                }

                val lead = Lead()

                fun at(t: Double): Lead {
                    val x = (rho0 + wr * t) / TAU
                    val n = floor(x + 0.5).toLong()
                    val rel = (x - n) * TAU
                    val tau = rel / wr
                    val before = out(n - 1)
                    val after = out(n)
                    val c = carrier.getValue(host) + w3 * t

                    lead.rr = r
                    lead.ang = c + rel
                    lead.lift = 0.0
                    lead.turn = 1.0
                    lead.shown = 1.0
                    lead.c = c

                    if (before == after) {
                        if (!before) lead.shown = 0.0
                    } else if (after) {
                        if (tau < -TD / 2) {
                            lead.shown = 0.0
                        } else if (tau < TD / 2) {
                            val s = (tau + TD / 2) / TD
                            val e = smooth(s)
                            lead.rr = e3 + (r - e3) * e
                            lead.ang = c + rel * e
                            lead.lift = liftT * bump(s)
                            lead.turn = 0.0
                            lead.shown = min(1.0, s * 3)
                        } else if (tau < TD / 2 + TM) {
                            lead.turn = smooth((tau - TD / 2) / TM)
                        }
                    } else {
                        if (tau > TD / 2) {
                            lead.shown = 0.0
                        } else if (tau > -TD / 2) {
                            val s = (tau + TD / 2) / TD
                            val e = smooth(s)
                            lead.rr = r + (e3 - r) * e
                            lead.ang = c + rel * (1 - e)
                            lead.lift = liftT * bump(s)
                            lead.turn = 0.0
                            lead.shown = min(1.0, (1 - s) * 3)
                        } else if (tau > -TD / 2 - TM) {
                            lead.turn = 1 - smooth((tau + TD / 2 + TM) / TM)
                        }
                    }

                    return lead
                }

                for ((type, back, side) in members) {
                    ships.add(
                        Ship(type, fly = { t, o ->
                            val l = at(t)
                            if (l.shown == 0.0) {
                                place(o, e3, l.c, ring)
                                0.0
                            } else {
                                val rad = l.rr + side
                                place(va, 1.0, l.ang - back / rad, ring)
                                slerp(va, toward(va, pl, vb), l.turn, vc)
                                o.set(vc).mul(rad).add(Vector3d(ring.up).mul(l.lift))
                                l.shown
                            }
                        })
                    )
                }
            }
        }

        ships += lane(Random(29), hv3, List(CONVOYS) { i -> formation(CONVOY_FORMATIONS[i % CONVOY_FORMATIONS.size], 3.0) })

        // Escorts ride the outer edge with their carrier; the one behind also clears the tail of a formation coming out of it.
        val bodies = mapOf("sd" to ball("destroyer"), "ds" to deathStar / 2, "sk" to starkiller / 2)
        for ((host, pair) in ESCORTS) {
            val ahead = formation(pair.first, 3.0)
            val behind = formation(pair.second, 3.0)
            val body = bodies.getValue(host)
            val deep = FLEETS.getValue(host).maxOf { depth(laneFormation(it)) }
            ships += group(e3, carrier.getValue(host) + (body + 6 * k + depth(ahead)) / e3, ahead)
            ships += group(e3, carrier.getValue(host) - (max(body, deep) + 6 * k + long(behind[0].first)) / e3, behind)
        }

        for (host in listOf("sd", "ds", "sk")) {
            val (r, pl) = tilted.getValue(host)
            lines.add(Line(r, pl))
        }
    }

    private val fa = Vector3d()
    private val fb = Vector3d()
    private val fc = Vector3d()

    /** Into [o]: position, nose x, side y, top up, and how far it shows (0 = docked). */
    fun craft(index: Int, t: Double, o: Pose): Pose {
        val s = ships[index]
        val pl = ring
        val fly = s.fly
        if (fly == null) {
            val a = s.phase + TAU / period(s.lane) * t
            place(o.p, s.r, a, pl)
            o.x.set(pl.u).mul(-sin(a)).add(Vector3d(pl.v).mul(cos(a)))
            o.up.set(pl.up)
            o.k = 1.0
        } else {
            o.k = fly(t, o.p)
            fly(t - 0.4, fa)
            fly(t + 0.4, fb)
            fc.set(o.p).sub(fa)
            o.x.set(fb).sub(o.p)
            val turn = if (fc.lengthSquared() > 1e-8 && o.x.lengthSquared() > 1e-8) {
                Vector3d(fc).normalize().cross(Vector3d(o.x).normalize()).dot(pl.up)
            } else {
                0.0
            }

            o.x.add(fc)
            if (o.x.lengthSquared() < 1e-10) o.x.set(pl.u)
            o.x.normalize()
            o.up.set(pl.up)
            o.y.set(o.up).cross(o.x).normalize()
            o.up.set(o.x).cross(o.y)
            val roll = (-turn * 7).coerceIn(-0.6, 0.6)
            if (roll != 0.0) {
                o.y.rotateAxis(roll, o.x.x, o.x.y, o.x.z)
                o.up.rotateAxis(roll, o.x.x, o.x.y, o.x.z)
            }

            return o
        }

        o.y.set(o.up).cross(o.x).normalize()
        o.up.set(o.x).cross(o.y)
        return o
    }

    /** [key] is "ds" or "sk". */
    fun station(key: String, t: Double, out: Vector3d): Vector3d {
        val (r, phase) = stations.getValue(key).let { it[0] to it[1] }
        return place(out, r, phase + TAU / period(r) * t, ring)
    }

    val reach: Double
        get() = (stations.getValue("ds")[0]) + max(deathStar, starkiller) / 2 + 12

    companion object {
        private const val TAU = 2 * PI

        /** Formations in each fighter lane's column. */
        private const val LANE_FLEETS = 5

        /** Most craft in a fighter lane's formation; rows that do not fit are left out. */
        private const val FLEET_SIZE = 3

        /** Convoys on the transports' lane. */
        private const val CONVOYS = 4

        /** Seconds to drop between the carrier and the lane, then to turn into the lane's banked plane. */
        private const val TD = 7.0
        private const val TM = 8.0

        /** Passes before this are taken as planned, so a place's state never depends on an unbounded history. */
        private const val FIRST_PASS = -3L

        /** Seconds for a fighter lane's column to lap its carrier. */
        private val CRUISE = mapOf("sd" to 48.0, "ds" to 58.0, "sk" to 69.0)

        private val FLEETS = mapOf(
            "sd" to listOf(
                "tie / tie tie / tie",
                "tie / tie tie / tie tie",
                "interceptor / tie tie / tie",
                "tie tie / tie tie",
                "tie / tie tie",
                "interceptor / interceptor interceptor / tie tie"
            ),
            "ds" to listOf(
                "advanced / tie tie / tie tie",
                "interceptor / interceptor interceptor / interceptor",
                "tie / interceptor interceptor / tie tie",
                "interceptor / interceptor interceptor",
                "interceptor / tie tie / tie",
                "interceptor interceptor / interceptor interceptor"
            ),
            "sk" to listOf(
                "phantom / phantom / tie tie",
                "tie / tie tie / tie",
                "phantom / interceptor interceptor",
                "phantom / phantom / phantom",
                "tie / tie tie / tie tie",
                "phantom / tie tie / tie"
            )
        )

        private val CONVOY_FORMATIONS = listOf(
            "gozanti / tie tie / tie tie",
            "decimator / interceptor interceptor",
            "phantom / phantom / phantom",
            "gozanti / gozanti / tie tie",
            "decimator / decimator",
            "gozanti / decimator / interceptor interceptor"
        )

        /** Per carrier: the escort ahead of it and the one behind. */
        private val ESCORTS = mapOf(
            "sd" to ("gozanti / tie tie" to "tie tie / tie tie"),
            "ds" to ("decimator / tie tie" to "interceptor / interceptor interceptor"),
            "sk" to ("gozanti / gozanti" to "phantom / tie tie")
        )
    }
}