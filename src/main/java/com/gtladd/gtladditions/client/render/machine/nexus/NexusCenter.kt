package com.gtladd.gtladditions.client.render.machine.nexus

import com.gtladd.gtladditions.client.RenderUtils.rotationRadians
import com.gtladd.gtladditions.client.render.machine.nexus.NexusFrame.Companion.GOLD
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

internal object NexusCenter {
    class State(nodes: Map<String, NexusGpu.Node>) {
        val star = nodes.getValue("dir.star2")
        val corona = nodes.getValue("core.corona.mesh")
        val innerGlow = nodes.getValue("dir.glowIn.mesh")
        val glory = nodes.getValue("core.glory.mesh")
        val flash = nodes.getValue("core.flash.mesh")
        val shell = nodes.getValue("core.shell.mesh")
        val bezel = nodes.getValue("dir.bezel.mesh")
        val moon = nodes.getValue("dir.moon")

        val moonMin = Vector3d()
        val moonMax = Vector3d()
        val limbRing = nodes.getValue("dir.limbRing.mesh")
        val beam = nodes.getValue("dir.bm.sheath.mesh")
        val rgb = Array(2) { nodes.getValue("dir.rgb.$it.mesh") }
        val rings = Array(3) { index ->
            arrayOf(
                nodes.getValue("dir.sr.styles.3.rings.$index.solid"),
                nodes.getValue("dir.sr.styles.3.rings.$index.holo")
            )
        }
    }

    // Render-thread scratch; Node.value copies the values into each scene's own storage.
    private val beamNoisePhases = FloatArray(12)
    private val warpOctaves = FloatArray(3)
    private val plasmaOctaves = FloatArray(5)
    private val spotOctaves = FloatArray(5)
    private val ringPhase = FloatArray(4)
    private val detailPhase = FloatArray(4)

    fun sprite(node: NexusGpu.Node, center: Vector3d, size: Double, color: Vector3d, alpha: Double) {
        node.visible = alpha > 0.002
        node.value("uCenter", center.x, center.y, center.z)
        node.value("uSize", size)
        node.value("uColor", color.x, color.y, color.z)
        node.value("uAlpha", alpha)
    }

