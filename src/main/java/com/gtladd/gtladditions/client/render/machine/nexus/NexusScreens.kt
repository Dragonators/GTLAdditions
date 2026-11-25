package com.gtladd.gtladditions.client.render.machine.nexus

import com.gtladd.gtladditions.client.RenderUtils.rotationRadians
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.GOLD
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.ICE
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.WHITE
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.fract
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.mix
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.polar
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.smooth
import com.gtladd.gtladditions.utils.SubspaceNexusLayout
import com.gtladd.gtladditions.utils.SubspaceNexusTimeline
import org.joml.Vector3d
import kotlin.math.*

internal object NexusScreens {
    private val sites = doubleArrayOf(-30.0, 90.0, 210.0)
    private val ports = arrayOf(doubleArrayOf(113.0, -65.0), doubleArrayOf(0.0, 130.0), doubleArrayOf(-113.0, -65.0))

    private val mapRadii = arrayOf(
        doubleArrayOf(0.0, 0.0),
        doubleArrayOf(17.0, 1.32),
        doubleArrayOf(25.0, 3.7),
        doubleArrayOf(31.5, 5.6),
        doubleArrayOf(38.0, 7.5),
        doubleArrayOf(44.5, 9.4),
        doubleArrayOf(60.0, 11.2)
    )

    private fun mapRadius(r: Double): Double {
        for (i in 1 until mapRadii.size) {
            if (r <= mapRadii[i][0]) {
                val a = mapRadii[i - 1]
                val b = mapRadii[i]
                return a[1] + (b[1] - a[1]) * (r - a[0]) / (b[0] - a[0])
            }
        }
        return 11.2
    }

    class State(nodes: Map<String, NexusGpu.Node>) {
        val order = LinkedHashMap<NexusGpu.Node, Double>()
        val ages = DoubleArray(3)
        val markers = FloatArray(9)
        val packets = FloatArray(3)
        val shipPositions = FloatArray(6)
        val shipPhases = FloatArray(6)
        val planets = Array(3) { FloatArray(16) }
        val planetInfo = Array(3) { FloatArray(16) }
        val glitchClocks = Array(3) { FloatArray(NexusScreenClock.SIZE) }
        val modules = Array(3) { FloatArray(144) }
        val screens = Array(3) { nodes.getValue("dir.screens.$it.m") }
        val flows = Array(3) { nodes.getValue("dir.r9.flow.items.$it.m") }
        val dispatchLine = nodes.getValue("dir.disp.line.mesh")
        val markerNodes = Array(9) { nodes.getValue("dir.markers.$it.s") }
        val markerState = FloatArray(36)

        private val connections = LongArray(3)
        private val activity = LongArray(3)
        private var modulesInitialized = false

        private val moduleOrder = SubspaceNexusLayout.metadata.getAsJsonArray("moduleScreenOrder")?.let { order ->
            Array(3) { k -> IntArray(144) { index -> order[(k + index / 48) % 3].asJsonArray[index % 48].asInt } }
        }

        fun updateModules(frame: NexusFrame) {
            val order = moduleOrder ?: return
            var changed = !modulesInitialized

            for (i in 0..2) {
                val connected = frame.machine.moduleConnections.getOrElse(i) { 0L }
                val active = frame.machine.moduleActivity.getOrElse(i) { 0L }
                if (connections[i] != connected || activity[i] != active) changed = true
                connections[i] = connected
                activity[i] = active
            }

            if (changed) {
                for (k in 0..2) {
                    for (index in 0 until 144) {
                        val module = order[k][index]
                        val bit = 1L shl (module % 64)
                        modules[k][index] = when {
                            activity[module / 64] and bit != 0L -> 1f
                            connections[module / 64] and bit != 0L -> 0.35f
                            else -> 0f
                        }
                    }
                }
                modulesInitialized = true
            }

            for (k in 0..2) screens[k].value("uModules", modules[k])
        }
    }

