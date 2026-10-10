package com.gtladd.gtladditions.integration.ae2

import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import com.gregtechceu.gtceu.integration.jade.GTElementHelper
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.material.Fluid
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachineBase
import org.gtlcore.gtlcore.utils.Registries
import org.gtlcore.gtlcore.utils.datastructure.Int128
import snownee.jade.api.ITooltip
import snownee.jade.api.fluid.JadeFluidObject
import java.math.BigDecimal
import java.math.BigInteger

object WideBufferDisplay {
    const val MARKER = "gtladdWideBufferDisplay"
    private const val COUNT = "precise_count"

    fun outgoingAmounts(pending: Map<AEKey, Int128>): Map<AEKey, BigInteger> {
        val amounts = linkedMapOf<AEKey, BigInteger>()
        pending.forEach { (key, amount) -> amounts.merge(key, amount.toBigInteger(), BigInteger::add) }
        return amounts
    }

    fun write(tag: CompoundTag, machine: MEPatternBufferPartMachineBase, pendingAmounts: Map<AEKey, Int128>) {
        tag.putBoolean(MARKER, true)
        tag.putInt("proxies", machine.proxies.size)

        val items = linkedMapOf<Item, BigInteger>()
        val fluids = linkedMapOf<Fluid, BigInteger>()
        val slots = machine as WidePatternSlots

        for (index in 0 until slots.wideSlotCount()) {
            val slot = slots.wideSlot(index)
            slot.wideInputs().forEach { (key, amount) ->
                when (key) {
                    is AEItemKey -> items.merge(key.item, amount.toBigInteger(), BigInteger::add)
                    is AEFluidKey -> fluids.merge(key.fluid, amount.toBigInteger(), BigInteger::add)
                }
            }

            slot.catalystItems().forEach { (key, amount) ->
                if (amount > 0L) items.merge(key.item, BigInteger.valueOf(amount), BigInteger::add)
            }
            slot.catalystFluids().forEach { (key, amount) ->
                if (amount > 0L) fluids.merge(key.fluid, BigInteger.valueOf(amount), BigInteger::add)
            }
        }

        machine.sharedCatalystInventory.contents.forEach {
            if (it is ItemStack && !it.isEmpty) items.merge(it.item, BigInteger.valueOf(it.count.toLong()), BigInteger::add)
        }
        machine.sharedCatalystTank.contents.forEach {
            if (it is com.lowdragmc.lowdraglib.side.fluid.FluidStack && !it.isEmpty) fluids.merge(it.fluid, BigInteger.valueOf(it.amount), BigInteger::add)
        }

        val itemTags = ListTag()
        items.forEach { (item, count) ->
            val key = Registries.getResourceKey(item) ?: return@forEach
            itemTags.add(
                CompoundTag().apply {
                    putString("item", key.toString())
                    putString(COUNT, count.toString())
                }
            )
        }
        tag.put("items", itemTags)

        val fluidTags = ListTag()
        fluids.forEach { (fluid, count) ->
            val key = Registries.getResourceKey(fluid) ?: return@forEach
            fluidTags.add(
                CompoundTag().apply {
                    putString("fluid", key.toString())
                    putString(COUNT, count.toString())
                }
            )
        }
        tag.put("fluids", fluidTags)

        val pending = ListTag()
        outgoingAmounts(pendingAmounts).forEach { (key, count) ->
            pending.add(
                CompoundTag().apply {
                    put("key", key.toTagGeneric())
                    putString(COUNT, count.toString())
                }
            )
        }
        tag.put("pending", pending)
    }

    fun read(tooltip: ITooltip, data: CompoundTag) {
        tooltip.add(Component.translatable("gtceu.top.proxies_bound", data.getInt("proxies")).withStyle(ChatFormatting.LIGHT_PURPLE))

        for (value in data.getList("items", Tag.TAG_COMPOUND.toInt())) {
            val tag = value as CompoundTag
            val item = Registries.getItem(tag.getString("item")) ?: continue
            item(tooltip, ItemStack(item), tag.getString(COUNT))
        }

        for (value in data.getList("fluids", Tag.TAG_COMPOUND.toInt())) {
            val tag = value as CompoundTag
            val fluid = Registries.getFluid(tag.getString("fluid")) ?: continue
            fluid(tooltip, fluid, tag.getString(COUNT))
        }

        val pending = data.getList("pending", Tag.TAG_COMPOUND.toInt())
        if (pending.isNotEmpty()) {
            tooltip.add(Component.translatable("gtceu.top.pending_refunds").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            for (value in pending) {
                val tag = value as CompoundTag
                when (val key = AEKey.fromTagGeneric(tag.getCompound("key"))) {
                    is AEItemKey -> item(tooltip, key.toStack(1), tag.getString(COUNT))
                    is AEFluidKey -> fluid(tooltip, key.fluid, tag.getString(COUNT))
                }
            }
        }
    }

    private fun item(tooltip: ITooltip, stack: ItemStack, count: String) {
        tooltip.add(tooltip.elementHelper.smallItem(stack))
        tooltip.append(Component.literal(" $count × ").withStyle(ChatFormatting.DARK_PURPLE).append(stack.hoverName.copy().withStyle(ChatFormatting.GOLD)))
    }

    private fun fluid(tooltip: ITooltip, fluid: Fluid, count: String) {
        tooltip.add(GTElementHelper.smallFluid(JadeFluidObject.of(fluid)))
        tooltip.append(
            Component.literal(" ${formatFluid(BigInteger(count))} ").withStyle(ChatFormatting.DARK_PURPLE)
                .append(fluid.fluidType.description.copy().withStyle(ChatFormatting.DARK_AQUA))
        )
    }

    fun formatFluid(amount: BigInteger): String = if (amount < BigInteger.valueOf(1000L)) {
        "$amount mB"
    } else {
        "${BigDecimal(amount, 3).stripTrailingZeros().toPlainString()} B"
    }
}