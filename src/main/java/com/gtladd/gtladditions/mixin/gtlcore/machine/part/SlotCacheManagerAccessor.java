package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.integration.ae2.handler.SlotCacheManager;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SlotCacheManager.class, remap = false)
public interface SlotCacheManagerAccessor {

    @Accessor("itemBestMatchCache")
    Object2ObjectMap<Ingredient, AEItemKey> getItemMatches();

    @Accessor("fluidBestMatchCache")
    Object2ObjectMap<FluidIngredient, AEFluidKey> getFluidMatches();
}