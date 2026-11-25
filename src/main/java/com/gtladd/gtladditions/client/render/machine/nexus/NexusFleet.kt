package com.gtladd.gtladditions.client.render.machine.nexus

import com.gtladd.gtladditions.client.RenderUtils.rotationRadians
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.WHITE
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.fract
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.mix
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.polar
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.smooth
import com.gtladd.gtladditions.utils.SubspaceNexusTimeline
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.IEEErem
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

internal object NexusFleet {
    class Logistics(val transfer: DoubleArray, val dock: DoubleArray, val cargo: DoubleArray)

    class State(nodes: Map<String, NexusGpu.Node>) {
        val logistics = Logistics(DoubleArray(6), DoubleArray(6), DoubleArray(6))
        val azimuth = DoubleArray(6) { it * PI / 3 }
        val centers = Array(6) { polar(azimuth[it], 134.0, 0.0) }
        val bases = Array(6) {
            Matrix4f().translation(centers[it].x.toFloat(), centers[it].y.toFloat(), 0f).rotateZ(azimuth[it].toFloat()).scale(4f)
        }

        val bodies = Array(6) { ship -> Array(12) { piece -> nodes.getValue("ship.$ship.0.$piece") } }
        val plumes = Array(6) { ship -> Array(3) { plume -> nodes.getValue("fleet.ships.$ship.plumes.$plume.m") } }
        val beams = Array(6) { nodes.getValue("fleet.ships.$it.beam") }
        val cables = Array(6) { nodes.getValue("fleet.cables.meshes.$it.m") }
        val pads = Array(6) { ship ->
            arrayOf(nodes.getValue("dir.padL.items.$ship.flat.m"), nodes.getValue("dir.padL.items.$ship.shell.m"))
        }

        private val rig = NexusGpu.effects().getAsJsonObject("ship_rig").getAsJsonObject("preview")

        private val anchors = rig.getAsJsonObject("anchors").entrySet().associate { (name, value) ->
            val a = value.asJsonArray
            name to Vector3f(a[0].asFloat, a[1].asFloat, a[2].asFloat)
        }

        val thrusters = arrayOf("E", "W", "S").map { anchors.getValue("Thruster_$it") }.toTypedArray()
        val beacons = arrayOf("E", "W", "S").map { anchors.getValue("Beacon_$it") }.toTypedArray()

        val wheelPositions = Array(6) { i ->
            val a = rig.getAsJsonArray("wheels")[i].asJsonObject.getAsJsonArray("pos")
            Vector3f(a[0].asFloat, a[1].asFloat, a[2].asFloat)
        }

        val wheelRotations = Array(6) { i ->
            val a = rig.getAsJsonArray("wheels")[i].asJsonObject.getAsJsonArray("axle")
            Quaternionf().rotationTo(Vector3f(0f, 0f, 1f), Vector3f(a[0].asFloat, a[1].asFloat, a[2].asFloat))
        }
    }

    private val maxAcceleration = PI * PI * 291 / (2 * 0.41.pow(2) * SubspaceNexusTimeline.PERIOD.pow(2))

