package com.gtladd.gtladditions.api.machine.feature

import com.gtladd.gtladditions.common.machine.trait.StructureTierTrait
import net.minecraft.network.chat.Component
import net.minecraft.world.item.crafting.Ingredient

interface IStructureTierMachine {
    val structureTierTrait: StructureTierTrait

    fun tierRequirement(tier: Int): TierRequirement?

    companion object {
        const val RECIPE_TIER = "structure_tier"
    }
}

class TierRequirement(val costs: List<TierCost>, val conditions: List<TierCondition> = emptyList())

class TierCost(ingredient: () -> Ingredient, val amount: Long) {
    val ingredient: Ingredient by lazy(ingredient)
}

interface TierCondition {
    fun test(machine: IStructureTierMachine): Boolean

    fun describe(machine: IStructureTierMachine): Component
}