    fun update(state: State, f: NexusFrame, logistics: NexusFleet.Logistics, b: NexusBatches) {
        val r = f.starRadius
        val observer = NexusBillboardView(f.camera, STAR)
        val limbCenter = observer.limbCenter(r)
        val limbRadius = observer.limbRadius(r)
        val inv = f.inverse
        val flash = f["flash"]
        val ember = f["ember"]
        val ack = f["ack"]

        val star = state.star
        star.value("uWarpPhase", NexusBeamClock.noiseOctaves(f.time * 0.006 * 1.4, 2.07, warpOctaves))
        star.value("uPlasmaPhase", NexusBeamClock.noiseOctaves(f.time * 0.006 * 1.6, 2.03, plasmaOctaves))
        star.value("uSpotPhase", NexusBeamClock.noiseOctaves(f.time * 0.002, 2.03, spotOctaves))
        star.value("uGranulePhase", f.time * 0.01 % 1024.0, f.time * 0.013 % 1024.0)
        star.visible = true
        star.matrix.translation(0f, 0f, 88f).scale(r.toFloat())
        star.value("uBright", (1 + 0.3 * flash) * (1 - 0.45 * ember))
        star.value("uWhite", min(1.0, 0.55 * f["charge"] + 0.35 * flash + 0.4 * ack))
        star.value("uEmber", ember)
        star.value("uAnti", inv)
        star.value("uRimK", f.rim)
        star.value(
            "uRot",
            Matrix3f().rotation(rotationRadians(2 * PI * f.time / 180), Vector3f(0.35f, 0.2f, 1f).normalize()).get(FloatArray(9))
        )

        val coronaColor = mix(Vector3d(0.561, 0.525, 1.0), GOLD, inv)
        val coronaSize = 72 * (1 + 0.08 * sin(2 * PI * f.time / 8.3776)) * (1 + 0.5 * flash + 0.35 * ack) * (1 - 0.65 * ember)
        sprite(
            state.corona,
            STAR,
            coronaSize,
            coronaColor,
            0.34 * (1 - 0.85 * ember) * (1 + ack + 0.8 * flash) * (1 + 0.5 * inv) * (1 - 0.55 * inv) * f.rim.pow(1.2)
        )
        sprite(
            state.innerGlow,
            STAR,
            4 * r,
            Vector3d(0.612, 0.565, 1.0),
            0.85 * (1 - inv) * (1 - ember) * (1 + 0.6 * flash)
        )

        val glory = state.glory
        glory.value("uHuePhase", fract(f.time * 0.05))
        sprite(
            glory,
            limbCenter,
            2 * (limbRadius * 1.035 + 6),
            WHITE,
            if (observer.outside(r)) 0.6 * inv * (1 - ember) * f.rim.pow(1.5) else 0.0
        )
        glory.value("uRing", limbRadius * 1.035 / (limbRadius * 1.035 + 6))
        glory.value("uWidth", 0.025 * limbRadius / (limbRadius * 1.035 + 6) * 4)

        sprite(state.flash, STAR, 260 * (0.45 + flash), Vector3d(1.0, 0.93, 0.82), 0.5 * flash)

        val k = if (observer.outside(r)) inv * f["corona"] else 0.0
        for (i in 0 until 132) {
            val h = fract(sin(i * 91.7) * 4375.85)
            val a = i * 2 * PI / 132 + f.time * 0.012 * (if (i % 2 == 1) 1 else -1) + h * 0.05
            val length = r * (0.45 + 1.95 * h * h) * smooth(k * 1.4 - h * 0.4)
            if (length < 0.3) continue
            val direction = Vector3d(f.right).mul(cos(a)).fma(sin(a), f.up)
            b.line(
                Vector3d(limbCenter).fma(limbRadius * 1.01, direction),
                Vector3d(limbCenter).fma(limbRadius * 1.01 + length, direction),
                0.42,
                Vector3d(1.0, 0.8, 0.48),
                0.55 * k * f.rim,
                0.12,
                0.0
            )
        }

        diamond(state, f, b)

        if (f.eventId > 0) {
            for (event in doubleArrayOf(0.05, 16.0)) {
                val age = f.eventAge(event, 1.6) ?: continue

                if (age in 0.0..1.6) {
                    val p = age / 1.6
                    b.ring(
                        STAR,
                        18 + 152 * (1 - (1 - p).pow(3)),
                        0.4 + 2 * p,
                        if (event > 5) Vector3d(1.0, 0.84, 0.55) else Vector3d(1.0, 0.96, 0.88),
                        0.55 * (1 - p).pow(2)
                    )
                }

                if (age in 0.0..1.2) {
                    val shell = state.shell
                    sprite(
                        shell,
                        STAR,
                        40 + 280 * (1 - (1 - age / 1.2).pow(3)),
                        if (event > 5) Vector3d(1.0, 0.8, 0.45) else Vector3d(1.0, 0.95, 0.85),
                        0.85 * (1 - age / 1.2)
                    )
                    shell.value("uRing", 0.92)
                    shell.value("uWidth", 0.05)
                }
            }
        }

        beam(state, f, logistics, b)
        rings(state, f, b)
        glitch(state, f, b)

        val bezel = state.bezel
        val visibility = if (observer.outside(r)) (1 - inv) * (1 - f["orbitFade"]) else 0.0
        val rs = limbRadius * 1.17
        sprite(bezel, limbCenter, (rs + 2) * 2, Vector3d(0.84, 0.83, 1.0), 0.4 * visibility)
        bezel.value("uRing", rs / (rs + 2))
        bezel.value("uWidth", 0.0015)

        if (visibility > 0.002) {
            for (i in 0 until 120) {
                val a = 2 * PI * i / 120
                val direction = Vector3d(f.right).mul(cos(a)).fma(sin(a), f.up)
                b.line(
                    Vector3d(limbCenter).fma(rs, direction),
                    Vector3d(limbCenter).fma(rs + if (i % 10 == 0) 0.8 else 0.3, direction),
                    if (i % 10 == 0) 0.055 else 0.025,
                    Vector3d(0.84, 0.83, 1.0),
                    0.55 * visibility
                )
            }
        }
    }

