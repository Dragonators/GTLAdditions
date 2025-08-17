package com.gtladd.gtladditions.common.machine.muiltblock.part;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;

import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import com.google.gson.JsonParser;
import com.gtladd.gtladditions.utils.CacheHashStrategies;
import it.unimi.dsi.fastutil.objects.*;

/**
 * 负责管理单个槽位的所有缓存数据，包括：
 * - BestMatch缓存：ingredient到配方实际使用AEKey的映射
 * - 缓存失效和清理
 */
public class SlotCacheManager implements ITagSerializable<CompoundTag> {

    private final Object2ObjectMap<Ingredient, AEItemKey> itemBestMatchCache = new Object2ObjectOpenCustomHashMap<>(CacheHashStrategies.IngredientHashStrategy.INSTANCE);
    private final Object2ObjectMap<FluidIngredient, AEFluidKey> fluidBestMatchCache = new Object2ObjectOpenCustomHashMap<>(CacheHashStrategies.FluidIngredientHashStrategy.INSTANCE);

    /**
     * 获取或计算item的最佳匹配
     * 
     * @param ingredient 原料
     * @param inventory  物品库存映射
     * @param needAmount 需要的数量
     * @return 最佳匹配的AEItemKey，如果没有足够库存则返回null
     */
    public AEItemKey getBestItemMatch(Ingredient ingredient, Object2LongMap<AEItemKey> inventory, long needAmount) {
        AEItemKey cached = itemBestMatchCache.get(ingredient);
        if (cached != null && inventory.getLong(cached) >= needAmount) {
            return cached;
        }

        AEItemKey bestMatch = findBestItemMatch(ingredient, inventory, needAmount);
        if (bestMatch != null) {
            itemBestMatchCache.put(ingredient, bestMatch);
        }
        return bestMatch;
    }

    /**
     * 获取或计算fluid的最佳匹配
     * 
     * @param ingredient 流体原料
     * @param inventory  流体库存映射
     * @param needAmount 需要的数量
     * @return 最佳匹配的AEFluidKey，如果没有足够库存则返回null
     */
    public AEFluidKey getBestFluidMatch(FluidIngredient ingredient, Object2LongMap<AEFluidKey> inventory, long needAmount) {
        AEFluidKey cached = fluidBestMatchCache.get(ingredient);
        if (cached != null && inventory.getLong(cached) >= needAmount) {
            return cached;
        }

        AEFluidKey bestMatch = findBestFluidMatch(ingredient, inventory, needAmount);
        if (bestMatch != null) {
            fluidBestMatchCache.put(ingredient, bestMatch);
        }
        return bestMatch;
    }

    private static AEItemKey findBestItemMatch(Ingredient ingredient, Object2LongMap<AEItemKey> inventory, long needAmount) {
        var items = ingredient.getItems();
        for (var item : items) {
            if (!item.isEmpty()) {
                AEItemKey aeKey = AEItemKey.of(item);
                if (inventory.getLong(aeKey) >= needAmount) {
                    return aeKey;
                }
            }
        }
        return null;
    }

    private static AEFluidKey findBestFluidMatch(FluidIngredient ingredient, Object2LongMap<AEFluidKey> inventory, long needAmount) {
        var stacks = ingredient.getStacks();
        for (var stack : stacks) {
            if (!stack.isEmpty()) {
                AEFluidKey aeKey = AEFluidKey.of(stack.getFluid());
                if (inventory.getLong(aeKey) >= needAmount) {
                    return aeKey;
                }
            }
        }
        return null;
    }

    public void clearAllCaches() {
        itemBestMatchCache.clear();
        fluidBestMatchCache.clear();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        if (!itemBestMatchCache.isEmpty()) {
            ListTag itemCacheList = new ListTag();
            for (var entry : Object2ObjectMaps.fastIterable(itemBestMatchCache)) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.put("ingredient", serializeIngredient(entry.getKey()));
                entryTag.put("aekey", entry.getValue().toTag());
                itemCacheList.add(entryTag);
            }
            tag.put("itemCache", itemCacheList);
        }

        if (!fluidBestMatchCache.isEmpty()) {
            ListTag fluidCacheList = new ListTag();
            for (var entry : Object2ObjectMaps.fastIterable(fluidBestMatchCache)) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.put("ingredient", serializeFluidIngredient(entry.getKey()));
                entryTag.put("aekey", entry.getValue().toTag());
                fluidCacheList.add(entryTag);
            }
            tag.put("fluidCache", fluidCacheList);
        }

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        clearAllCaches();

        if (tag.contains("itemCache", Tag.TAG_LIST)) {
            ListTag itemCacheList = tag.getList("itemCache", Tag.TAG_COMPOUND);
            for (Tag t : itemCacheList) {
                if (!(t instanceof CompoundTag entryTag)) continue;
                Ingredient ingredient = deserializeIngredient(entryTag.getCompound("ingredient"));
                AEItemKey aeKey = AEItemKey.fromTag(entryTag.getCompound("aekey"));
                if (ingredient != null && aeKey != null) {
                    itemBestMatchCache.put(ingredient, aeKey);
                }
            }
        }

        if (tag.contains("fluidCache", Tag.TAG_LIST)) {
            ListTag fluidCacheList = tag.getList("fluidCache", Tag.TAG_COMPOUND);
            for (Tag t : fluidCacheList) {
                if (!(t instanceof CompoundTag entryTag)) continue;
                FluidIngredient ingredient = deserializeFluidIngredient(entryTag.getCompound("ingredient"));
                AEFluidKey aeKey = AEFluidKey.fromTag(entryTag.getCompound("aekey"));
                if (ingredient != null && aeKey != null) {
                    fluidBestMatchCache.put(ingredient, aeKey);
                }
            }
        }
    }

    private static CompoundTag serializeIngredient(Ingredient ingredient) {
        CompoundTag tag = new CompoundTag();
        var json = ingredient.toJson();
        tag.putString("json", json.toString());
        return tag;
    }

    private static CompoundTag serializeFluidIngredient(FluidIngredient ingredient) {
        CompoundTag tag = new CompoundTag();
        var json = ingredient.toJson();
        tag.putString("json", json.toString());
        return tag;
    }

    private static Ingredient deserializeIngredient(CompoundTag tag) {
        try {
            String jsonStr = tag.getString("json");
            if (jsonStr.isEmpty()) return null;
            var json = JsonParser.parseString(jsonStr);
            return Ingredient.fromJson(json, false);
        } catch (Exception e) {
            return null;
        }
    }

    private static FluidIngredient deserializeFluidIngredient(CompoundTag tag) {
        try {
            String jsonStr = tag.getString("json");
            if (jsonStr.isEmpty()) return null;
            var json = JsonParser.parseString(jsonStr);
            return FluidIngredient.fromJson(json);
        } catch (Exception e) {
            return null;
        }
    }
}
