package com.gtladd.gtladditions.utils

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries

object ObjectHolderSlotRules {
    private val dataItems = setOf(
        ResourceLocation("gtceu", "data_stick"),
        ResourceLocation("gtceu", "data_orb"),
        ResourceLocation("gtceu", "data_module")
    )

    @JvmStatic
    fun isItemValid(slot: Int, stack: ItemStack): Boolean {
        if (slot !in 0..1) return false
        if (stack.isEmpty) return true
        val isData = ForgeRegistries.ITEMS.getKey(stack.item) in dataItems
        return if (slot == 1) isData else !isData
    }
}
