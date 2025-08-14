package com.gtladd.gtladditions.mixin.ae2;

import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "appeng.api.stacks.AEKey2LongMap$OpenHashMap", remap = false)
public abstract class AEKey2LongMapMixin extends Object2LongOpenHashMap<AEKey> {

    @Override
    public long addTo(AEKey key, long incr) {
        long oldValue = getLong(key);
        long newValue;

        if ((incr > 0 && oldValue > Long.MAX_VALUE - incr) ||
                (incr < 0 && oldValue < Long.MIN_VALUE - incr)) {
            newValue = incr > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        } else {
            newValue = oldValue + incr;
        }

        put(key, newValue);
        return oldValue;
    }
}
