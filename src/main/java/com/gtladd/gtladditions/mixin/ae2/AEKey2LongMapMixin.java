package com.gtladd.gtladditions.mixin.ae2;

import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "appeng.api.stacks.AEKey2LongMap$OpenHashMap", remap = false)
public abstract class AEKey2LongMapMixin extends Object2LongOpenHashMap<AEKey> {

    @Override
    public long addTo(AEKey key, long incr) {
        long oldValue = getLong(key);
        
        // Fast path: zero increment
        if (incr == 0) {
            return oldValue;
        }
        
        long newValue = oldValue + incr;
        
        // Check for overflow using bitwise operations (faster than bounds checking)
        // Overflow occurs when operands have same sign but result has different sign
        if (((oldValue ^ newValue) & (incr ^ newValue)) < 0) {
            newValue = incr > 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
        }

        put(key, newValue);
        return oldValue;
    }
}
