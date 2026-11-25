package com.gtladd.gtladditions.client.render.machine.nexus

import com.google.gson.JsonArray
import com.gtladd.gtladditions.client.RenderUtils.rotationRadians
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.GOLD
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.ICE
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.STAR
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.WHITE
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.fract
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.mix
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.polar
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.smooth
import org.joml.Matrix3f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.*

internal object NexusPlanets {
    class State(nodes: Map<String, NexusGpu.Node>) {
        val definitions: JsonArray = NexusGpu.manifest().getAsJsonArray("planetDefinitions")
        val bodies = Array(4) { index ->
            nodes.getValue(if (index == 0) "dir.sets.PM.bodies.0.xSk.mesh" else "dir.sets.PM.bodies.$index.mesh")
        }

        val ring = nodes.getValue("dir.sets.PM.bodies.2.ring")
        val moons = Array(definitions[2].asJsonObject.getAsJsonArray("moons").size()) {
            nodes.getValue("dir.sets.PM.bodies.2.moons.$it.mesh")
        }

        val orbits = Array(4) { nodes.getValue("dir.olines.items.$it.m") }
        val starkillerGlow = nodes.getValue("dir.sets.PM.bodies.0.xSk.glow.mesh")
        val deathStar = Array(6) { nodes.getValue("deathStar.$it") }
        val traffic = Array(3) { nodes.getValue("dir.sets.PM.x.lines.items.$it.m") }
        val trafficWheel = nodes.getValue("traffic.wheel.0")
        val links = nodes.getValue("dir.links.mesh")
        val trails = nodes.getValue("dir.trails.mesh")
    }

    fun update(state: State, f: NexusFrame, batches: NexusBatches) {
        val definitions = state.definitions
        val fade = 1 - f["orbitFade"]
        val size = 0.86 * 0.65
        val scales = DoubleArray(4) { i ->
            size * min(fade, f["orbitDraw"]) * (1 - smooth((f.fall[i] - 0.8) / 0.2)) * (1 - 0.3 * f.fall[i].pow(1.6))
        }

        val radii = DoubleArray(4) { definitions[it].asJsonObject.get("r").asDouble * scales[it] }
        val neg = f.inverse * (1 - 0.5.pow(1.6))

        fun light(node: NexusGpu.Node) {
            node.value("uNeg", neg)
            node.value("uRimG", f.inverse)
            node.value("uAmb", 0.06 + 0.05 * f.ambient)
            node.value("uStarR", f.starRadius)
            node.value("uLight", 0.902, 0.902, 1.0)
            node.value("uAmb", 0.06 + 0.05 * f.ambient)
        }

        for (i in 0..3) {
            val d = definitions[i].asJsonObject
            val center = f.planets[i]
            val scale = scales[i]
            val a = Math.toRadians(f.planetAngles[i]) + 2 * PI * (0.55 + 0.3 * i) * f.fall[i].pow(2)

            if (i == 0) {
                starkiller(state, f, center, radii[i], a, scale > 0.01, neg)
            } else {
                val node = state.bodies[i]
                node.visible = scale > 0.01
                node.matrix.translation(center.x.toFloat(), center.y.toFloat(), center.z.toFloat()).scale(max(0.001, scale).toFloat())
                node.value("uSpin", Matrix3f().rotationZ(rotationRadians(a * (d.get("spin")?.asDouble ?: 4.0) + i)).get(FloatArray(9)))
                light(node)
            }

            if (i == 2) {
                val ring = state.ring
                val spec = d.getAsJsonObject("ring")
                val tilt = Math.toRadians(spec.get("tilt").asDouble)
                val normal = Vector3f(0f, -sin(tilt).toFloat(), cos(tilt).toFloat()).rotateZ(rotationRadians(a * 0.25))
                ring.visible = scale > 0.01
                ring.matrix.translation(center.x.toFloat(), center.y.toFloat(), center.z.toFloat())
                    .rotate(Quaternionf().rotationTo(Vector3f(0f, 0f, 1f), normal))
                    .scale(max(0.001, scale).toFloat())
                ring.value("uPlanetC", center.x, center.y, center.z)
                ring.value("uPlanetR", radii[i])
                light(ring)

                val planet = state.bodies[2]
                planet.value("uRingN", normal.x.toDouble(), normal.y.toDouble(), normal.z.toDouble(), 1.0)
                planet.value("uRingC", center.x, center.y, center.z)
                planet.value("uRingR", spec.get("rIn").asDouble * scale, spec.get("rOut").asDouble * scale)

                d.getAsJsonArray("moons").forEachIndexed { index, moonData ->
                    val md = moonData.asJsonObject
                    val ma = 2 * PI * f.chartTime / md.get("period").asDouble + index * 2.1
                    val n = Vector3d(normal)
                    val u = Vector3d(1.0, 0.0, 0.0).fma(-n.x, n).normalize()
                    val v = Vector3d(n).cross(u)
                    val orbit = md.get("d").asDouble * scale
                    val p = if (md.get("plane")?.asBoolean == true) {
                        Vector3d(center).fma(orbit * cos(ma), u).fma(orbit * sin(ma), v)
                    } else {
                        Vector3d(center).add(orbit * cos(ma), orbit * sin(ma), 0.5 * sin(ma) * scale)
                    }

                    val moon = state.moons[index]
                    moon.visible = scale > 0.01
                    moon.matrix.translation(p.x.toFloat(), p.y.toFloat(), p.z.toFloat()).scale(max(0.001, scale).toFloat())
                    moon.value("uSpin", Matrix3f().rotationZ(rotationRadians(ma * 0.5)).get(FloatArray(9)))
                    light(moon)
                }
            }

            val orbit = state.orbits[i]
            val alpha = 0.3 * fade * (1 + 0.4 * f.inverse) * (1 - f.vanish) * 2.6
            orbit.visible = alpha > 0.003
            orbit.value("uA", alpha)
            orbit.value("uR", f.orbitRadii[i] * (1 - 0.08 * f.vanish.pow(2)))
            orbit.value("uTh0", f.planetAngles[i] / 360)
            orbit.value("uArc", f.planetAngles[i] / 360 - 0.02, f["orbitDraw"])
            orbit.value(
                "uGap",
                if (f.fall[i] > 0.001) {
                    -1.0
                } else {
                    radii[i] * if (i == 2) d.getAsJsonObject("ring").get("rOut").asDouble / d.get("r").asDouble * 1.04 else 1.04
                }
            )
            orbit.value("uDash", if (f.vanish > 0.002) 96.0 + 24 * i else 0.0, 1 - 0.82 * f.vanish)
            val color = mix(Vector3d(0.91, 0.769, 0.518), GOLD, f.inverse)
            orbit.value("uCol", color.x, color.y, color.z)
        }

        if (scales[1] > 0.01) traffic(state, f, radii[1], scales[1] / size, batches)
        deathStar(state, f, radii[3], scales[3], neg)

        linksAndTrails(state, f, radii, scales, batches)
    }

