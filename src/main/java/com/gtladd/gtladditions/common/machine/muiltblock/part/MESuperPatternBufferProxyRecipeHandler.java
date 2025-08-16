package com.gtladd.gtladditions.common.machine.muiltblock.part;

import org.gtlcore.gtlcore.api.machine.trait.IMERecipeHandlerTrait;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import com.gtladd.gtladditions.api.machine.trait.NotifiableMERecipeHandlerTrait;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

public class MESuperPatternBufferProxyRecipeHandler<T> extends NotifiableMERecipeHandlerTrait<T> {

    @Setter
    private IMERecipeHandlerTrait<T> handler;
    private final RecipeCapability<T> capability;

    public MESuperPatternBufferProxyRecipeHandler(MetaMachine machine, RecipeCapability<T> capability) {
        super(machine);
        this.capability = capability;
    }

    @Override
    public RecipeCapability<T> getCapability() {
        return capability;
    }

    @Override
    public List<Integer> getActiveSlots(RecipeCapability<?> recipeCapability) {
        if (handler != null) {
            return handler.getActiveSlots(recipeCapability);
        }
        return Collections.emptyList();
    }

    @Override
    public Int2ObjectMap<List<Object>> getActiveSlotsLimitContentsMap() {
        if (handler != null) {
            return handler.getActiveSlotsLimitContentsMap();
        }
        return Int2ObjectMaps.emptyMap();
    }

    @Override
    public Object2LongMap<?> getCustomSlotsStackMap(List<Integer> slots) {
        if (handler != null) {
            return handler.getCustomSlotsStackMap(slots);
        }
        return Object2LongMaps.EMPTY_MAP;
    }

    @Override
    public boolean meHandleRecipeInner(GTRecipe recipe, Object2LongMap<?> left, boolean simulate, int trySlot) {
        if (handler != null) {
            return handler.meHandleRecipeInner(recipe, left, simulate, trySlot);
        }
        return false;
    }

    @Override
    public void prepareMEHandleContents(GTRecipe recipe, List<T> left, boolean simulate) {
        if (handler != null) {
            handler.prepareMEHandleContents(recipe, left, simulate);
        }
    }

    @Override
    public Object2LongMap<?> getPreparedMEHandleContents() {
        if (handler != null) {
            return handler.getPreparedMEHandleContents();
        }
        return Object2LongMaps.EMPTY_MAP;
    }
}
