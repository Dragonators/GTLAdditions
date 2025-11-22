package com.gtladd.gtladditions.mixin.mc;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.povstalec.sgjourney.StargateJourney;

import com.google.gson.JsonElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"))
    private void filterRecipes(Map<ResourceLocation, JsonElement> map,
                               ResourceManager rm,
                               ProfilerFiller profiler,
                               CallbackInfo ci) {
        map.entrySet().removeIf(e -> e.getKey().getNamespace().equals(StargateJourney.MODID) ||
                e.getKey().getPath().equals("endless_cake") ||
                e.getKey().getPath().equals("ultimate_stew") ||
                e.getKey().getPath().equals("infinity_bucket"));
    }
}
