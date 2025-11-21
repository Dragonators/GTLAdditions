package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops
import com.gtladd.gtladditions.common.items.GTLAddItems
import net.minecraft.world.item.ItemStack
import kotlin.math.min

interface IAstralArrayInteractionMachine : IMachineModifyDrops {
    fun increaseAstralArrayCount(amount: Int): Int

    val astralArrayCount: Int

    override fun onDrops(list: MutableList<ItemStack>) {
        val count = this.astralArrayCount
        if (count > 0) {
            var i = 0
            while (i < count) {
                val stackSize = min(64, count - i)
                val stack = ItemStack(GTLAddItems.ASTRAL_ARRAY.asItem(), stackSize)
                list.add(stack)
                i += 64
            }
        }
    }
}
