package com.gtladd.gtladditions.api.machine.logic.chemical

internal class ExoticChemicalParallelPool(unitCount: Int) {

    private val limits = LongArray(unitCount)
    private val shares = LongArray(unitCount)
    private var dirty = true
    private var cachedStructureTier = 0
    private var cachedCapacity = 0L

    // ========================================
    // CONFIGURED LIMITS
    // ========================================

    fun limitOf(id: Int): Long = limits[id]

    fun setLimit(id: Int, limit: Long) {
        limits[id] = limit
        // Activation and return can change membership even when the limit stays at zero.
        dirty = true
    }

    inline fun maxLimit(id: Int, structureTier: Int, capacity: Long, isIndependent: (Int) -> Boolean): Long {
        if (id !in 1 until limits.size || structureTier < 2) return 0L
        if (structureTier >= 3) return Long.MAX_VALUE

        var free = capacity
        for (other in 1 until limits.size) {
            if (other != id && isIndependent(other)) free -= minOf(free, limits[other])
        }
        return free
    }

    // ========================================
    // EFFECTIVE SHARES
    // ========================================

    private inline fun refreshShares(structureTier: Int, capacity: Long, isIndependent: (Int) -> Boolean) {
        if (!dirty && cachedStructureTier == structureTier && cachedCapacity == capacity) return

        var remaining = capacity
        for (id in 1 until shares.size) {
            val share = if (structureTier == 2 && isIndependent(id)) minOf(limits[id], remaining) else 0L
            shares[id] = share
            remaining -= share
        }
        shares[0] = remaining

        cachedStructureTier = structureTier
        cachedCapacity = capacity
        dirty = false
    }

    inline fun shareOf(id: Int, structureTier: Int, capacity: Long, isIndependent: (Int) -> Boolean): Long {
        refreshShares(structureTier, capacity, isIndependent)
        return shares[id]
    }

    inline fun available(
        id: Int,
        structureTier: Int,
        capacity: Long,
        isIndependent: (Int) -> Boolean,
        usedLimitOf: (Int) -> Long
    ): Long {
        refreshShares(structureTier, capacity, isIndependent)
        var free = capacity
        for (other in shares.indices) {
            if (other != id) free -= minOf(free, maxOf(shares[other], usedLimitOf(other)))
        }
        return minOf(shares[id], free)
    }

    // ========================================
    // PERSISTENCE
    // ========================================

    fun save(): LongArray = limits.copyOf()

    fun load(saved: LongArray) {
        for (id in limits.indices) {
            limits[id] = saved.getOrElse(id) { 0L }.coerceAtLeast(0L)
        }
        dirty = true
    }
}