    fun update(state: State, frame: NexusFrame, batches: NexusBatches): Logistics {
        val f = frame
        val power = f["power"]
        val transfer = state.logistics.transfer
        val dock = state.logistics.dock
        val cargo = state.logistics.cargo
        transfer.fill(-1.0)
        dock.fill(0.0)
        cargo.fill(0.0)

        for (i in 0..5) {
            val ship = f.ships[i]
            if (ship.docked && !ship.continuous && ship.recallProgress < 0) {
                cargo[i] = 1.0
                dock[i] = 1.0
            } else {
                val u = ship.phase
                when {
                    u < 0.09 -> {
                        cargo[i] = smooth(u / 0.09)
                        dock[i] = 1.0
                    }
                    u < 0.5225 -> cargo[i] = 1.0
                    else -> {
                        transfer[i] = (u - 0.5225) * SubspaceNexusTimeline.PERIOD
                        cargo[i] = (1 - transfer[i] / 1.2).coerceIn(0.0, 1.0)
                    }
                }

                if (ship.recallProgress >= 0) {
                    val w = ship.recallProgress
                    dock[i] = max(dock[i] * (1 - w), smooth((w - 0.82) / 0.18))
                    cargo[i] = max(cargo[i] * (1 - w), if (w > 0.95) 1.0 else 0.0)
                    if (w > 0.05) transfer[i] = -1.0
                }
            }
        }

        val delta = 1.0 / 30
        val previous = SubspaceNexusTimeline.sampleShips(f.animationState, (f.time - delta) * 20, f.eventTick)
        val next = SubspaceNexusTimeline.sampleShips(f.animationState, (f.time + delta) * 20, f.eventTick)
        fun anchor(position: Vector3f, matrix: Matrix4f): Vector3d = Vector3d(Vector3f(position).mulPosition(matrix))

        for (i in 0..5) {
            val ship = f.ships[i]
            val center = state.centers[i].set(state.centers[i].x, state.centers[i].y, ship.z)
            val base = state.bases[i].m32(ship.z.toFloat())
            val acceleration = (next[i].z - 2 * ship.z + previous[i].z) / (delta * delta)
            val thrust = if (power > 0.05 && !ship.docked) (acceleration / maxAcceleration).coerceIn(0.0, 1.0) else 0.0
            val band = 0.16 + 0.84 * power
            val beaconPhase = fract((f.time + if (f["paradox"] > 0.5) 0.0 else i * 0.37) / 2) * 2
            val beacon = if (power > 0.05 && (beaconPhase in 0.0..0.08 || beaconPhase in 0.3..0.38)) 1.6 else 0.12

            for (piece in 0 until 12) {
                val node = state.bodies[i][piece]
                node.visible = true
                node.matrix.set(base)
                if (piece >= 6) {
                    val position = state.wheelPositions[piece - 6]
                    node.matrix.translate(position.x, position.y, position.z).rotate(state.wheelRotations[piece - 6])
                        .rotateZ((-(ship.z - 69) / (0.1035 * 4)).toFloat())
                }

                val kind = piece
                val intensity = when (kind) {
                    1 -> band * (1 + 0.35 * f["paradox"])
                    2 -> band * 0.5 + if (transfer[i] >= 0) 2.4 * exp(-transfer[i] / 0.8) else 0.0
                    3 -> if (power > 0.05) 0.08 + 1.05 * cargo[i] + if (transfer[i] in 0.0..0.35) 1.2 else 0.0 else 0.04
                    4 -> 0.2 + 1.6 * thrust
                    5 -> beacon
                    else -> 1.0
                }

                node.value("uI", intensity)
                node.value("uNeg", f.inverse)
                node.value("uNegGlow", if (kind in 1..5) 0.75 else 0.0)
            }

            for (plume in state.thrusters.indices) {
                val pos = anchor(state.thrusters[plume], base)
                val node = state.plumes[i][plume]
                node.visible = thrust > 0.02
                node.matrix.translation(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                    .scale(1.25f, 1.25f, (-(2.5 + 12.5 * thrust)).toFloat())
                node.value("uAlpha", 0.6 * thrust * f.glow)
                node.value("uHead", 1.0)
                if (node.visible) {
                    batches.point(Vector3d(pos).add(0.0, 0.0, -0.6), 4.2 * (0.5 + thrust), Vector3d(0.831, 0.969, 1.0), 0.55 * thrust)
                }

                if (beacon > 1) batches.point(anchor(state.beacons[plume], base), 3.2, Vector3d(1.0, 0.78, 0.45), 0.75)
            }

            val beam = state.beams[i]
            beam.visible = power * dock[i] > 0.01
            beam.matrix.translation(center.x.toFloat(), center.y.toFloat(), 34f).scale(6.2f, 6.2f, (ship.z - 2.88 - 34).toFloat())
            beam.value("uAlpha", 0.17 * power * dock[i] * f.glow)
            beam.value("uHead", 0.0)
            beam.value("uDash", fract(f.time * 9.0 * 0.22), 0.22)

            val padAlpha = if (power < 0.02) 0.072 else (0.32 + (0.85 - 0.32) * dock[i]) * max(power, 0.3)
            val padClock = if (f.state == "unlock" && f.age in 10.0..16.0) f.animationState.getDouble("since") / 20.0 + 10.0 else f.time
            val burst = if (f.burst in 0.0..1.4) f.burst / 1.4 else -1.0

            for (part in 0..1) {
                val pad = state.pads[i][part]
                pad.visible = part == 0 || burst >= 0
                pad.value("uRot", fract(padClock * 0.02), fract(-padClock * 0.05))
                pad.value("uA", padAlpha)
                pad.value("uB", burst)
            }

            val cable = state.cables[i]
            cable.visible = true
            cable.value("uFlowPhase", rotationRadians(f.time * 6.0 * 0.42))
            cable.value("uCablePhase", fract(f.time * 0.45), (f.time * 0.22).IEEErem(2 * PI))
            cable.value(
                "uBody",
                mix(Vector3d(0.365, 0.4, 0.447), Vector3d(0.043, 0.055, 0.078), f.inverse)
                    .let { doubleArrayOf(it.x, it.y, it.z) }
                    .let { FloatArray(3) { i -> it[i].toFloat() } }
            )
            cable.value("uRimGain", 0.0)
            cable.value(
                "uOn",
                (1 - f["railFade"]) * max(0.15, power) * (1 - 0.6 * f["railDim"]) *
                    (1 + 0.25 * f["paradox"] + 0.8 * f["flash"]) * f.coreBrightness
            )
            cable.value("uSkyCol", f.skyColor.x, f.skyColor.y, f.skyColor.z)
            cable.value("uFog", 0.8 * f.ambient)
            cable.value("uSweepZ", if (f["railSweep"] > 0 && f["railSweep"] < 1) 29 + 421 * f["railSweep"] else 100000.0)
            cable.value(
                "uShip",
                ship.z + 1.4,
                (0.18 + 0.82 * (abs(ship.velocity) / 22).coerceIn(0.0, 1.0)) * power * (1 - f["railFade"]),
                1.8,
                0.0
            )
            cable.value(
                "uPulse",
                if (transfer[i] >= 0) ship.z + 7.2 + 170 * transfer[i] else -999.0,
                22.0,
                if (transfer[i] >= 0) 0.9 * power else 0.0,
                0.0
            )
            cable.value("uBurst", if (f.burst >= 0) 34 + 260 * f.burst else -999.0, if (f.burst >= 0) 0.95 * (1 - f.burst / 2.2) else 0.0)

            if (transfer[i] in 0.0..3.0) {
                val k = transfer[i] / 3
                val radius = 3 + 35 * (1 - (1 - k).pow(3))
                val point = Vector3d(center.x, center.y, 364.0)
                batches.ring(point, radius, 0.35 + 1.25 * k, Vector3d(0.78, 0.961, 1.0), 0.55 * (1 - k).pow(2) * power)
                if (k < 0.5) batches.ring(point, radius * 0.55, 0.25, WHITE, 0.33 * (1 - 2 * k) * power)
            }
        }

        return state.logistics
    }
}
