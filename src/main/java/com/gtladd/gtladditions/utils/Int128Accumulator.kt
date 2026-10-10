package com.gtladd.gtladditions.utils

import org.gtlcore.gtlcore.utils.datastructure.Int128

/** Stateless helpers for single-copy batch arithmetic; all per-batch values stay in caller-owned maps. */
object Int128Accumulator {
    @JvmOverloads
    fun copyOf(amount: Int128? = null): Int128 = amount?.copy() ?: Int128.ZERO()

    /** Copies the inventory amount only when its key first enters the temporary batch. */
    fun <K> getOrCopy(batch: MutableMap<K, Int128>, inventory: Map<K, Int128>, key: K): Int128 {
        require(batch !== inventory) { "Int128 batch must be separate from inventory" }

        return batch.getOrPut(key) { copyOf(inventory[key]) }
    }

    /** Transfers temporary values without copying; callers must stop using them after commit. */
    fun <K> commit(batch: MutableMap<K, Int128>, inventory: MutableMap<K, Int128>) {
        require(batch !== inventory) { "Int128 batch must be separate from inventory" }

        batch.forEach { (key, amount) ->
            if (amount.isZero) inventory.remove(key) else inventory[key] = amount
        }

        batch.clear()
    }
}