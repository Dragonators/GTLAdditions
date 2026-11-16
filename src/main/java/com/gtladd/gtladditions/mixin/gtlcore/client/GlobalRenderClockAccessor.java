package com.gtladd.gtladditions.mixin.gtlcore.client;

import org.gtlcore.gtlcore.client.GlobalRenderClock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GlobalRenderClock.class, remap = false)
public interface GlobalRenderClockAccessor {

    @Invoker(value = "getElapsedNanos", remap = false)
    static long invokeGetElapsedNanos() {
        throw new AssertionError();
    }
}