    private fun diamond(state: State, f: NexusFrame, b: NexusBatches) {
        if (f.state != "unlock" || f.age < 15) return
        val observer = NexusBillboardView(f.camera, STAR)
        if (!observer.outside(f.starRadius)) return
        val coverage = smooth((f.age - 15) / 0.85)
        val fade = 1 - smooth((f.inverse - 0.82) / 0.18)
        if (fade < 0.002) return

        val toController = Vector3d(0.0, -1.0, 0.0)
        var dx = toController.dot(f.right)
        var dy = toController.dot(f.up)
        val length = hypot(dx, dy)
        if (length < 0.001) {
            dx = 0.0
            dy = -1.0
        } else {
            dx /= length
            dy /= length
        }

        val r = f.starRadius
        val lift = min(r * 1.05, observer.distance * 0.95)
        val rho = observer.radiusAtLift(r, lift)
        val offset = -(1 - coverage) * 2.08 - 0.035 * coverage
        val moon = state.moon
        moon.visible = coverage > 0
        moon.value("uCenter", 0.0, 0.0, 88.0)
        moon.value("uRad", rho)
        moon.value("uLift", lift)
        moon.value("uOff", dx * offset, dy * offset)
        moon.value("uAlpha", fade)
        // Keep identical to the corners celestial_eclipse.vsh emits.
        state.moonMin.set(Double.MAX_VALUE)
        state.moonMax.set(-Double.MAX_VALUE)

        for (corner in 0..3) {
            val sx = if (corner and 1 == 0) -1.0 else 1.0
            val sy = if (corner and 2 == 0) -1.0 else 1.0
            val point = Vector3d(STAR)
                .fma(lift, observer.normal)
                .fma(rho * (sx + dx * offset), observer.right)
                .fma(rho * (sy + dy * offset), observer.up)
            state.moonMin.min(point)
            state.moonMax.max(point)
        }

        val view = observer.normal
        val center = observer.limbCenter(r)
        val radius = observer.limbRadius(r)
        fun limb(angle: Double, push: Double) =
            Vector3d(center).fma(cos(angle) * radius, f.right).fma(sin(angle) * radius, f.up).fma(push, view)
        val angle = atan2(dy, dx)

        val ring = state.limbRing
        sprite(
            ring,
            Vector3d(center).fma(0.3, view),
            2 * radius * 1.1,
            Vector3d(1.0, 0.957, 0.886),
            0.55 * ((coverage - 0.8) / 0.2).coerceIn(0.0, 1.0).pow(2) * fade
        )
        ring.value("uRing", 1.006 / 1.1)
        ring.value("uWidth", 0.012)

        val bead = ((coverage - 0.55) / 0.45).coerceIn(0.0, 1.0) * f["diamond"]
        if (bead > 0.005) {
            val position = limb(angle, 0.6)
            b.point(position, 24 * bead, Vector3d(1.0, 0.97, 0.9), 1.0)
            b.point(position, 52.8 * bead, Vector3d(1.0, 0.88, 0.66), 0.3 * bead)

            for (axis in listOf(f.right, f.up)) {
                for (sign in listOf(-1, 1)) {
                    b.line(
                        position,
                        Vector3d(position).fma(sign * 30 * bead, axis),
                        1.1 * bead,
                        Vector3d(1.0, 0.95, 0.85),
                        0.8 * bead,
                        0.05,
                        0.0
                    )
                }
            }

            for ((delta, strength) in listOf(0.13 to 0.55, -0.11 to 0.5, 0.24 to 0.34, -0.21 to 0.3, 0.33 to 0.18)) {
                b.point(
                    limb(angle + delta, 0.5),
                    3.2 * strength + 1,
                    Vector3d(1.0, 0.95, 0.86),
                    strength * bead * ((coverage - 0.9) / 0.1).coerceIn(0.0, 1.0)
                )
            }
        }
    }

