package com.gtladd.gtladditions.integration.ae2

import appeng.api.networking.IGrid
import appeng.api.networking.security.IActionSource
import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.storage.StorageHelper
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient
import com.gtladd.gtladditions.utils.compareTo
import com.gtladd.gtladditions.utils.longValue
import com.gtladd.gtladditions.utils.saturatedAdd
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.utils.datastructure.Int128
import org.gtlcore.gtlcore.utils.datastructure.Int128.ZERO
import java.util.function.Function

object WideBufferTools {

    // ========================================
    // Recipe and AE interface adapters
    // ========================================

    fun refundSlot(pending: MutableMap<AEKey, Int128>, inputs: MutableMap<AEKey, Int128>) {
        inputs.forEach { (key, amount) ->
            // A fresh receiver keeps the source amount independently owned, including new keys.
            if (amount > 0) pending.getOrPut(key) { ZERO() }.saturatedAdd(amount)
        }
        inputs.clear()
    }

    fun itemOutputs(
        pending: Object2ObjectOpenHashMap<AEKey, Int128>,
        left: List<Ingredient>,
        simulate: Boolean
    ): List<Ingredient> {
        if (simulate) return emptyList()

        for (ingredient in left) {
            if (ingredient is IntProviderIngredient) {
                ingredient.setItemStacks(null)
                ingredient.setSampledCount(null)
            }

            val stack = ingredient.items.firstOrNull() ?: continue
            if (stack.isEmpty) continue
            val key = AEItemKey.of(stack) ?: continue
            val amount = if (ingredient is LongIngredient) ingredient.actualAmount else stack.count.toLong()
            if (amount > 0L) pending.getOrPut(key) { ZERO() }.saturatedAdd(amount)
        }

        return emptyList()
    }

    fun fluidOutputs(
        pending: Object2ObjectOpenHashMap<AEKey, Int128>,
        left: List<FluidIngredient>,
        simulate: Boolean
    ): List<FluidIngredient> {
        if (simulate) return emptyList()

        for (ingredient in left) {
            val stack = ingredient.getStacks().firstOrNull() ?: continue
            if (!stack.isEmpty) pending.getOrPut(AEFluidKey.of(stack.fluid)) { ZERO() }.saturatedAdd(stack.amount)
        }

        return emptyList()
    }

    fun flushToNetwork(
        pending: Object2ObjectOpenHashMap<AEKey, Int128>,
        network: IGrid?,
        source: IActionSource
    ): Boolean {
        if (pending.isEmpty() || network == null) return false

        val inventory = network.storageService.inventory
        val energy = network.energyService
        var changed = false
        var failures = 0
        var operations = 0
        val iterator = Object2ObjectMaps.fastIterator(pending)

        while (iterator.hasNext() && operations < 64) {
            val entry = iterator.next()
            val before = entry.value
            if (before <= 0) {
                iterator.remove()
                continue
            }

            val inserted = StorageHelper.poweredInsert(energy, inventory, entry.key, before.longValue(), source)
            operations++

            if (inserted > 0L) {
                before.add(-inserted)
                if (before <= 0) iterator.remove()
                changed = true
                failures = 0
            } else if (++failures >= 5) {
                break
            }
        }

        return changed
    }

    fun itemLongAmounts(amounts: Map<AEKey, Int128>): Object2LongOpenHashMap<AEItemKey> = Object2LongOpenHashMap<AEItemKey>().also { view ->
        amounts.forEach { (key, amount) ->
            if (key is AEItemKey) view.put(key, amount.longValue())
        }
    }

    fun fluidLongAmounts(amounts: Map<AEKey, Int128>): Object2LongOpenHashMap<AEFluidKey> = Object2LongOpenHashMap<AEFluidKey>().also { view ->
        amounts.forEach { (key, amount) ->
            if (key is AEFluidKey) view.put(key, amount.longValue())
        }
    }

    // ========================================
    // Persistence
    // ========================================

    fun <K> writeAmounts(amounts: Map<K, Int128>, serializer: Function<K, CompoundTag>): ListTag {
        val result = ListTag()
        amounts.forEach { (key, amount) ->
            result.add(
                serializer.apply(key).apply {
                    putLongArray("quantity", longArrayOf(amount.high, amount.low))
                }
            )
        }

        return result
    }

    @JvmOverloads
    fun <K> readAmounts(
        entries: ListTag,
        extractor: Function<CompoundTag, K?>,
        onLegacyLoaded: Runnable? = null
    ): Object2ObjectMap<K, Int128> {
        val loaded = Object2ObjectOpenHashMap<K, Int128>()
        if (entries.isEmpty()) return loaded

        fun readEntries(readAmount: (CompoundTag) -> Int128?) {
            for (value in entries) {
                if (value !is CompoundTag) continue
                val key = extractor.apply(value) ?: continue
                val amount = readAmount(value) ?: continue
                if (amount.isPositive) loaded.getOrPut(key) { ZERO() }.saturatedAdd(amount)
            }
        }

        if (entries.getCompound(0).contains("quantity")) {
            readEntries { value ->
                val quantity = value.getLongArray("quantity")
                if (quantity.size == 2) Int128(quantity[0], quantity[1]) else null
            }
        } else {
            readEntries { value -> Int128(value.getLong("real")) }
            onLegacyLoaded?.run()
        }

        return loaded
    }
}