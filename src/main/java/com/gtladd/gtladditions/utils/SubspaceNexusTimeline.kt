package com.gtladd.gtladditions.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.nbt.CompoundTag
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

object SubspaceNexusTimeline {
    const val PERIOD = 50.2655
    private val phases = doubleArrayOf(0.557, 0.0234, 0.9655, 0.286, 0.6424, 0.1825)
    private val workingStates = setOf("normal", "paradox", "startN", "startP", "resumeP", "startU", "unlock")
    private val paradoxStates = setOf("paradox", "idleP", "startP", "resumeP", "stopP", "refail")

    private val defaults = mapOf(
        "chartSpin" to 1.0,
        "orbitDraw" to 1.0,
        "railSweep" to 1.0,
        "rim" to 1.0,
        "glitchK" to 1.0,
        "ringSpin" to 1.0,
        "ringLv" to 1.0,
        "timeK" to 1.0
    )

    private val states: JsonObject by lazy {
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream("assets/gtladditions/subspace_nexus/timeline.json"))
        InputStreamReader(stream, StandardCharsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject.getAsJsonObject("states") }
    }

    val keys: Set<String> by lazy {
        defaults.keys + states.entrySet().flatMap { (_, entry) ->
            val value = entry.asJsonObject
            (value.getAsJsonObject("base")?.keySet() ?: emptySet()) + (value.getAsJsonObject("tracks")?.keySet() ?: emptySet())
        }
    }

    data class Ship(
        val z: Double,
        val velocity: Double,
        val phase: Double,
        val docked: Boolean,
        val continuous: Boolean = false,
        val recallProgress: Double = -1.0
    )

    data class Frame(val state: String, val age: Double, val time: Double, val values: Map<String, Double>, val ships: List<Ship>) {
        operator fun get(key: String): Double = values[key] ?: defaults[key] ?: 0.0
    }

    fun smooth(value: Double): Double = value.coerceIn(0.0, 1.0).let { it * it * (3 - 2 * it) }

    fun fract(value: Double): Double = value - floor(value)

    fun duration(state: String): Double = states.getAsJsonObject(state)?.get("duration")?.asDouble ?: Double.POSITIVE_INFINITY

    fun isWorkingState(state: String): Boolean = state in workingStates

    fun isParadoxState(state: String): Boolean = state in paradoxStates

    fun completedState(state: String): String? = when (state) {
        "startN" -> "normal"
        "stopN", "abort" -> "idle"
        "startU" -> "unlock"
        "unlock", "startP", "resumeP" -> "paradox"
        "stopP", "refail" -> "idleP"
        else -> null
    }

    private fun ease(value: Double, kind: String): Double = when (kind) {
        "sine" -> (1 - cos(PI * value)) * 0.5
        "in" -> value * value * value
        "out" -> 1 - (1 - value) * (1 - value) * (1 - value)
        "step" -> if (value >= 1) 1.0 else 0.0
        else -> value
    }

    fun track(keys: JsonArray, time: Double): Double {
        var previous = keys[0].asJsonArray
        if (time <= previous[0].asDouble) return previous[1].asDouble

        for (i in 1 until keys.size()) {
            val next = keys[i].asJsonArray
            if (time < next[0].asDouble) {
                val progress = (time - previous[0].asDouble) / (next[0].asDouble - previous[0].asDouble)
                val k = ease(progress, if (next.size() > 2) next[2].asString else "lin")
                return previous[1].asDouble + (next[1].asDouble - previous[1].asDouble) * k
            }

            previous = next
        }

        return previous[1].asDouble
    }

    fun cycle(index: Int, time: Double, paradoxEpoch: Double? = null): Ship {
        val u = fract(
            if (paradoxEpoch == null) {
                time / PERIOD + phases[index]
            } else {
                (time - paradoxEpoch) / PERIOD + 0.09 + phases[index] - phases[3]
            }
        )
        return cyclePhase(u)
    }

    private fun cyclePhase(u: Double): Ship {
        val ascending = u >= 0.09 && u < 0.5
        val descending = u >= 0.59

        val x = when {
            ascending -> (u - 0.09) / 0.41
            descending -> (u - 0.59) / 0.41
            else -> 0.0
        }

        val f = when {
            ascending -> (1 - cos(PI * x)) * 0.5
            descending -> (1 + cos(PI * x)) * 0.5
            u >= 0.5 -> 1.0
            else -> 0.0
        }

        val velocity = if (ascending || descending) 291 * PI * sin(PI * x) / (2 * 0.41 * PERIOD) * (if (ascending) 1 else -1) else 0.0
        return Ship(69 + 291 * f, velocity, u, u < 0.09, true)
    }

    private fun recall(start: Ship, age: Double, duration: Double): Ship {
        val p = (age / duration).coerceIn(0.0, 1.0)

        // Smoothstep has zero endpoint slope, preserving take-over velocity.
        val moving = if (start.continuous) cyclePhase(fract(start.phase + age / PERIOD)) else start
        val k = smooth(p)
        val z = moving.z + (69 - moving.z) * k
        val speed = moving.velocity * (1 - k) + (69 - moving.z) * 6 * p * (1 - p) / duration
        return Ship(z, if (p >= 1) 0.0 else speed, moving.phase, p >= 1 || z <= 69.0001, recallProgress = k)
    }

    fun sampleValues(animationState: CompoundTag, age: Double): Map<String, Double> {
        val state = animationState.getString("state").ifEmpty { "idle" }
        val definition = states.getAsJsonObject(state) ?: states.getAsJsonObject("idle")
        val base = definition.getAsJsonObject("base")
        val tracks = definition.getAsJsonObject("tracks")
        val values = keys.associateWith { key ->
            tracks?.getAsJsonArray(key)?.let { track(it, age) } ?: base?.get(key)?.asDouble ?: defaults[key] ?: 0.0
        }.toMutableMap()

        val from = animationState.getCompound("from")
        val blend = definition.get("blendIn")?.asDouble ?: if (state == "stopP" && animationState.getBoolean("cutShow")) 1.0 else 0.0
        if (blend > 0 && age < blend && !from.isEmpty) {
            val k = smooth(age / blend)
            values.replaceAll { key, value -> from.getDouble(key) + (value - from.getDouble(key)) * k }
        }

        return values
    }

    private val integrals = java.util.WeakHashMap<CompoundTag, DoubleArray>()

    @Synchronized
    fun chartClock(animationState: CompoundTag, gameTick: Double): Double {
        val since = animationState.getDouble("since") / 20.0
        val age = maxOf(0.0, gameTick / 20.0 - since)
        val state = animationState.getString("state").ifEmpty { "idle" }
        val start = if (animationState.contains("chartAt")) animationState.getDouble("chartAt") else since
        if (state == "normal" || state == "paradox") return start + age
        if (state == "idle" || state == "idleP") return start
        if (!duration(state).isFinite()) return start

        val table = integrals.getOrPut(animationState) {
            val count = (duration(state) * 30).toInt() + 2
            DoubleArray(count).also { array ->
                for (i in 1 until count) array[i] = array[i - 1] + (sampleValues(animationState, (i - 0.5) / 30.0)["chartSpin"] ?: 0.0) / 30
            }
        }

        val x = minOf(age * 30, table.size - 1.000001)
        val i = x.toInt()
        return start + table[i] + (table[i + 1] - table[i]) * (x - i)
    }

    fun sample(animationState: CompoundTag, gameTick: Double, eventTick: Double): Frame {
        val state = animationState.getString("state").ifEmpty { "idle" }
        val time = gameTick / 20.0
        val age = (time - animationState.getDouble("since") / 20.0).coerceAtLeast(0.0)
        return Frame(state, age, time, sampleValues(animationState, age), sampleShips(animationState, gameTick, eventTick))
    }

    fun sampleValue(animationState: CompoundTag, age: Double, key: String): Double {
        val state = animationState.getString("state").ifEmpty { "idle" }
        val definition = states.getAsJsonObject(state) ?: states.getAsJsonObject("idle")
        val value = definition.getAsJsonObject("tracks")?.getAsJsonArray(key)?.let { track(it, age) }
            ?: definition.getAsJsonObject("base")?.get(key)?.asDouble ?: defaults[key] ?: 0.0

        val from = animationState.getCompound("from")
        val blend = definition.get("blendIn")?.asDouble ?: if (state == "stopP" && animationState.getBoolean("cutShow")) 1.0 else 0.0
        return if (blend > 0 && age < blend && !from.isEmpty) {
            from.getDouble(key) + (value - from.getDouble(key)) * smooth(age / blend)
        } else {
            value
        }
    }

    fun sampleShips(animationState: CompoundTag, gameTick: Double, eventTick: Double): List<Ship> {
        val state = animationState.getString("state").ifEmpty { "idle" }
        val definition = states.getAsJsonObject(state) ?: states.getAsJsonObject("idle")
        val since = animationState.getDouble("since") / 20.0
        val time = gameTick / 20.0
        val age = (time - since).coerceAtLeast(0.0)
        val eclipse = eventTick / 20.0 + 16.0

        if (animationState.contains("carry") && time < animationState.getDouble("carryUntil")) {
            return sampleShips(animationState.getCompound("carry"), gameTick, eventTick)
        }

        val modes = definition.getAsJsonArray("ships")
        var modeIndex = modes.size() - 1
        while (modeIndex > 0 && age < modes[modeIndex].asJsonObject.get("t0").asDouble) modeIndex--
        val mode = modes[modeIndex].asJsonObject
        val kind = mode.get("mode").asString

        return List(6) { index ->
            when (kind) {
                "docked" -> Ship(69.0, 0.0, 0.0, true)
                "normal" -> if (animationState.contains("join$index") && time < animationState.getDouble("join$index")) {
                    Ship(69.0, 0.0, 0.0, true)
                } else {
                    cycle(index, time)
                }
                "recall" -> {
                    val startTime = mode.get("t0").asDouble
                    val start = if (state == "unlock") {
                        cycle(index, since + startTime)
                    } else {
                        animationState.getCompound("ship$index").let {
                            if (it.isEmpty) {
                                cycle(index, since, if (sampleValue(animationState, age, "invert") > 0.5) eclipse else null)
                            } else {
                                Ship(
                                    it.getDouble("z"),
                                    it.getDouble("velocity"),
                                    it.getDouble("phase"),
                                    it.getBoolean("docked"),
                                    it.getBoolean("continuous"),
                                    if (it.contains("recallProgress")) it.getDouble("recallProgress") else -1.0
                                )
                            }
                        }
                    }

                    if (mode.get("keepDocked")?.asBoolean == true && start.docked && !start.continuous && start.recallProgress < 0) {
                        start
                    } else {
                        recall(start, age - startTime, (mode.get("until")?.asDouble ?: 5.0) - startTime)
                    }
                }
                "carry" -> Ship(69.0, 0.0, 0.0, true)
                "waitslot" -> {
                    val start = maxOf(since + mode.get("t0").asDouble, animationState.getDouble("carryUntil"))
                    val epoch = if (mode.get("sched")?.asString == "unison") eclipse else null
                    val phase = cycle(index, start, epoch).phase
                    val first = start + if (phase > 1e-6) (1 - phase) * PERIOD else 0.0
                    if (time < first) Ship(69.0, 0.0, 0.0, true) else cycle(index, time, epoch)
                }
                else -> {
                    val first = eclipse + fract(phases[3] - phases[index]) * PERIOD
                    if (time < maxOf(first, if (animationState.contains("join$index")) animationState.getDouble("join$index") else first)) {
                        Ship(69.0, 0.0, 0.0, true)
                    } else {
                        cycle(index, time, eclipse)
                    }
                }
            }
        }
    }

    fun transition(previous: CompoundTag, state: String, tick: Double, eventTick: Double): CompoundTag {
        val frame = sample(previous, tick, eventTick)
        return CompoundTag().apply {
            putString("state", state)
            putDouble("since", tick)

            val previousState = previous.getString("state")
            val oldEvent = when {
                previous.contains("showEventTick") -> previous.getDouble("showEventTick")
                previousState == "unlock" -> previous.getDouble("since")
                previousState == "startU" -> previous.getDouble("since") - 40
                else -> eventTick
            }

            val newShow = state == "unlock" || state == "startU"
            putDouble("showEventTick", if (newShow) eventTick else oldEvent)
            putDouble(
                "showBegin",
                if (state == "startU" || (state == "unlock" && previousState == "startU")) {
                    10.0
                } else if (state == "unlock") {
                    0.0
                } else {
                    previous.getDouble("showBegin")
                }
            )

            val previousEnd = if (previous.contains("showEnd")) previous.getDouble("showEnd") else 36.0
            putDouble(
                "showEnd",
                if (newShow) {
                    36.0
                } else {
                    minOf(previousEnd, if (previousState == "unlock" || previousState == "startU") (tick - oldEvent) / 20.0 else 36.0)
                }
            )
            putDouble("chartAt", chartClock(previous, tick))
            putDouble(
                "eclipseChart",
                if (state == "unlock") chartClock(previous, tick) + 16 else previous.getDouble("eclipseChart")
            )
            put(
                "planetAt",
                CompoundTag().apply {
                    planetAngles(previous, tick, eventTick).forEachIndexed { i, angle -> putDouble("$i", angle) }
                }
            )

            val hold =
                previous.getDouble("beltHold") + if (previous.getString("state") == "unlock") (frame.age - 12).coerceIn(0.0, 4.0) else 0.0
            putDouble("beltHold", hold)
            put(
                "from",
                CompoundTag().apply {
                    frame.values.forEach { (key, value) -> putDouble(key, value) }
                    putDouble("helix", buildProgress(previous, tick))
                }
            )

            frame.ships.forEachIndexed { index, ship ->
                put(
                    "ship$index",
                    CompoundTag().apply {
                        putDouble("z", ship.z)
                        putDouble("velocity", ship.velocity)
                        putDouble("phase", ship.phase)
                        putBoolean("docked", ship.docked)
                        putBoolean("continuous", ship.continuous)
                        putDouble("recallProgress", ship.recallProgress)
                    }
                )
            }

            val cut = previous.getString("state") == "unlock" && state == "stopP"
            putBoolean("cutShow", cut)
            if (cut) {
                putDouble("cutTau", frame.age)
                putDouble("fallCutTau", frame.age)
                putDouble("fallCutTick", tick)
            } else if (previous.contains("fallCutTick") && tick < previous.getDouble("fallCutTick") + 70) {
                putDouble("fallCutTau", previous.getDouble("fallCutTau"))
                putDouble("fallCutTick", previous.getDouble("fallCutTick"))
            }

            if (state == "resumeP") {
                put("carry", previous.copy())
                putDouble("carryUntil", previous.getDouble("since") / 20.0 + if (previous.getString("state") == "refail") 6 else 10)
            } else if (state == "paradox" && previous.contains("carry") && previous.getDouble("carryUntil") > tick / 20.0) {
                put("carry", previous.getCompound("carry").copy())
                putDouble("carryUntil", previous.getDouble("carryUntil"))
            }

            for (index in 0..5) {
                val previousState = previous.getString("state")
                if (previousState in setOf("startN", "startP", "resumeP")) {
                    val start = maxOf(
                        previous.getDouble("since") / 20.0 + if (previousState == "resumeP") 5.0 else 4.0,
                        previous.getDouble("carryUntil")
                    )
                    val phase = cycle(index, start, if (previousState == "startN") null else eventTick / 20.0 + 16).phase
                    putDouble("join$index", start + if (phase > 1e-6) (1 - phase) * PERIOD else 0.0)
                } else if (previous.contains("join$index") && previous.getDouble("join$index") > tick / 20.0) {
                    putDouble("join$index", previous.getDouble("join$index"))
                }
            }
        }
    }

    /** The event began two seconds before startU, so its eight seconds end at countdown tau 10. */
    fun joinShow(animationState: CompoundTag, eventTick: Double): CompoundTag = animationState.copy().apply {
        putDouble("since", eventTick)
        putDouble("chartAt", getDouble("chartAt") - 10)
        putDouble("eclipseChart", getDouble("chartAt") + 16)
    }

    fun showClock(animationState: CompoundTag, tick: Double): Double? {
        if (animationState.getString("state") == "unlock") return (tick - animationState.getDouble("since")) / 20.0

        if (animationState.contains("fallCutTick")) {
            val cut = animationState.getDouble("fallCutTau")
            return cut + (23.4 - cut) * smooth((tick - animationState.getDouble("fallCutTick")) / 70.0)
        }

        if (animationState.getBoolean("cutShow")) {
            val cut = animationState.getDouble("cutTau")
            return cut + (23.4 - cut) * smooth((tick - animationState.getDouble("since")) / 70.0)
        }

        return null
    }

    fun eventAge(animationState: CompoundTag, tick: Double, eventTick: Double, offset: Double, duration: Double): Double? {
        val epoch = if (animationState.contains("showEventTick")) animationState.getDouble("showEventTick") else eventTick
        if (epoch < 0) return null

        val begin = if (animationState.contains("showBegin")) {
            animationState.getDouble("showBegin")
        } else if (animationState.getString("state") == "startU") {
            10.0
        } else {
            0.0
        }

        val end = if (animationState.contains("showEnd")) animationState.getDouble("showEnd") else 36.0
        if (offset < begin || offset > end) return null

        val age = (tick - epoch) / 20.0 - offset
        return age.takeIf { it >= 0 && it < duration }
    }

    fun buildProgress(animationState: CompoundTag, tick: Double): Double {
        val age = (tick - animationState.getDouble("since")) / 20.0
        if (animationState.getString("state") == "unlock") return smooth((age - 19.2) / 4.2)

        val values = sampleValues(animationState, age)
        val helix = values["helix"] ?: 0.0
        return if (animationState.getBoolean("cutShow")) {
            minOf(smooth((animationState.getDouble("cutTau") - 19.2) / 4.2), helix)
        } else {
            minOf(helix, smooth((values["invert"] ?: 0.0) * 3))
        }
    }

    fun planetAngles(animationState: CompoundTag, tick: Double, eventTick: Double): DoubleArray {
        val periods = doubleArrayOf(25.1327, 50.2655, 100.531, 150.796)
        val phases = doubleArrayOf(0.0, 140.0, 250.0, 320.0)
        val state = animationState.getString("state")
        val age = (tick - animationState.getDouble("since")) / 20.0
        val clock = chartClock(animationState, tick)

        return DoubleArray(4) { index ->
            val normal = phases[index] + 360 * clock / periods[index]
            when {
                state == "startU" -> -90.0
                state == "abort" -> animationState.getCompound("planetAt").getDouble("$index")
                state == "unlock" && age >= 10 -> -90 + maxOf(0.0, age - 16) * 360 / periods[index]
                state == "unlock" && age >= 1.5 -> {
                    val at10 = phases[index] + 360 * (clock + 10 - age) / periods[index]
                    normal + (1 - cos(PI * (age - 1.5) / 8.5)) * 0.5 * (((-90 - at10) % 360 + 720) % 360 + 360)
                }
                isParadoxState(state) -> -90 + 360 * (
                    clock - if (animationState.contains("eclipseChart")) animationState.getDouble("eclipseChart") else eventTick / 20.0 + 16
                    ) / periods[index]
                else -> normal
            }
        }
    }
}
