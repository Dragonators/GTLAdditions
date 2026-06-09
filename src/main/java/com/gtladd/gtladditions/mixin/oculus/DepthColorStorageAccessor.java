package com.gtladd.gtladditions.mixin.oculus;

import net.irisshaders.iris.gl.blending.DepthColorStorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = DepthColorStorage.class, remap = false)
public interface DepthColorStorageAccessor {

    @Invoker(value = "isDepthColorLocked", remap = false)
    static boolean gtladditions$isDepthColorLocked() {
        throw new AssertionError();
    }

    @Invoker(value = "unlockDepthColor", remap = false)
    static void gtladditions$unlockDepthColor() {
        throw new AssertionError();
    }
}