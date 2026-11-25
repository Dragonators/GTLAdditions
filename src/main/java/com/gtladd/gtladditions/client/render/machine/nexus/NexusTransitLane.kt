package com.gtladd.gtladditions.client.render.machine.nexus

import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.smooth
import org.joml.Matrix4f
import org.joml.Vector3d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal class NexusTransitLane(nodes: Map<String, NexusGpu.Node>) {
    private class Craft(nodes: Map<String, NexusGpu.Node>, name: String, val length: Double) {
        val solid = nodes.getValue("transit.$name.solid")
        val depth = nodes.getValue("transit.$name.depth")
        val fade = nodes.getValue("transit.$name.fade")
        val solidStream = solid.attributeStream(STREAM, HULL_FIELDS, true)
        val fadeStream = fade.attributeStream(STREAM, HULL_FIELDS, true)

        init {
            depth.instanceSource = fade
        }
    }

    // All ships of a lane turn at one speed, so none ever catches up with another.
    private class Lane(val radius: Double, val z: Double, val direction: Double, val speed: Double) {
        val turn = direction * speed / radius
        val relative = speed / radius - direction * ZONE_TURN
        val interval = ZONE_STEP / relative
        val entry = ZONE_START + if (direction < 0) Math.toRadians((ZONE_ARC_MIN + ZONE_ARC_MAX) / 2) else 0.0
        val trailSpan = TRAIL / speed
    }

    private class Pass {
        var index = 0L
        var arrival = Double.POSITIVE_INFINITY
        var nextArrival = Double.NEGATIVE_INFINITY
        var u = 0.0
        var start = 0.0
        var cruise = 0.0
    }

    private class Ship(val index: Int, val lane: Lane, val craft: Int) {
        val threshold = 0.08 + 0.85 * hash(index, 4)

        // Convoy: a member keeps its leader's passes and ring threshold, lagging every jump by `lag` seconds.
        var lead = this
        var lag = 0.0

        // The angle of its place at time 0, and its small offset from the lane line.
        var slot = 0.0
        var radius = lane.radius
        var z = lane.z
        var entryPhase = 0.0
        val pass = Pass()

        var active = false
        var want = false
        var flipAt = Double.NEGATIVE_INFINITY
        var onPass = Long.MIN_VALUE
        var onPhase = Phase.GAP
        var onTime = 0.0

        // While leaving, pass stays at the last active orbit and offU fixes the launch point.
        var offTime = Double.NEGATIVE_INFINITY
        var offU = 0.0
    }

    private enum class Phase { ARRIVING, CRUISING, LEAVING, GAP }

    private class Pose {
        val p = Vector3d()
        val x = Vector3d()
        val y = Vector3d()
        val up = Vector3d()
    }

    private val crafts = arrayOf(Craft(nodes, "gozanti", 4.2), Craft(nodes, "decimator", 3.2), Craft(nodes, "bff1", 5.2))
    private val flares = nodes.getValue("transit.flares")
    private val plumes = nodes.getValue("transit.plumes")
    private val trails = nodes.getValue("transit.trails")
    private val flareStream = flares.attributeStream(STREAM, FLARE_FIELDS, true)
    private val plumeStream = plumes.attributeStream(STREAM, PLUME_FIELDS, true)
    private val trailStream = trails.attributeStream(STREAM, TRAIL_FIELDS, true)
    private val ships = fleet()
    private var initialized = false
    private var running = false

    private val pose = Pose()
    private val matrix = Matrix4f()
    private val point = Vector3d()
    private val before = Vector3d()
    private val after = Vector3d()
    private val chord = Vector3d()
    private val engineRim = Vector3d()

    fun update(frame: NexusFrame) {
        val build = if (frame.inverse > 0) frame.build else 0.0
        if (build == 0.0 && !running) {
            // Entering an already active scene starts mid-pass. Entering normal space arms later arrivals.
            initialized = true
            return
        }

        val time = frame.time
        running = false
        for (ship in ships) {
            var active = build >= ship.lead.threshold
            if (ship.lag > 0) {
                if (!initialized) {
                    ship.want = active
                } else if (active != ship.want) {
                    ship.want = active
                    ship.flipAt = time + ship.lag
                }
                if (time < ship.flipAt) active = ship.active
            }

            if (active || ship.active) {
                val pass = ship.pass
                updatePass(ship, time)
                val phase = phase(pass)
                if (initialized && active != ship.active) {
                    if (active) {
                        ship.onPass = pass.index
                        ship.onPhase = phase
                        ship.onTime = time
                        ship.offTime = Double.NEGATIVE_INFINITY
                    } else if (phase != Phase.GAP) {
                        ship.offTime = if (phase == Phase.LEAVING) time - (pass.u - ARRIVAL - pass.cruise) else time
                        ship.offU = min(pass.u, ARRIVAL + pass.cruise)
                    }
                }
                if (active) draw(ship, pass, phase, time)
            }

            ship.active = active
            if (!active && ship.offTime != Double.NEGATIVE_INFINITY) {
                val elapsed = time - ship.offTime
                if (elapsed < DEPARTURE) depart(ship, ship.pass, ship.offU, elapsed)
                if (!trail(ship, ship.pass, ship.offU, elapsed, 0.0) && elapsed >= DEPARTURE) {
                    ship.offTime = Double.NEGATIVE_INFINITY
                }
            }
            if (active || ship.want || ship.offTime != Double.NEGATIVE_INFINITY) running = true
        }

        initialized = true
        val power = 0.35 + 0.4 * (0.3 + 0.7 * min(1.0, frame.rim))
        for (craft in crafts) {
            craft.solid.useInstances(craft.solidStream)
            craft.fade.useInstances(craft.fadeStream)
            craft.depth.instances = craft.fade.instances
            craft.depth.visible = craft.fade.visible
            if (craft.solid.visible) craft.solid.value("uPower", power)
            if (craft.fade.visible) craft.fade.value("uPower", power)
        }
        flares.useInstances(flareStream)
        plumes.useInstances(plumeStream)
        trails.useInstances(trailStream)
    }

    private fun NexusGpu.Node.useInstances(stream: NexusGpu.FloatData) {
        instances = stream.size / stream.components
        visible = instances > 0
    }

    // ========================================
    // Schedule
    // ========================================

    // Convoys keep MIN_GAP apart: the approach behind and the launch ahead of a jump need that much empty lane.
    private fun fleet(): Array<Ship> {
        val inner = floor(SHIPS * LANES[0].radius / (LANES[0].radius + LANES[1].radius)).toInt()
        val ships = Array(SHIPS) { i ->
            val lane = if ((i + 1) * inner / SHIPS > i * inner / SHIPS) LANES[0] else LANES[1]
            val h = hash(i, 1)
            val craft = when {
                h < BFF_SHARE -> 2
                h < BFF_SHARE + (1 - BFF_SHARE) * GOZANTI_SHARE -> 0
                else -> 1
            }
            Ship(i, lane, craft)
        }

        for ((index, lane) in LANES.withIndex()) {
            val list = ships.filter { it.lane === lane }
            val groups = ArrayList<List<Ship>>()
            var k = 0
            while (k < list.size) {
                val n = min(1 + floor(hash(k, 90L + index) * CONVOY).toInt(), list.size - k)
                groups.add(list.subList(k, k + n))
                k += n
            }

            val spacing = groups.map { group -> group.drop(1).map { CONVOY_SPACING + 6 * hash(it.index, 91) } }
            val spare = 2 * PI * lane.radius - spacing.sumOf { it.sum() } - groups.size * MIN_GAP
            val weights = groups.indices.map { 0.15 + hash(it, 92L + index) }
            val away = if (index == 0) -1.0 else 1.0
            var cursor = 2 * PI * lane.radius * hash(index, 93)
            for ((j, group) in groups.withIndex()) {
                val length = spacing[j].sum()
                var x = cursor + length
                for ((m, ship) in group.withIndex()) {
                    if (m > 0) x -= spacing[j][m - 1]
                    ship.lead = group[0]
                    ship.lag = LAG * m
                    ship.slot = lane.direction * x / lane.radius
                    ship.entryPhase = lane.direction * (ship.lead.slot - lane.entry)
                    // Sideways and in height only away from the other lane, so oncoming traffic keeps its clearance.
                    val reach = if (m > 0) 1.0 else 0.4
                    ship.radius = lane.radius + 0.35 * hash(ship.index, 94) * reach * away
                    ship.z = lane.z + 0.45 * hash(ship.index, 95) * reach * away
                }
                cursor += length + MIN_GAP + spare * weights[j] / weights.sum()
            }
        }
        return ships
    }

    // ZONES entry points turn slowly round the ring. A convoy drops in when its place passes one (up to 15 % of a
    // pass later), flies 40-65 degrees and is gone before the next entry, so the traffic gathers in ZONES stretches.
    // The outer lane's entries sit at the far end of the inner lane's stretches: its ships come back through them.
    private fun updatePass(ship: Ship, time: Double) {
        val lane = ship.lane
        val lead = ship.lead
        val t = time - ship.lag
        val pass = ship.pass
        if (t < pass.arrival || t >= pass.nextArrival) {
            fun arrival(k: Long) = (k * ZONE_STEP - ship.entryPhase) / lane.relative + 0.15 * lane.interval * hash(lead.index, 210 + k)
            var index = floor((ship.entryPhase + lane.relative * t) / ZONE_STEP).toLong()
            val at = arrival(index)
            if (at > t) {
                pass.nextArrival = at
                pass.arrival = arrival(--index)
            } else {
                pass.arrival = at
                pass.nextArrival = arrival(index + 1)
            }

            pass.index = index
            val arc = Math.toRadians(ZONE_ARC_MIN + (ZONE_ARC_MAX - ZONE_ARC_MIN) * hash(lead.index, 230 + index))
            pass.cruise = arc * lane.radius / lane.speed
            pass.start = ship.slot + lane.turn * (pass.arrival + ship.lag)
        }
        pass.u = t - pass.arrival
    }

    private fun phase(pass: Pass) = when {
        pass.u < ARRIVAL -> Phase.ARRIVING
        pass.u < ARRIVAL + pass.cruise -> Phase.CRUISING
        pass.u < ARRIVAL + pass.cruise + DEPARTURE -> Phase.LEAVING
        else -> Phase.GAP
    }

    private fun draw(ship: Ship, pass: Pass, phase: Phase, time: Double) {
        val samePass = ship.onPass == pass.index
        if (samePass && ship.onPhase == Phase.LEAVING) return

        when (phase) {
            Phase.ARRIVING -> arrive(ship, pass, pass.u)
            Phase.CRUISING -> arrive(ship, pass, if (samePass && ship.onPhase == Phase.CRUISING) time - ship.onTime else ARRIVAL)
            Phase.LEAVING -> depart(ship, pass, ARRIVAL + pass.cruise, pass.u - ARRIVAL - pass.cruise)
            Phase.GAP -> Unit
        }

        // A ship that came in with the rings mid-cruise has no trail behind it yet.
        val born = if (samePass && ship.onPhase == Phase.CRUISING) max(0.0, pass.u - (time - ship.onTime)) else 0.0
        val head = min(pass.u, ARRIVAL + pass.cruise)
        trail(ship, pass, head, pass.u - head, born)
    }

    // ========================================
    // Arrival: the hull pulls in from its stretched approach and joins the cruise.
    // ========================================

    private fun arrive(ship: Ship, pass: Pass, tau: Double) {
        val craft = crafts[ship.craft]
        val length = craft.length
        place(ship, pass, min(pass.u, ARRIVAL + pass.cruise))
        if (tau >= 0.21) {
            hull(craft, 0.0)
            engine(length, 0.0, 1.0)
            return
        }

        val p = tau / 0.21
        val remaining = (1 - p) * (1 - p)
        val range = length * 3.65
        val visibility = smooth(p / 0.26)
        flight(craft, -range * remaining, 1 + 1.45 * remaining, visibility, remaining, 0.025 * (2 * range / 0.21) * (1 - p) * visibility)
    }

    // ========================================
    // Departure: a 0.34 s recoil charge, then the whole ship launches and fades in flight.
    // ========================================

    private fun depart(ship: Ship, pass: Pass, u: Double, tau: Double) {
        if (tau >= DEPARTURE) return
        val craft = crafts[ship.craft]
        val length = craft.length
        place(ship, pass, u)
        val recoil = -length * 0.5

        // Hermite from the cruise speed to rest at the recoil point.
        val anchor = if (tau <= COIL) {
            val k = tau / COIL
            ship.lane.speed * COIL * (k - 2 * k * k + k * k * k) + recoil * (3 * k * k - 2 * k * k * k)
        } else {
            recoil
        }

        if (tau < 0.74) {
            val coil = tau <= COIL
            val k = if (coil) sat(tau / COIL) else sat((tau - COIL) / 0.38)
            val shockX = if (coil) anchor + length * lerp(0.55, 0.22, smooth(k)) else recoil - length * (0.12 + 0.5 * (1 - (1 - k).pow(2.2)))
            val alpha = if (coil) smooth(tau / 0.09) * (0.52 + 0.4 * k) else 1.05 * (1 - k).pow(1.85)
            val spread = 1 - (1 - k).pow(2.4)

            val coilSpread = if (coil) k.pow(1.3) else 0.0
            val rx = if (coil) lerp(0.86, 0.46, coilSpread) else lerp(0.42, 1.28, spread)
            val ry = if (coil) lerp(0.58, 0.32, coilSpread) else lerp(0.30, 0.88, spread)
            for (ring in 0 until (if (coil) 1 else 2)) {
                val scale = 1 - ring * 0.28
                for (side in 0..1) {
                    flare(
                        shockX + ring * length * 0.16,
                        length * scale * rx,
                        length * scale * ry,
                        0.78,
                        1.0,
                        if (side == 0) ACCENT else PRIMARY,
                        alpha * if (ring == 0) 1.0 else 0.72,
                        if (side == 0) (if (coil) 0.54 * k else 0.44 * (1 - k)) else 0.0,
                        side * PI,
                        if (coil) 0.052 else 0.062
                    )
                }
            }

            if (!coil && k < 0.65) {
                val f = 1 - k / 0.65
                flare(recoil - length * 0.15, length * 0.68 * (0.6 + 0.6 * (1 - f)), length * 0.42 * f, 1.0, 2.0, PRIMARY, 1.02 * f * f, 0.25, 0.0, 0.85 * f)
            }
        }

        if (tau <= COIL) {
            val charge = smooth(tau / COIL).pow(1.25)
            val compression = 0.34 * charge
            val wave = lerp(-0.6, 0.54, sat(tau / (COIL * 0.88)).pow(1.15))
            hull(
                craft,
                anchor,
                tailShift = compression * 0.6,
                noseShift = -compression * 0.4,
                tailGirth = 1 + compression * 0.52,
                noseGirth = 1 + compression * 0.36,
                heat = 0.18 * charge * charge,
                wave = wave,
                stretchHeat = 0.28
            )
            engine(length, anchor + compression * 0.6 * length, 1 + 2.6 * charge)
            return
        }

        val p = (tau - COIL) / 0.17
        if (p >= 1) return
        val range = length * 5.3
        // A launch impulse followed by positive acceleration of the whole ship.
        val travel = 0.5 * range * p * (1 + p)
        val speed = range / 0.17 * (0.5 + p)
        flight(craft, recoil + 0.034 * length + travel, lerp(0.66, 2.25, p * (2 - p)), 1 - smooth((p - 0.75) / 0.25), lerp(2.6 / 2.2, 0.85, p), min(travel, 0.028 * speed))
    }

    // The centre moves the instance; the stretch is about it, and the engine and its wake ride the stern.
    private fun flight(craft: Craft, center: Double, stretch: Double, visibility: Double, thrust: Double, wake: Double) {
        if (visibility <= 0.002) return
        val length = craft.length
        val half = 0.5 * (stretch - 1)
        val girth = 1 / sqrt(max(1.0, stretch))
        val compression = max(0.0, 1 - stretch)
        hull(
            craft,
            center,
            tailShift = -half,
            noseShift = half,
            tailGirth = girth + 0.52 * compression,
            noseGirth = girth + 0.36 * compression,
            heat = 0.18 * thrust,
            opacity = visibility,
            stretchHeat = 0.55 * thrust
        )

        val stern = center - length * half
        engine(length, stern, (1 + 2.2 * thrust) * visibility)
        if (wake <= 0.01) return
        val engineX = stern - 0.52 * length
        plume(engineX - wake, engineX, 0.26 * length * girth, 0.5 * thrust * visibility)
    }

    private fun engine(length: Double, shift: Double, strength: Double) {
        if (strength <= 0.01) return
        val r = min(0.42 * length, 1.25) * (0.8 + 0.2 * strength)
        val warm = sat((strength - 1) * 0.55)
        val rim = ENGINE.lerp(PRIMARY, warm, engineRim)
        flare(shift - length * 0.52, r, r * 0.78, 1.0, 2.0, rim, min(1.1, 0.7 * strength), 0.24, 0.0, sat((strength - 1) * 0.45))
    }

    // After the jump the trail's tail keeps moving up while it fades.
    private fun trail(ship: Ship, pass: Pass, head: Double, after: Double, born: Double): Boolean {
        val fade = 1 - smooth(after / 1.6)
        if (fade <= 0) return false
        val lane = ship.lane
        val length = crafts[ship.craft].length
        val span = lane.trailSpan
        val stern = head - 0.55 * length / lane.speed
        val tail = max(born, stern - span) + after
        if (stern - tail < 0.05) return false

        trailStream.put(ship.radius, pass.start + lane.turn * tail, pass.start + lane.turn * stern, ship.z)
        trailStream.put(0.13 * length, TRAIL_ALPHA * fade * (0.35 + 0.65 * sat((stern - tail) / span)))
        return true
    }

    // ========================================
    // Instances
    // ========================================

    private fun place(ship: Ship, pass: Pass, u: Double) {
        val lane = ship.lane
        val end = ARRIVAL + pass.cruise
        fun at(t: Double, out: Vector3d) {
            val angle = pass.start + lane.turn * t.coerceIn(0.0, end)
            out.set(ship.radius * cos(angle), ship.radius * sin(angle), ship.z)
        }
        at(u, pose.p)
        at(u - 0.3, before)
        at(u + 0.3, after)

        pose.x.set(after).sub(before)
        if (pose.x.lengthSquared() < 1e-10) pose.x.set(1.0, 0.0, 0.0)
        pose.x.normalize()
        chord.set(pose.p).sub(before)
        val turn = if (chord.lengthSquared() > 1e-8) chord.normalize().cross(pose.x).z else 0.0
        pose.up.set(0.0, 0.0, 1.0)
        pose.up.cross(pose.x, pose.y).normalize()
        pose.x.cross(pose.y, pose.up)

        // Banks into the lane's curve.
        val roll = (-turn * 8).coerceIn(-0.5, 0.5)
        if (roll != 0.0) {
            pose.y.rotateAxis(roll, pose.x.x, pose.x.y, pose.x.z)
            pose.up.rotateAxis(roll, pose.x.x, pose.x.y, pose.x.z)
        }
    }

    private fun world(along: Double): Vector3d = point.set(pose.x).mul(along).add(pose.p)

    private fun hull(
        craft: Craft,
        along: Double,
        tailShift: Double = 0.0,
        noseShift: Double = 0.0,
        tailGirth: Double = 1.0,
        noseGirth: Double = 1.0,
        heat: Double = 0.0,
        wave: Double = -9.0,
        opacity: Double = 1.0,
        stretchHeat: Double = 0.0
    ) {
        val l = craft.length.toFloat()
        val origin = world(along)
        matrix.set(
            (pose.x.x * l).toFloat(), (pose.x.y * l).toFloat(), (pose.x.z * l).toFloat(), 0f,
            (pose.y.x * l).toFloat(), (pose.y.y * l).toFloat(), (pose.y.z * l).toFloat(), 0f,
            (pose.up.x * l).toFloat(), (pose.up.y * l).toFloat(), (pose.up.z * l).toFloat(), 0f,
            origin.x.toFloat(), origin.y.toFloat(), origin.z.toFloat(), 1f
        )

        val stream = if (opacity < 1) craft.fadeStream else craft.solidStream
        stream.put(matrix)
        stream.put(tailShift, noseShift, tailGirth, noseGirth)
        stream.put(heat, wave, opacity, stretchHeat)
    }

    private fun flare(
        along: Double,
        rx: Double,
        ry: Double,
        span: Double,
        mode: Double,
        rim: Vector3d,
        alpha: Double,
        cushion: Double,
        rotation: Double,
        open: Double
    ) {
        if (alpha <= 0.002) return
        flareStream.put(world(along))
        flareStream.put(pose.y)
        flareStream.put(pose.up)
        flareStream.put(rx, ry, span, mode)
        flareStream.put(rim)
        flareStream.put(alpha, cushion, rotation, open)
    }

    // A parallel-sided wake with rounded ends, from the engine back.
    private fun plume(from: Double, to: Double, width: Double, alpha: Double) {
        if (alpha <= 0.002 || width <= 0.002) return
        plumeStream.put(world(from))
        plumeStream.put(world(to))
        plumeStream.put(width, alpha)
    }

    companion object {
        private const val STREAM = "transit-instances"
        private const val ARRIVAL = 0.90
        private const val DEPARTURE = 0.85
        private const val COIL = 0.34

        private const val SHIPS = 20
        private const val BFF_SHARE = 0.3
        private const val GOZANTI_SHARE = 0.5
        private const val CONVOY = 3
        private const val CONVOY_SPACING = 11.0
        private const val MIN_GAP = 36.0
        private const val LAG = 0.6

        // The arcs stay short enough that a convoy is gone before its next entry on either lane.
        private const val ZONES = 3
        private const val ZONE_STEP = 2 * PI / ZONES
        private const val ZONE_TURN = 2 * PI / 1200
        private const val ZONE_ARC_MIN = 40.0
        private const val ZONE_ARC_MAX = 65.0
        private val ZONE_START = 2 * PI * hash(7, 300)

        private const val TRAIL = 20.0
        private const val TRAIL_ALPHA = 1.04

        private val HULL_FIELDS = listOf("nexusM0" to 4, "nexusM1" to 4, "nexusM2" to 4, "nexusM3" to 4, "iDeform" to 4, "iStyle" to 4)
        private val FLARE_FIELDS = listOf(
            "iCenter" to 3,
            "iRight" to 3,
            "iUp" to 3,
            "iSize" to 4,
            "iColRim" to 3,
            "iMeta" to 4
        )
        private val PLUME_FIELDS = listOf("iA" to 3, "iB" to 3, "iPlume" to 2)
        private val TRAIL_FIELDS = listOf("iArc" to 4, "iTrail" to 2)

        private val LANES = arrayOf(Lane(44.0, 65.0, 1.0, 3.6), Lane(53.4, 70.7, -1.0, 4.2))

        private val PRIMARY = Vector3d(0.42, 0.84, 1.0)
        private val ACCENT = Vector3d(1.0, 0.80, 0.34)
        private val ENGINE = Vector3d(0.38, 0.86, 1.0)

        private fun hash(i: Int, k: Long): Double {
            val s = sin(i * 12.9898 + k * 78.233) * 43758.5453
            return s - floor(s)
        }

        private fun hash(i: Int, k: Int) = hash(i, k.toLong())

        private fun sat(x: Double) = x.coerceIn(0.0, 1.0)

        private fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t
    }
}
