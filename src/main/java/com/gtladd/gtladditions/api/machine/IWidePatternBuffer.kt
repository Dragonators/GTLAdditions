package com.gtladd.gtladditions.api.machine

import appeng.api.stacks.AEKey
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.gtlcore.gtlcore.utils.datastructure.Int128

interface IWidePatternBuffer {
    /** Live owned inventory; readers must not mutate or retain its amounts for temporary arithmetic. */
    fun widePending(): Object2ObjectOpenHashMap<AEKey, Int128>?

    fun wideSlot(index: Int): IWidePatternSlot

    fun wideSlotCount(): Int
}