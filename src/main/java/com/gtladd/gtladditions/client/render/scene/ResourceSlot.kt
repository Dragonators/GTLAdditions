package com.gtladd.gtladditions.client.render.scene

import com.gtladd.gtladditions.GTLAdditions

/** Render thread only. The slot is the one closer of its values. */
internal class ResourceSlot<T : AutoCloseable>(private val owner: String) {
    enum class State { EMPTY, PENDING, READY, FAILED }

    var state = State.EMPTY
        private set
    private var current: T? = null

    val value: T?
        get() = current.takeIf { state == State.READY }

    fun invalidate() {
        state = State.PENDING
    }

    fun publish(next: T) {
        val previous = current
        current = next
        state = State.READY
        previous?.close()
    }

    fun fail(error: Exception) {
        val previous = current
        current = null
        state = State.FAILED
        GTLAdditions.LOGGER.error("Unable to load {}", owner, error)
        previous?.close()
    }
}
