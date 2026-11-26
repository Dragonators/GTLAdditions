package com.gtladd.gtladditions.api.machine.feature

import com.gtladd.gtladditions.common.machine.trait.StructureTierTrait
import net.minecraft.network.chat.Component
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.utils.Registries.getItem

interface IStructureTierMachine {
    val structureTierTrait: StructureTierTrait

    fun tierRequirement(tier: Int): TierRequirement?

    companion object {
        const val RECIPE_TIER = "structure_tier"
    }
}

/** [requirements] start at the first tier above [minTier]. */
class StructureTierSpec(val minTier: Int, val maxTier: Int, val requirements: List<TierRequirement>) {
    fun requirement(tier: Int): TierRequirement? = requirements.getOrNull(tier - minTier - 1)
}

class TierRequirement(val costs: List<TierCost>, val conditions: List<TierCondition> = emptyList())

class TierCost private constructor(val amount: Long, val tag: TagKey<Item>?, ingredient: () -> Ingredient) {
    val ingredient: Ingredient by lazy(ingredient)

    companion object {
        fun of(id: String, amount: Long) = TierCost(amount, null) { Ingredient.of(getItem(id)) }

        fun of(tag: TagKey<Item>, amount: Long) = TierCost(amount, tag) { Ingredient.of(tag) }
    }
}

interface TierCondition {
    fun test(machine: IStructureTierMachine): Boolean

    fun describe(machine: IStructureTierMachine): Component

    fun describeRequirement(): Component
}
