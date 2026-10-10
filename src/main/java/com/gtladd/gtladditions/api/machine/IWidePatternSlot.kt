package com.gtladd.gtladditions.api.machine

import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.KeyCounter
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.gtlcore.gtlcore.utils.datastructure.Int128

interface IWidePatternSlot {
    /** Live owned inventory; quantity writes belong to the buffer's input, refund and restore paths. */
    fun wideInputs(): Object2ObjectOpenHashMap<AEKey, Int128>

    fun canPushWide(inputs: Array<KeyCounter>): Boolean

    fun catalystItems(): Object2LongMap<AEItemKey>

    fun catalystFluids(): Object2LongMap<AEFluidKey>
}