package com.gtladd.gtladditions.client.render.machine.nexus

import com.gtladd.gtladditions.common.machine.multiblock.controller.SubspaceCorridorNexusIndustrialArray
import com.gtladd.gtladditions.utils.SubspaceNexusAnimationClock
import com.gtladd.gtladditions.utils.SubspaceNexusTimeline
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

internal class NexusFrame(
    val machine: SubspaceCorridorNexusIndustrialArray,
    timing: SubspaceNexusAnimationClock.Frame,
    val camera: Vector3d,
    val right: Vector3d,
    val up: Vector3d
) {
    val animationState = timing.animationState
    val tick = timing.tick
    val eventTick = timing.eventTick
    val eventId = timing.eventId
    val animation = SubspaceNexusTimeline.sample(animationState, tick, eventTick)

    val ambient = 0.2 + 0.8 * (1 - ((machine.level?.skyDarken ?: 0) / 11.0).coerceIn(0.0, 1.0))
    val glow = 1 - (ambient - 0.2) * 0.275
    val coreBrightness = 1 + (ambient - 0.2) * 0.375

    val skyColor: Vec3 = Minecraft.getInstance().level
        ?.getSkyColor(Minecraft.getInstance().gameRenderer.mainCamera.position, Minecraft.getInstance().partialTick)
        ?: Vec3(0.027, 0.043, 0.078)

    val time = animation.time
    val age = animation.age
    val state = animation.state
    val ships = animation.ships

    operator fun get(key: String) = animation[key]

    val inverse = this["invert"]
    val starRadius = 17.02 * (1 - 0.09 * this["charge"] + 0.06 * this["flash"]) * (1 - 0.59 * this["ember"])
    val rim = this["rim"] * (1 + 0.16 * maxOf(0.0, 1 - this["rim"]) * sin(time * 0.7))
    val beamTop = 10 + 970 * (exp(4.5 * (1 - this["beamRetract"])) - 1) / (exp(4.5) - 1)
    val ignition = eventTick / 20.0 + 16

    fun eventAge(offset: Double, duration: Double): Double? = if (eventId > 0) {
        SubspaceNexusTimeline.eventAge(animationState, time * 20, eventTick, offset, duration)
    } else {
        null
    }

    val burst = eventAge(16.0, 2.2) ?: -1.0
    private val showClock = SubspaceNexusTimeline.showClock(animationState, tick)
    val hasShowClock = showClock != null
    val showTau = showClock ?: if (inverse > 0.5) 40.0 else 0.0
    val build = SubspaceNexusTimeline.buildProgress(animationState, tick)
    val vanish = if (showClock != null) smooth((showTau - 16.5) / 2.2) else smooth((inverse - 0.12) / 0.7)

    val glitch = if (showClock != null) {
        maxOf(
            if (showTau > 18.6 && showTau < 20.6) sin(PI * ((showTau - 18.6) / 2)).pow(0.7) else 0.0,
            2.4 * build * (1 - build)
        ) * this["glitchK"]
    } else if (build > 0.003 && build < 0.997) {
        minOf(1.0, 3 * build * (1 - build)) * this["glitchK"]
    } else {
        0.0
    }

    val fall = DoubleArray(4) {
        if (showClock != null) ((showTau - 16.2 - it * 0.3) / 2.3).coerceIn(0.0, 1.0) else smooth((inverse - 0.12) / 0.7)
    }

    val chartTime = SubspaceNexusTimeline.chartClock(animationState, tick)

    // Evaluate the held clock at one fixed tick: subtracting two advancing doubles leaves a few ULPs of motion in the
    // asteroid spin phases.
    val beltTime = if (state == "unlock" && age >= 12.0 && age <= 16.0) {
        SubspaceNexusTimeline.chartClock(animationState, animationState.getDouble("since") + 240.0) - animationState.getDouble("beltHold")
    } else {
        chartTime - animationState.getDouble("beltHold") - if (state == "unlock") (age - 12).coerceIn(0.0, 4.0) else 0.0
    }

    val orbitRadii = doubleArrayOf(25.0, 31.5, 38.0, 44.5)
    val planetAngles = SubspaceNexusTimeline.planetAngles(animationState, tick, eventTick)

    val planets = List(4) { i ->
        val progress = fall[i]
        val r = orbitRadii[i] + (starRadius * 0.98 - orbitRadii[i]) * progress.pow(1.6)
        val angle = planetAngles[i] * PI / 180 + 2 * PI * (0.55 + 0.3 * i) * progress * progress
        Vector3d(r * cos(angle), r * sin(angle), 88.0)
    }

    companion object {
        val STAR = Vector3d(0.0, 0.0, 88.0)
        val ICE = Vector3d(0.545, 0.937, 1.0)
        val GOLD = Vector3d(1.0, 0.769, 0.42)
        val WHITE = Vector3d(1.0, 1.0, 1.0)

        fun smooth(x: Double) = SubspaceNexusTimeline.smooth(x)

        fun fract(x: Double) = SubspaceNexusTimeline.fract(x)

        fun polar(angle: Double, radius: Double, z: Double) = Vector3d(radius * cos(angle), radius * sin(angle), z)

        fun mix(a: Vector3d, b: Vector3d, k: Double): Vector3d = Vector3d(a).lerp(b, k)
    }
}