    private fun starkiller(
        state: State,
        f: NexusFrame,
        c: Vector3d,
        radius: Double,
        a: Double,
        visible: Boolean,
        neg: Double
    ) {
        val out = Vector3d(cos(a), sin(a), 0.0)
        val tangent = Vector3d(-sin(a), cos(a), 0.0)
        val yaw = Math.toRadians(25.0)
        val pitch = Math.toRadians(14.0)
        val roll = Math.toRadians(16.0)
        val forward = Vector3d(out).mul(cos(yaw)).fma(sin(yaw), tangent)
        val horizontal = Vector3d(tangent).mul(cos(yaw)).fma(-sin(yaw), out)
        val ex = Vector3d(forward).mul(cos(pitch)).add(0.0, 0.0, -sin(pitch))
        val up = Vector3d(ex).cross(horizontal)
        val ey = Vector3d(horizontal).mul(cos(roll)).fma(sin(roll), up)
        val ez = Vector3d(ex).cross(ey)

        val node = state.bodies[0]
        node.visible = visible
        node.matrix.translation(c.x.toFloat(), c.y.toFloat(), c.z.toFloat()).scale(max(0.001, radius).toFloat())
        node.value("uC", c.x, c.y, c.z)
        node.value("uS", radius)
        node.value("uStarR", f.starRadius)
        node.value("uNeg", neg)
        node.value("uRimG", f.inverse)
        node.value("uAmb", 0.06 + 0.05 * f.ambient)
        node.value(
            "uW2L",
            floatArrayOf(
                ex.x.toFloat(),
                ey.x.toFloat(),
                ez.x.toFloat(),
                ex.y.toFloat(),
                ey.y.toFloat(),
                ez.y.toFloat(),
                ex.z.toFloat(),
                ey.z.toFloat(),
                ez.z.toFloat()
            )
        )

        val face = max(0.0, Vector3d(f.camera).sub(c).normalize().dot(ex))
        NexusCenter.sprite(
            state.starkillerGlow,
            Vector3d(c).fma(radius * 1.03, ex),
            radius * 1.1,
            Vector3d(1.0, 0.227, 0.11),
            if (visible) 0.5 * face * face * (1 - 0.55 * neg) else 0.0
        )
    }

