package com.gtladd.gtladditions.utils

import com.gtladd.gtladditions.utils.animation.ClientAnimationClock
import net.minecraft.nbt.CompoundTag

class SubspaceNexusAnimationClock {
    data class Frame(val animationState: CompoundTag, val tick: Double, val eventTick: Double, val eventId: Int)

    private val clock = ClientAnimationClock()
    private var sourceState = CompoundTag()
    private var animationState = CompoundTag()
    private var eventTick = 0.0
    private var eventId = 0

    fun clear() {
        clock.clear()
        sourceState = CompoundTag()
        animationState = CompoundTag()
        eventTick = 0.0
        eventId = 0
    }

    fun synchronize(state: CompoundTag, worldTick: Double, renderTick: Double, smooth: Boolean) {
        if (state.isEmpty) {
            clear()
            return
        }

        val tick = clock.sample(renderTick, smooth)
        if (tick == null) {
            clock.start(worldTick, renderTick, smooth, true)
            sourceState = state.copy()
            animationState = state.copy()
            eventTick = state.getDouble("eventTick")
            eventId = state.getInt("eventId")
            advance(worldTick)
            return
        }

        if (state == sourceState) return

        advance(tick)
        val next = state.getString("state")
        val newEvent = state.getInt("eventId") != eventId
        val completion = !newEvent && SubspaceNexusTimeline.completedState(sourceState.getString("state")) == next
        sourceState = state.copy()
        // Finite visual stages finish on smooth time. A delayed server acknowledgement must not restart one or snap its
        // phase backwards.
        if (completion) return

        if (newEvent) {
            eventId = state.getInt("eventId")
            eventTick = when {
                state.getDouble("eventTick") < 0 -> -1.0
                next == "unlock" -> tick
                next == "startU" -> tick - 40
                else -> tick - (worldTick - state.getDouble("eventTick"))
            }
        } else if (state.getDouble("eventTick") < 0) {
            eventTick = -1.0
        }

        if (next != animationState.getString("state") || newEvent) {
            // Transition from this client's current frame, not the server's older 'from' values, ship positions or
            // partially assembled rings.
            animationState = SubspaceNexusTimeline.transition(animationState, next, tick, eventTick)
        }
    }

    fun sample(renderTick: Double, smooth: Boolean): Frame? {
        val tick = clock.sample(renderTick, smooth) ?: return null
        advance(tick)
        return Frame(animationState, tick, eventTick, eventId)
    }

    private fun advance(tick: Double) {
        while (true) {
            val state = animationState.getString("state")
            val next = SubspaceNexusTimeline.completedState(state) ?: return
            val end = animationState.getDouble("since") + SubspaceNexusTimeline.duration(state) * 20
            if (tick < end) return
            animationState = SubspaceNexusTimeline.transition(animationState, next, end, eventTick)
            if (state == "startU") animationState = SubspaceNexusTimeline.joinShow(animationState, eventTick)
        }
    }
}
