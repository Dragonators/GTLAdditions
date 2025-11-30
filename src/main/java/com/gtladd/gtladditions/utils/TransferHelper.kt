package com.gtladd.gtladditions.utils

import com.google.common.primitives.Ints
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputFluidTank
import com.gtladd.gtladditions.common.machine.trait.FastNotifiableInputItemStack
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper
import com.lowdragmc.lowdraglib.side.item.ItemTransferHelper
import it.unimi.dsi.fastutil.objects.Object2LongMaps
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import java.util.function.Predicate

object TransferHelper {

    fun exportToTarget(
        source: FastNotifiableInputFluidTank,
        filter: Predicate<FluidStack>,
        level: Level,
        pos: BlockPos,
        direction: Direction?
    ) {
        if (source.isEmpty()) return

        val target = FluidTransferHelper.getFluidTransfer(level, pos, direction) ?: return

        var changed = false
        val iterator = source.getFluidStorage().iterator()
        while (iterator.hasNext()) {
            val currentFluid = iterator.next()

            if (currentFluid.isEmpty || !filter.test(currentFluid)) continue

            val toDrain = currentFluid.copy()
            var remainAmount = currentFluid.amount
            do {
                val filled = target.fill(toDrain, true)
                if (filled > 0) {
                    toDrain.amount = filled
                    target.fill(toDrain, false)
                    remainAmount -= filled
                    currentFluid.amount -= filled
                    changed = true
                } else {
                    break
                }
                toDrain.amount = remainAmount
            } while (!toDrain.isEmpty)

            if (currentFluid.amount <= 0) iterator.remove()
        }

        if (changed) {
            source.onContentsChanged()
        }
    }

    fun exportToTarget(
        source: FastNotifiableInputItemStack,
        filter: Predicate<ItemStack>,
        level: Level,
        pos: BlockPos,
        direction: Direction?
    ) {
        if (source.isEmpty()) return

        val target = ItemTransferHelper.getItemTransfer(level, pos, direction) ?: return

        var changed = false
        val iterator = Object2LongMaps.fastIterator(source.getItemStorage())
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val stack = entry.key
            val count = entry.longValue

            if (stack.isEmpty || !filter.test(stack)) continue

            val tryTransferStack = stack.copy()
            var remainCount = count
            do {
                val intCount = Ints.saturatedCast(remainCount)
                tryTransferStack.count = intCount
                val remainder = ItemTransferHelper.insertItem(target, tryTransferStack, true)
                val amountToInsert = intCount - remainder.count

                if (amountToInsert > 0) {
                    tryTransferStack.count = amountToInsert
                    ItemTransferHelper.insertItem(target, tryTransferStack, false)
                    remainCount -= amountToInsert
                    changed = true
                } else {
                    break
                }
            } while (remainCount > 0)

            if (remainCount > 0) {
                if (remainCount != count) entry.setValue(remainCount)
            } else {
                iterator.remove()
            }
        }

        if (changed) {
            source.onContentsChanged()
        }
    }

    fun importToTarget(
        target: FastNotifiableInputItemStack,
        filter: Predicate<ItemStack>,
        level: Level,
        pos: BlockPos,
        direction: Direction?
    ) {
        val source = ItemTransferHelper.getItemTransfer(level, pos, direction) ?: return

        var changed = false
        val inventory = target.getItemStorage()

        for (srcIndex in 0 until source.slots) {
            val sourceStack = source.extractItem(srcIndex, Int.MAX_VALUE, true, false)
            if (sourceStack.isEmpty || !filter.test(sourceStack)) continue

            val extractCount = sourceStack.count.toLong()
            val existing = inventory.getLong(sourceStack)
            val canInsert = Long.MAX_VALUE - existing

            if (canInsert > 0) {
                val toExtract = Ints.saturatedCast(canInsert.coerceAtMost(extractCount))
                val extracted = source.extractItem(srcIndex, toExtract, false, false)

                if (!extracted.isEmpty) {
                    val actualExtracted = extracted.count.toLong()
                    extracted.count = 1
                    inventory.addTo(extracted, actualExtracted)
                    changed = true
                }
            }
        }

        if (changed) {
            target.onContentsChanged()
        }
    }

    fun importToTarget(
        target: FastNotifiableInputFluidTank,
        filter: Predicate<FluidStack>,
        level: Level,
        pos: BlockPos,
        direction: Direction?
    ) {
        val source = FluidTransferHelper.getFluidTransfer(level, pos, direction) ?: return

        var changed = false
        val fluidInventory = target.getFluidStorage()

        for (srcIndex in 0 until source.tanks) {
            val currentFluid = source.getFluidInTank(srcIndex)
            if (currentFluid.isEmpty || !filter.test(currentFluid)) {
                continue
            }

            val existing: FluidStack? = fluidInventory.get(currentFluid)
            val canInsert = Long.MAX_VALUE - (existing?.amount ?: 0)
            val toDrainAmount = canInsert.coerceAtMost(currentFluid.amount)

            if (toDrainAmount > 0) {
                val toDrain = currentFluid.copy()
                toDrain.amount = toDrainAmount
                val drained = source.drain(toDrain, false)

                if (!drained.isEmpty) {
                    existing?.let {
                        it.amount += drained.amount
                    } ?: run {
                        fluidInventory.add(drained)
                    }
                    changed = true
                }
            }
        }

        if (changed) {
            target.onContentsChanged()
        }
    }
}