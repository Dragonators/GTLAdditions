package com.gtladd.gtladditions.common.machine.trait

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.trait.ICapabilityTrait
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy
import com.gtladd.gtladditions.utils.TransferHelper
import com.hepdd.gtmthings.utils.FormatUtil
import com.lowdragmc.lowdraglib.side.item.IItemTransfer
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenCustomHashMap
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongMaps
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.machine.trait.MEStock.IOptimizedMEList
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.integration.ae2.AEUtils
import kotlin.math.max
import kotlin.math.min

class FastNotifiableInputItemStack(machine: MetaMachine) : NotifiableRecipeHandlerTrait<Ingredient>(machine),
    ICapabilityTrait, IItemTransfer, IOptimizedMEList, ITagSerializable<CompoundTag>, IContentChangeAware {

    private val itemInventory: Object2LongLinkedOpenCustomHashMap<ItemStack> =
        Object2LongLinkedOpenCustomHashMap(ItemStackHashStrategy.comparingAllButCount())

    init {
        itemInventory.defaultReturnValue(0)
    }

    val realSize: Int
        get() = itemInventory.size

    fun isEmpty(): Boolean = itemInventory.isEmpty()

    fun getItemStorage() = itemInventory

    fun addDisplayText(textList: MutableList<Component?>) {
        for (entry in Object2LongMaps.fastIterable(itemInventory)) {
            textList.add(
                entry.key.displayName.copy().setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))
                    .append(
                        Component.literal(FormatUtil.formatNumber(entry.longValue))
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA))
                    )
            )
        }
    }

    fun importFromNearby(vararg facings: Direction) {
        val level = machine.level ?: return
        val pos = machine.pos

        for (facing in facings) {
            TransferHelper.importToTarget(
                this,
                machine.getItemCapFilter(facing),
                level,
                pos.relative(facing),
                facing.opposite
            )
        }
    }

    fun exportToNearby(vararg facings: Direction) {
        if (isEmpty()) return

        val level = machine.level ?: return
        val pos = machine.pos

        for (facing in facings) {
            TransferHelper.exportToTarget(
                this,
                machine.getItemCapFilter(facing),
                level,
                pos.relative(facing),
                facing.opposite
            )
        }
    }

    override fun getHandlerIO(): IO = IO.IN

    override fun onContentsChanged() {
        notifyListeners()
    }

    override fun getMEItemMap(): Object2LongMap<ItemStack> = itemInventory

    override fun handleRecipeInner(
        io: IO,
        recipe: GTRecipe,
        left: MutableList<Ingredient>,
        slotName: String?,
        simulate: Boolean
    ): List<Ingredient>? {
        if (io != IO.IN) return left.ifEmpty { null }

        var changed = false
        val iter = left.listIterator()
        outer@ while (iter.hasNext()) {
            val ingredient = iter.next()
            if (ingredient.isEmpty) {
                iter.remove()
                continue
            }

            val items = ingredient.items
            if (items.isEmpty()) {
                iter.remove()
                continue
            }

            var leftAmount = if (ingredient is LongIngredient) ingredient.actualAmount else items[0].count.toLong()
            for (itemStack in items) {
                val has = itemInventory.getLong(itemStack)
                if (has <= 0) continue
                val extracted = min(has, leftAmount)
                leftAmount -= extracted
                if (!simulate) {
                    if (extracted == has)
                        itemInventory.removeLong(itemStack)
                    else
                        itemInventory.addTo(itemStack, -extracted)
                }
                changed = true

                if (leftAmount <= 0L) {
                    iter.remove()
                    continue@outer
                }
            }

            if (ingredient is LongIngredient)
                ingredient.actualAmount = leftAmount
            else
                items[0].count = Ints.saturatedCast(leftAmount)
        }

        if (!simulate && changed) onContentsChanged()

        return left.ifEmpty { null }
    }

    override fun getContents(): List<Any> {
        return itemInventory.map { entry ->
            entry.key.copyWithCount(Ints.saturatedCast(entry.value))
        }
    }

    override fun getTotalContentAmount(): Double = itemInventory.values.sum().toDouble()

    override fun getCapability(): RecipeCapability<Ingredient> = ItemRecipeCapability.CAP

    override fun getCapabilityIO(): IO = IO.IN

    override fun getSlots(): Int = max(itemInventory.size, 1)

    override fun getStackInSlot(slot: Int): ItemStack =
        itemInventory.object2LongEntrySet().elementAtOrNull(slot)?.run {
            key.copyWithCount(Ints.saturatedCast(longValue))
        } ?: ItemStack.EMPTY

    override fun insertItem(
        slot: Int,
        stack: ItemStack,
        simulate: Boolean,
        notifyChanges: Boolean
    ): ItemStack {
        if (stack.isEmpty) return ItemStack.EMPTY

        val count = stack.count
        val existing = itemInventory.getLong(stack)
        val realInsert = min(Long.MAX_VALUE - existing, count.toLong())

        if (!simulate && realInsert > 0L) {
            stack.count = 1
            itemInventory.addTo(stack, realInsert)
            if (notifyChanges) this.onContentsChanged()
        }

        return if (realInsert == count.toLong())
            ItemStack.EMPTY
        else
            stack.copyWithCount(count - realInsert.toInt())
    }

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean, notifyChanges: Boolean): ItemStack =
        ItemStack.EMPTY

    override fun getSlotLimit(slot: Int): Int = Int.MAX_VALUE

    override fun isItemValid(slot: Int, stack: ItemStack): Boolean = true

    @Suppress("UnstableApiUsage")
    override fun createSnapshot(): Any {
        throw UnsupportedOperationException("Why Fabric")
    }

    @Suppress("UnstableApiUsage")
    override fun restoreFromSnapshot(snapshot: Any?) {
        throw UnsupportedOperationException("Why Fabric")
    }

    override fun onConfigChanged() {}

    override fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()

        val itemsTag = AEUtils.createListTag(ItemStack::serializeNBT, itemInventory)
        if (!itemsTag.isEmpty()) tag.put("inventory", itemsTag)

        return tag
    }

    override fun deserializeNBT(tag: CompoundTag) {
        itemInventory.clear()

        val items = tag.getList("inventory", Tag.TAG_COMPOUND.toInt())
        AEUtils.loadInventory(items, ItemStack::of, itemInventory)
    }

    override fun setOnContentsChanged(onContentChanged: Runnable?) {}

    override fun getOnContentsChanged(): Runnable = Runnable { }
}