    private fun beam(state: State, f: NexusFrame, logistics: NexusFleet.Logistics, b: NexusBatches) {
        val node = state.beam
        node.visible = f.beamTop > 10.5
        if (!node.visible) return

        val dim = (1 - f["beamDim"]) * (1 - f["ember"] * 0.9) * (1 + 0.6 * f["flash"])
        val radius = if (f.beamTop < 88 - f.starRadius) 0.3 + 0.7 * smooth((f.beamTop - 10) / (78 - f.starRadius)) else 1.0
        node.value("uTop", f.beamTop)
        node.value("uTopK", if (f["beamRetract"] > 0.0005) 1.0 else 0.0)
        node.value("uRK", radius)
        node.value("uStar", 0.0, 0.0, 88.0, f.starRadius * 0.98)
        node.value("uRootK", 0.45 * (1 - f["ember"]))
        node.value("uDisc", 0.0, 0.0, 88.0, f.starRadius)
        node.value("uDiscK", 0.9 * f.inverse)
        node.value("uAlpha", dim * f.coreBrightness)
        node.value("uRb", 0.7 * f["spectral"])

        val spacing = 155.0
        val speed = 40.0
        node.value("uJet", spacing, speed, 0.35 + 0.2 * f["paradox"], 0.0)
        val phase = NexusBeamClock.flowPhase(f.time, speed, spacing)
        node.value("uBeamPhase", phase, phase, NexusBeamClock.huePhase(f.time))
        NexusBeamClock.noisePhases(f.time, speed, beamNoisePhases)
        node.value("uNoisePhase", beamNoisePhases)

        val pulses = mutableListOf<DoubleArray>()
        var kick = 0.0

        for (age in logistics.transfer) {
            if (age in 0.0..9.0) {
                pulses.add(doubleArrayOf(106 + 95 * age, 0.7 * f["power"] * exp(-age / 5) * 1.6, 70.0))
                kick = max(kick, exp(-age / 0.25))
            }
        }

        if (f.eventId > 0) {
            for (event in doubleArrayOf(16.0, 16.45, 16.9)) {
                val age = f.eventAge(event, 9.0) ?: continue
                if (age in 0.0..9.0) {
                    pulses.add(doubleArrayOf(106 + 114 * age, 2.56, 105.0))
                    if (age < 1) kick = max(kick, 1 - age)
                }
            }
        }

        node.value(
            "uPulse",
            FloatArray(30) { index ->
                pulses.getOrNull(index / 3)?.get(index % 3)?.toFloat() ?: if (index % 3 == 0) {
                    -999f
                } else if (index % 3 == 2) {
                    30f
                } else {
                    0f
                }
            }
        )

        val color = mix(Vector3d(0.839, 0.831, 1.0), GOLD, f.inverse)
        val ba = (1 - f["beamDim"]) * (1 - f["ember"])
        val throat = ba * smooth((f.beamTop - 106) / 40)
        val bright = 1 + 1.6 * kick
        val cc = mix(color, WHITE, 0.5 * kick)

        b.ring(Vector3d(0.0, 0.0, 106.0), 9.6, 0.09, cc, 0.85 * throat * bright, segments = 3.0, gap = 0.1, rotation = f.time * 0.025)
        b.ring(
            Vector3d(0.0, 0.0, 106.0),
            10.25,
            0.09,
            cc,
            0.75 * throat * bright,
            segments = 3.0,
            gap = 0.1,
            rotation = -f.time * 0.025 + 0.5
        )
        b.ring(Vector3d(0.0, 0.0, 106.0), 9.9, 0.5, cc, 0.05 * throat * bright)
        b.ring(Vector3d(0.0, 0.0, 106.0), 11.85, 0.03, color, 0.4 * throat)
        for (i in 0 until 36) b.line(polar(i * PI / 18, 11.85, 106.0), polar(i * PI / 18, 12.25, 106.0), 0.05, color, 0.45 * throat)

        for (i in 0..5) {
            val angle = 2 * PI * (i / 6.0 + 1.0 / 12)
            b.line(polar(angle, 10.45, 106.0), polar(angle, 11.35, 106.0), 0.22, cc, 0.85 * throat * bright)
            b.point(polar(angle, 10.45, 106.0), 0.8, mix(cc, WHITE, 0.5), 0.9 * throat)
        }

        for (sign in listOf(-1, 1)) {
            val z = 88 + sign * f.starRadius
            val gain = (if (sign == 1) 1.0 else 0.45) * smooth((f.beamTop - z) / 14)
            b.point(Vector3d(0.0, 0.0, z + sign * 0.4), 14.0, mix(color, WHITE, 0.3), 0.42 * ba * gain)
        }
    }

    private fun glitch(state: State, f: NexusFrame, b: NexusBatches) {
        val strength = f.glitch
        if (strength < 0.02) return
        val observer = NexusBillboardView(f.camera, STAR)
        if (!observer.outside(f.starRadius)) return

        val step = floor(f.time * 12)
        val view = observer.normal
        val center = observer.limbCenter(f.starRadius)
        val silhouette = observer.limbRadius(f.starRadius)
        val colors = listOf(WHITE, Vector3d(0.45, 1.0, 1.0), Vector3d(1.0, 0.35, 0.85), Vector3d(1.0, 0.82, 0.45))

        for (i in 0 until 30) {
            fun hash(n: Double) = fract(sin((i * 7.13 + step * 1.71 + n * 3.3) * 12.9898) * 43758.5453)
            if (hash(1.0) > strength) continue
            val p = Vector3d(center).fma((hash(2.0) - 0.5) * 72, f.right).fma((hash(3.0) - 0.5) * 44, f.up).fma(2.0, view)
            val length = 2 + hash(4.0) * 10
            b.line(
                Vector3d(p).fma(-length / 2, f.right),
                Vector3d(p).fma(length / 2, f.right),
                0.15 + hash(5.0) * 1.1,
                colors[(hash(6.0) * 4).toInt()],
                (0.35 + 0.5 * hash(7.0)) * strength
            )
        }

        val offset = (0.5 + 0.8 * fract(sin(step * 3.17) * 43758.5)) * strength * 1.2
        val radius = silhouette * 1.04
        val size = radius + 6

        for (i in 0..1) {
            val node = state.rgb[i]
            sprite(
                node,
                Vector3d(center).fma((if (i == 0) 1 else -1) * offset, f.right).fma(0.5, view),
                2 * size,
                if (i == 0) Vector3d(1.0, 0.25, 0.3) else Vector3d(0.25, 0.95, 1.0),
                0.55 * strength
            )
            node.value("uRing", radius / size)
            node.value("uWidth", 0.02)
        }
    }

