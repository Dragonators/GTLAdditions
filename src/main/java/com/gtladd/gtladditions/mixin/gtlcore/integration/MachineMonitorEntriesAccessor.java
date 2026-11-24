package com.gtladd.gtladditions.mixin.gtlcore.integration;

import org.gtlcore.gtlcore.integration.monitor.MachineMonitorEntries;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import net.minecraft.nbt.ListTag;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Map;

@Mixin(value = MachineMonitorEntries.class, remap = false)
public interface MachineMonitorEntriesAccessor {

    @Invoker(value = "append", remap = false)
    static void invokeAppend(ListTag rows, Map<RecipeCapability<?>, List<Content>> contents, boolean tick) {
        throw new AssertionError();
    }
}