    private fun deathStar(state: State, f: NexusFrame, parentRadius: Double, scale: Double, neg: Double) {
        val az = Math.toRadians(200.0)
        val u = Vector3d(cos(az), sin(az), 0.0)
        val n = Vector3d(0.0, 0.0, 1.0).rotateAxis(Math.toRadians(-18.0), u.x, u.y, u.z)
        val v = Vector3d(n).cross(u)
        val a = 2 * PI * f.chartTime / 50.265 + Math.toRadians(300.0)
        val c = Vector3d(f.planets[3]).fma(2.1 * parentRadius * cos(a), u).fma(2.1 * parentRadius * sin(a), v)

        val pole = Vector3d(n).rotateAxis(Math.toRadians(12.0), u.x, u.y, u.z)
        val x = Vector3d(v).fma(-v.dot(pole), pole).normalize()
        val z = Vector3d(x).cross(pole)
        val basis = Matrix3f().setColumn(0, Vector3f(x.x.toFloat(), x.y.toFloat(), x.z.toFloat()))
            .setColumn(1, Vector3f(pole.x.toFloat(), pole.y.toFloat(), pole.z.toFloat()))
            .setColumn(2, Vector3f(z.x.toFloat(), z.y.toFloat(), z.z.toFloat()))

        for (i in 0..5) {
            val node = state.deathStar[i]
            node.visible = scale > 0.01
            node.matrix.translation(c.x.toFloat(), c.y.toFloat(), c.z.toFloat())
                .rotate(Quaternionf().setFromNormalized(basis))
                .rotateY(rotationRadians(Math.toRadians(44 * f.chartTime)))
                .scale(max(0.001, 1.45 * scale * 0.9).toFloat())
            node.value("uStarR", f.starRadius)
            node.value("uNeg", neg)
            node.value("uRimG", f.inverse)
            node.value("uAmb", 0.06 + 0.05 * f.ambient)
        }
    }