    fun update(state: State, f: NexusFrame, logistics: NexusFleet.Logistics, b: NexusBatches): Map<NexusGpu.Node, Double> {
        state.updateModules(f)
        val order = state.order
        val on = f["power"] * (1 - f["eFade"])

        val ages = state.ages
        for (i in ages.indices) {
            val age = fract((f.time - i * 1.39626) / 4.18879) * 4.18879
            val stateAge = (f.time - age) - f.animationState.getDouble("since") / 20.0
            ages[i] = if (
                (
                    if (stateAge >= 0) {
                        SubspaceNexusTimeline.sampleValue(f.animationState, stateAge, "packets")
                    } else {
                        f.animationState.getCompound("from").getDouble("packets")
                    }
                    ) > 0.5
            ) {
                age
            } else {
                -1.0
            }
        }

        val markers = state.markers
        for (index in markers.indices) {
            val base = if (index < 6) {
                if (f.ships[index].velocity > 0) 0.75 else 0.35
            } else {
                0.35
            }

            val age = if (index < 6) logistics.transfer[index] else ages[index - 6]
            markers[index] = ((base + if (age in 0.0..0.6) 1.4 * (1 - age / 0.6) else 0.0) * (1 - f["markerFade"])).toFloat()
        }

        val packets = state.packets
        for (i in packets.indices) packets[i] = if (ages[i] in 0.0..1.5) ((1 - cos(PI * ages[i] / 1.5)) * 0.5).toFloat() else -1f

        for (i in 0..5) {
            state.shipPositions[i] = ((f.ships[i].z - 69) / 291).toFloat()
            state.shipPhases[i] = if (f.ships[i].docked) -1f else f.ships[i].phase.toFloat()
        }

        for (k in 0..2) {
            val az = Math.toRadians(sites[k])
            val rot = Math.toRadians(sites[k] - 90)
            val x = ports[k][0]
            val y = ports[k][1]
            val scan = if (ages[k] >= 1.5) ages[k] - 1.5 else 99.0
            val flash = if (scan < 0.6) 1 - scan / 0.6 else 0.0

            val screen = state.screens[k]
            screen.visible = on > 0.01
            screen.value("uAlpha", on)
            screen.value("uScan", scan)
            screen.value("uShow", f["screenShow"])
            screen.value("uCount", f["countdown"])
            screen.value("uTear", f["tear"])
            screen.value("uInv", f.inverse)
            screen.value("uStarA", max(f.inverse, 0.6))
            screen.value("uGlitch", min(1.0, on * 2))
            NexusScreenClock.sample(f.time, 0.4 + k * 1.37, state.glitchClocks[k])
            screen.value("uGlitchClock", state.glitchClocks[k])
            screen.value("uScar", 0.0)

            screen.value("uShips", state.shipPositions)
            screen.value("uShipU", state.shipPhases)
            screen.value("uMk", markers)
            screen.value("uPk", packets)
            screen.value("uBelt", rotationRadians(2 * PI * f.beltTime / 100.531 - rot))
            screen.value("uOrbA", min(1 - f["orbitFade"], f["orbitDraw"]) * (1 - f.vanish))
            screen.value(
                "uNum",
                4.18879 - fract((f.time - k * 1.39626) / 4.18879) * 4.18879,
                fract(f.time / SubspaceNexusTimeline.PERIOD) * SubspaceNexusTimeline.PERIOD,
                0.0
            )

            val planets = state.planets[k]
            val planetInfo = state.planetInfo[k]

            for (i in 0..3) {
                val p = f.fall[i]
                val fade = (1 - smooth((p - 0.8) / 0.2)) * (1 - 0.3 * p.pow(1.6))
                val radius = hypot(f.planets[i].x, f.planets[i].y)
                val angle = f.orbitAngle(i) - rot
                val mapped = mapRadius(radius)

                planets[i * 4] = (23.562 + 1.04 * mapped * cos(angle)).toFloat()
                planets[i * 4 + 1] = (18 + 1.04 * mapped * sin(angle)).toFloat()
                planets[i * 4 + 2] = max(0.001, doubleArrayOf(0.62, 0.74, 1.02, 1.2)[i] * 1.04.pow(0.75) * fade).toFloat()
                planets[i * 4 + 3] = (min(1 - f["orbitFade"], f["orbitDraw"]) * if (fade > 0.01) 1 else 0).toFloat()
                planetInfo[i * 4] = rotationRadians(angle)
                planetInfo[i * 4 + 1] = mapRadius(f.orbitRadii[i]).toFloat()
                planetInfo[i * 4 + 2] = (angle * 2.2 + f.time * 0.05 * (i + 1)).IEEErem(8 * PI).toFloat()
                planetInfo[i * 4 + 3] = rotationRadians(2 * PI * f.chartTime / 50.265 + 1.2)
            }

            screen.value("uPl", planets)
            screen.value("uPlB", planetInfo)
            order[screen] = if ((f.camera.x - 135 * cos(az)) * cos(az) + (f.camera.y - 135 * sin(az)) * sin(az) > 0) 18.0 else -0.5

            val flow = state.flows[k]
            flow.value("uFlowPhase", fract(f.time * 18.0 / 36.0))
            flow.visible = on > 0.003
            val color = mix(Vector3d(0.749, 0.941, 1.0), GOLD, f.inverse * 0.7)
            flow.value("uA", 0.42 * on)
            flow.value("uCol", color.x, color.y, color.z)
            flow.value("uPool", flash)
            val packetS = if (packets[k] >= 0) 63 + (hypot(x, y) + 22 - 63) * packets[k] else 0.0
            flow.value("uPk", packetS, if (packets[k] >= 0) 0.95 * on * 0.72 else 0.0, 0.0)

            val projector = mix(Vector3d(0.8, 0.97, 1.0), GOLD, f.inverse)
            b.line(Vector3d(x, y, 22.1), Vector3d(x, y, 22.6), 1.3, projector, 0.0, 1.25, (0.42 + 0.9 * flash) * on)
            b.line(Vector3d(x, y, 22.6), Vector3d(x, y, 28.6), 1.25, projector, (0.42 + 0.9 * flash) * on, 0.8, (0.5 + 0.9 * flash) * on)
            b.ring(
                Vector3d(x, y, 28.6),
                1.5,
                0.22,
                mix(Vector3d(0.85, 0.98, 1.0), GOLD, f.inverse),
                (0.55 + 0.8 * flash) * on,
                segments = 3.0,
                gap = 0.15,
                rotation = f.time * 0.08
            )
            b.point(
                Vector3d(x, y, 29.0),
                4 + 3 * flash,
                mix(Vector3d(0.9, 1.0, 1.0), Vector3d(1.0, 0.9, 0.7), f.inverse),
                (0.6 + 0.6 * flash) * on
            )

            for (a in doubleArrayOf(az - Math.toRadians(14.0), az + Math.toRadians(14.0))) {
                for (z in doubleArrayOf(40.0, 92.0)) {
                    val gain = 0.17 * on * (1 + 1.5 * flash)
                    b.line(
                        Vector3d(x, y, 28.9),
                        polar(a, 135.0, z),
                        0.5,
                        mix(Vector3d(0.75, 0.95, 1.0), GOLD, f.inverse),
                        gain * 0.6,
                        0.9,
                        gain
                    )
                }
            }
        }

        dispatch(state, f, markers, ages, logistics)
        return order
    }

