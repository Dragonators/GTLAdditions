package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops
import com.gtladd.gtladditions.common.items.GTLAddItems
import net.minecraft.world.item.ItemStack
import kotlin.math.min

interface IAstralArrayInteractionMachine : IMachineModifyDrops {
    fun increaseAstralArrayCount(amount: Int): Int

    val astralArrayCount: Int

    override fun onDrops(list: MutableList<ItemStack>) {
        var count = this.astralArrayCount
        while (count > 0) {
            val stackSize = min(64, count)
            list.add(ItemStack(GTLAddItems.ASTRAL_ARRAY.asItem(), stackSize))
            count -= stackSize
        }
    }

    companion object {
        const val ASTRAL_ARRAY_EQUIVALENT = 1
        const val COMPRESSED_ASTRAL_ARRAY_EQUIVALENT = 1024

        fun getAstralArrayEquivalent(stack: ItemStack): Int = when {
            stack.`is`(GTLAddItems.COMPRESSED_ASTRAL_ARRAY.asItem()) -> COMPRESSED_ASTRAL_ARRAY_EQUIVALENT
            stack.`is`(GTLAddItems.ASTRAL_ARRAY.asItem()) -> ASTRAL_ARRAY_EQUIVALENT
            else -> 0
        }
    }
}