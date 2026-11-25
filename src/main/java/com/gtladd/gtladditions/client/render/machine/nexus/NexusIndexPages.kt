package com.gtladd.gtladditions.client.render.machine.nexus

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL15.*
import java.lang.ref.WeakReference
import java.util.BitSet

internal class NexusIndexPages(private val source: IntArray, val pageSize: Int) {
    private val pages = source.size / pageSize
    private val slotForPage = IntArray(pages) { -1 }
    private val pageForSlot = IntArray(pages)
    private val dirty = BitSet(pages)
    private val data = BufferUtils.createIntBuffer(source.size)
    private var id = 0
    var size = 0
        private set
    val indexCount: Int get() = size * pageSize
    val residentBytes: Long get() = source.size.toLong() * Int.SIZE_BYTES
    var uploadedBytes = 0L
        private set

    init {
        check(source.size % pageSize == 0)
    }

    fun contains(page: Int): Boolean = slotForPage[page] >= 0

    fun setVisible(first: Int, count: Int, visible: Boolean) {
        for (page in first until first + count) {
            val slot = slotForPage[page]
            if (visible && slot < 0) {
                val next = size++
                slotForPage[page] = next
                pageForSlot[next] = page
                write(next, page)
            } else if (!visible && slot >= 0) {
                val tail = --size
                if (slot != tail) {
                    val moved = pageForSlot[tail]
                    slotForPage[moved] = slot
                    pageForSlot[slot] = moved
                    write(slot, moved)
                }

                slotForPage[page] = -1
                dirty.clear(tail)
            }
        }
    }

    private fun write(slot: Int, page: Int) {
        data.limit(data.capacity())
        data.position(slot * pageSize)
        data.put(source, page * pageSize, pageSize)
        dirty.set(slot)
    }

    fun bind() {
        if (id == 0) {
            collect()
            id = glGenBuffers()
            allocations.add(Allocation(WeakReference(this), id))
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, residentBytes, GL_DYNAMIC_DRAW)
            dirty.set(0, size)
        } else {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id)
        }

        var first = dirty.nextSetBit(0)
        while (first >= 0 && first < size) {
            val end = minOf(size, dirty.nextClearBit(first))
            data.limit(end * pageSize).position(first * pageSize)
            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, first.toLong() * pageSize * Int.SIZE_BYTES, data)
            uploadedBytes += (end - first).toLong() * pageSize * Int.SIZE_BYTES
            first = dirty.nextSetBit(end)
        }

        dirty.clear()
    }

    companion object {
        private data class Allocation(val owner: WeakReference<NexusIndexPages>, val id: Int)
        private val allocations = ArrayList<Allocation>()

        fun collect() {
            val iterator = allocations.iterator()

            while (iterator.hasNext()) {
                val allocation = iterator.next()
                if (allocation.owner.get() == null) {
                    glDeleteBuffers(allocation.id)
                    iterator.remove()
                }
            }
        }

        fun release() {
            for (allocation in allocations) {
                allocation.owner.get()?.id = 0
                glDeleteBuffers(allocation.id)
            }
            allocations.clear()
        }
    }
}