    private fun dispatch(
        state: State,
        f: NexusFrame,
        markers: FloatArray,
        ages: DoubleArray,
        logistics: NexusFleet.Logistics
    ) {
        val fade = 1 - f["markerFade"]
        val line = state.dispatchLine
        line.visible = fade > 0.003
        line.value("uAlpha", 0.68 * fade * (1 + f["ack"]))
        val color = mix(ICE, WHITE, min(1.0, f["ack"] * 3))
        line.value("uCol", color.x, color.y, color.z)

        val markerState = state.markerState
        for (i in 0..8) {
            val angle = if (i < 6) i * PI / 3 else Math.toRadians(sites[i - 6])
            markerState[i * 4] = angle.toFloat()
            markerState[i * 4 + 1] = if (i < 6 && f.ships[i].velocity > 0) 1f else 0f
            markerState[i * 4 + 2] = if (i < 6) 0.052f else 0.042f
            markerState[i * 4 + 3] = (if (i < 6) logistics.transfer[i] else ages[i - 6]).toFloat()

            val marker = state.markerNodes[i]
            marker.visible = fade > 0.001
            marker.matrix.scale(smooth(fade).toFloat())
            marker.value("uInv", f.inverse)
            marker.value("uStarR", f.starRadius)
            marker.value("uE", (0.8 + 0.7 * markers[i] + 2.0 * f["ack"]) * fade)
            marker.value("uIce", color.x, color.y, color.z)
        }

        line.value("uM", markerState)
    }
}