    private fun traffic(state: State, f: NexusFrame, radius: Double, visible: Double, b: NexusBatches) {
        val c = f.planets[1]
        val color = mix(ICE, GOLD, f.inverse)
        val lanes = listOf(
            doubleArrayOf(1.32, 28.0, 20.0, 14.0, 0.1, 3.0),
            doubleArrayOf(1.48, -52.0, 140.0, 21.0, 0.55, 2.0),
            doubleArrayOf(1.66, 76.0, 260.0, 31.0, 0.8, 1.0)
        )

        for ((i, lane) in lanes.withIndex()) {
            val az = Math.toRadians(lane[2])
            val u = Vector3d(cos(az), sin(az), 0.0)
            val n = Vector3d(0.0, 0.0, 1.0).rotateAxis(Math.toRadians(lane[1]), u.x, u.y, u.z)
            val v = Vector3d(n).cross(u)
            val r = lane[0] * radius
            val phase = fract(f.time / lane[3] + lane[4])

            val node = state.traffic[i]
            node.visible = true
            node.value("uC", c.x, c.y, c.z)
            node.value("uU", u.x, u.y, u.z)
            node.value("uV", v.x, v.y, v.z)
            node.value("uR", r)
            node.value("uTh0", phase)
            node.value("uA", 0.95 * visible)
            node.value("uCol", color.x, color.y, color.z)

            for (sat in 0 until lane[5].toInt()) {
                val a = 2 * PI * (phase - sat * 0.05)
                val p = Vector3d(c).fma(r * cos(a), u).fma(r * sin(a), v)
                b.point(
                    p,
                    (if (sat == 0) 1.0 else 0.55) * 0.06 * radius,
                    mix(color, WHITE, 0.6),
                    (if (sat == 0) 1.0 else 0.55) * 0.95 * visible
                )
            }

            if (i == 2) {
                val a = 2 * PI * fract(f.time / lane[3] * 0.5 + 0.31)
                val p = Vector3d(c).fma(r * cos(a), u).fma(r * sin(a), v)
                val q = Quaternionf().rotationTo(Vector3f(0f, 0f, 1f), Vector3f(n.x.toFloat(), n.y.toFloat(), n.z.toFloat()))
                val size = 0.2 * radius

                val wheel = state.trafficWheel
                wheel.visible = true
                wheel.matrix.translation(p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
                    .rotate(q)
                    .rotateZ(rotationRadians(f.time * 0.6))
                    .scale(size.toFloat())
                wheel.value("uAmb", 0.06 + 0.05 * f.ambient)
                wheel.value("uW", size)
                wheel.value("uPC", c.x, c.y, c.z)
                wheel.value("uPR", radius)
                wheel.value("uNeg", f.inverse * (1 - 0.5.pow(1.6)))
                wheel.value("uRimG", f.inverse)

                for (j in 0..3) {
                    val d = polar(f.time * 0.6 + j * PI / 2, 1.0, 0.0).rotate(Quaterniond(q))
                    b.line(p, Vector3d(p).fma(size * 0.92, d), size * 0.045, Vector3d(0.62, 0.68, 0.76), 0.85 * visible)
                }

                b.point(p, size * 0.5, Vector3d(1.0, 0.9, 0.7), 0.9 * visible)
            }
        }
    }

    private fun linksAndTrails(
        state: State,
        f: NexusFrame,
        radii: DoubleArray,
        scales: DoubleArray,
        b: NexusBatches
    ) {
        val fade = (1 - f["orbitFade"]) * (1 - f.vanish)
        val link = state.links
        link.visible = fade > 0.002
        link.instances = 6
        if (link.visible) {
            val a = link.attributeData("iA", 3, true)
            val c = link.attributeData("iB", 3, true)
            val sizes = link.attributeData("iR", 2, true)

            for (i in 0..3) {
                for (j in i + 1..3) {
                    a.put(f.planets[i])
                    c.put(f.planets[j])
                    sizes.put(radii[i] * if (i == 1) 1.66 else 1.0)
                    sizes.put(radii[j] * if (j == 1) 1.66 else 1.0)
                }
            }

            val color = mix(Vector3d(0.95, 0.97, 1.0), GOLD, f.inverse)
            link.value("uA", 0.42 * fade)
            link.value("uCol", color.x, color.y, color.z)
        }

        val trail = state.trails
        val position = trail.attributeData("position", 3)
        val tangent = trail.attributeData("aT", 3)
        val widths = trail.attributeData("aW", 1)
        val alpha = trail.attributeData("aA", 1)
        val color = mix(Vector3d(0.803, 0.814, 1.0), GOLD, f.inverse)
        trail.visible = scales.any { it > 0.01 }
        trail.drawCount = 4 * 96 * 6

        for (i in 0..3) {
            if (trail.visible) {
                val points = List(97) { j ->
                    val s = j / 96.0
                    val p = f.fall[i] - s * 0.32
                    val angle = Math.toRadians(f.planetAngles[i])
                    if (p > 0) {
                        polar(
                            angle + 2 * PI * (0.55 + 0.3 * i) * p * p,
                            f.orbitRadii[i] + (f.starRadius * 0.98 - f.orbitRadii[i]) * p.pow(1.6),
                            88.0
                        )
                    } else {
                        polar(
                            angle - (if (f.fall[i] > 0) 0.0 else radii[i] * 1.2 / f.orbitRadii[i]) -
                                Math.toRadians(80.0) * (if (f.fall[i] > 0) s - f.fall[i] / 0.32 else s),
                            f.orbitRadii[i],
                            88.0
                        )
                    }
                }

                for (j in 0..96) {
                    val s = j / 96.0
                    val t = Vector3d(points[min(96, j + 1)]).sub(points[max(0, j - 1)]).normalize()

                    for (side in 0..1) {
                        position.put(points[j].x, points[j].y, 88.0)
                        tangent.put(t)
                        widths.put(min(radii[i] * 0.95, 2.4) * 0.58 * (1 - s).pow(1.35))
                        alpha.put(
                            0.95 * (1 + 0.5 * f.inverse) * (1 - f["orbitFade"]) *
                                (1 - s).pow(1.6) * smooth(s / 0.07) * (if (scales[i] > 0.01) 1 else 0)
                        )
                    }
                }
            }

            if (f.hasShowClock) {
                val age = f.showTau - (16.2 + 0.3 * i + 2.3)
                if (age in -0.15..1.1) {
                    val strength = if (age < 0) smooth((age + 0.15) / 0.15) else 1 - smooth(age / 1.1)
                    val observer = NexusBillboardView(f.camera, STAR)
                    if (!observer.outside(f.starRadius)) continue

                    val center = observer.limbCenter(f.starRadius)
                    val radius = observer.limbRadius(f.starRadius) * 1.035
                    val a0 = atan2(Vector3d(f.planets[i]).sub(STAR).dot(f.up), Vector3d(f.planets[i]).sub(STAR).dot(f.right))
                    fun point(a: Double) = Vector3d(center).fma(cos(a) * radius, f.right).fma(sin(a) * radius, f.up)
                    b.point(point(a0), 10 * strength, Vector3d(1.0, 0.95, 0.86), 0.9 * strength)
                    b.point(point(a0), 22 * strength, Vector3d(1.0, 0.8, 0.5), 0.25 * strength)

                    for (j in 0 until 16) {
                        b.line(
                            point(a0 + max(0.0, age) * 3.4 + 1.4 * j / 16),
                            point(a0 + max(0.0, age) * 3.4 + 1.4 * (j + 1) / 16),
                            0.55 * strength + 0.12,
                            Vector3d(1.0, 0.86, 0.6),
                            0.8 * strength * (1 - j / 16.0),
                            endAlpha = 0.8 * strength * (1 - (j + 1) / 16.0)
                        )
                    }
                }
            }
        }

        if (trail.visible) {
            trail.value("uCol", color.x, color.y, color.z)
        }
    }
}
