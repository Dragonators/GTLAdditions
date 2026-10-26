package com.gtladd.gtladditions.mixin.gtceu.api.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(value = MultiblockControllerMachine.class, priority = 900)
public abstract class MultiblockControllerMachineMixin {

    @Shadow(remap = false)
    public abstract Lock getPatternLock();

    @Dynamic("GTLCore supplies these overrides of IMultiController's default methods.")
    @WrapMethod(method = { "checkPatternWithLock", "checkPatternWithTryLock" }, remap = false)
    private boolean gtladditions$releaseLeakedPatternLock(Operation<Boolean> original) {
        // GTCEu creates a ReentrantLock; preserve custom lock implementations unchanged.
        if (!(getPatternLock() instanceof ReentrantLock lock)) return original.call();
        int previousHolds = lock.getHoldCount();
        try {
            return original.call();
        } finally {
            // Newer Core versions already balance their acquisition. Do not release a caller's lock.
            while (lock.getHoldCount() > previousHolds) lock.unlock();
        }
    }
}