    private fun rings(state: State, f: NexusFrame, b: NexusBatches) {
        if (f.build <= 0.002) return

        ringPhase[0] = (floor(f.time * 13) % 1024.0).toFloat()
        ringPhase[1] = fract(f.time * 0.8).toFloat()
        ringPhase[2] = rotationRadians(f.time * 23)
        ringPhase[3] = rotationRadians(f.time * 0.5)
        detailPhase[0] = fract(f.time * 0.4).toFloat()
        detailPhase[1] = fract(f.time * 0.22).toFloat()
        detailPhase[2] = rotationRadians(f.time * 1.4)
        detailPhase[3] = rotationRadians(f.time * 6)
        val radii = doubleArrayOf(25.0, 32.0, 39.0)
        for (i in 0..2) {
            val az = Math.toRadians(i * 120.0 + (if (i % 2 == 1) -1 else 1) * 0.35 * 44 * f.time)
            val el = Math.toRadians(doubleArrayOf(0.0, 30.0, -30.0)[i])
            val axis = Vector3f((cos(el) * cos(az)).toFloat(), (cos(el) * sin(az)).toFloat(), sin(el).toFloat())
            val normal = Vector3f(0f, 0f, 1f).fma(-axis.z, axis).normalize()
            val side = Vector3f(normal).cross(axis)
            val basis = Matrix3f().setColumn(0, axis).setColumn(1, side).setColumn(2, normal)
            val theta = Math.toRadians(doubleArrayOf(0.0, 55.0, -40.0)[i] + doubleArrayOf(1.0, -0.78, 0.61)[i] * 44 * f.time)
            val q = Quaternionf().rotationAxis(rotationRadians(theta), axis)
                .mul(Quaternionf().setFromNormalized(basis))
                .rotateZ(rotationRadians(Math.toRadians(74 * doubleArrayOf(1.0, -0.7, 0.5)[i] * f.time)))

            val progress = smooth((f.build * 1.25 - 0.1 - i * 0.3) / 0.55)
            val holo = if (f.build < 0.999) smooth((f.build * 1.25 - 0.1 - i * 0.3 + 0.12) / 0.12) else 0.0

            for (kind in 0..1) {
                val node = state.rings[i][kind]
                node.value("uRingPhase", ringPhase)
                node.value("uDetailPhase", detailPhase)
                node.visible = (progress > 0.001 || f.build > 0.02) && (kind == 0 || (f.build < 0.999 && progress < 0.999))
                node.matrix.translation(0f, 0f, 88f).rotate(q)
                node.value("uBuildA", progress)
                node.value("uHoloA", holo)
                node.value("uGlitch", f.glitch)
                node.value("uStarR", f.starRadius)
                node.value("uBeamR", (1 - f["beamDim"]) * (1 - f["beamRetract"]))
            }

            if (progress in 0.001..0.999) {
                val point = polar(2 * PI * progress, radii[i], 0.0).rotate(Quaterniond(q)).add(STAR)
                b.point(point, 1.8, Vector3d(0.8, 1.0, 1.0), 0.55)

                for (j in 0..9) {
                    val life = fract(f.time * 1.3 + j * 0.618 + i * 0.27)
                    val seed = j * 7.1 + i * 3.3
                    fun hash(n: Double) = fract(sin(n * 12.9898) * 43758.5453)
                    val p = polar(
                        2 * PI * progress + life * 3.2 / radii[i],
                        radii[i] + (hash(seed) - 0.5) * 1.6 * life,
                        (hash(seed + 1.7) - 0.5) * 2.4 * life
                    ).rotate(Quaterniond(q)).add(STAR)
                    b.point(p, 0.12 + 0.4 * (1 - life), Vector3d(0.75, 0.97, 1.0), 0.9 * (1 - life).pow(1.5))
                }
            }
        }
    }
}
