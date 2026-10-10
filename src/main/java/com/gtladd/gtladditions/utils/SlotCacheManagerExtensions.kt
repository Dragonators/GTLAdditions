@file:JvmName("SlotCacheManagerExtensions")

package com.gtladd.gtladditions.utils

import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gtladd.gtladditions.mixin.gtlcore.machine.part.SlotCacheManagerAccessor
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.integration.ae2.handler.SlotCacheManager
import org.gtlcore.gtlcore.utils.datastructure.Int128

fun SlotCacheManager.getBestItemMatch(
    ingredient: Ingredient,
    inventory: Object2ObjectMap<AEKey, Int128>,
    needAmount: Long
): AEItemKey? {
    val matches = (this as SlotCacheManagerAccessor).itemMatches
    val cached = matches[ingredient]
    if (cached != null && (inventory[cached]?.longValue() ?: 0L) >= needAmount) return cached

    for (stack in ingredient.items) {
        if (stack.isEmpty) continue
        val key = AEItemKey.of(stack) ?: continue
        if ((inventory[key]?.longValue() ?: 0L) >= needAmount) {
            matches[ingredient] = key
            return key
        }
    }

    return null
}

fun SlotCacheManager.getBestFluidMatch(
    ingredient: FluidIngredient,
    inventory: Object2ObjectMap<AEKey, Int128>,
    needAmount: Long
): AEFluidKey? {
    val matches = (this as SlotCacheManagerAccessor).fluidMatches
    val cached = matches[ingredient]
    if (cached != null && (inventory[cached]?.longValue() ?: 0L) >= needAmount) return cached

    for (stack in ingredient.getStacks()) {
        if (stack.isEmpty) continue
        val key = AEFluidKey.of(stack.fluid)
        if ((inventory[key]?.longValue() ?: 0L) >= needAmount) {
            matches[ingredient] = key
            return key
        }
    }

    return null
}

fun SlotCacheManager.getBestItemMatchSimulate(
    ingredient: Ingredient,
    inventory: Object2ObjectMap<AEKey, Int128>,
    catalystInventory: Object2LongMap<AEItemKey>,
    needAmount: Long
): AEItemKey? {
    val matches = (this as SlotCacheManagerAccessor).itemMatches
    val cached = matches[ingredient]
    if (cached != null && ((inventory[cached]?.longValue() ?: 0L) >= needAmount || catalystInventory.getLong(cached) >= needAmount)) return cached

    for (stack in ingredient.items) {
        if (stack.isEmpty) continue
        val key = AEItemKey.of(stack) ?: continue
        if ((inventory[key]?.longValue() ?: 0L) >= needAmount || catalystInventory.getLong(key) >= needAmount) {
            matches[ingredient] = key
            return key
        }
    }

    return null
}

fun SlotCacheManager.getBestFluidMatchSimulate(
    ingredient: FluidIngredient,
    inventory: Object2ObjectMap<AEKey, Int128>,
    catalystInventory: Object2LongMap<AEFluidKey>,
    needAmount: Long
): AEFluidKey? {
    val matches = (this as SlotCacheManagerAccessor).fluidMatches
    val cached = matches[ingredient]
    if (cached != null && ((inventory[cached]?.longValue() ?: 0L) >= needAmount || catalystInventory.getLong(cached) >= needAmount)) return cached

    for (stack in ingredient.getStacks()) {
        if (stack.isEmpty) continue
        val key = AEFluidKey.of(stack.fluid)
        if ((inventory[key]?.longValue() ?: 0L) >= needAmount || catalystInventory.getLong(key) >= needAmount) {
            matches[ingredient] = key
            return key
        }
    }

    return null
}