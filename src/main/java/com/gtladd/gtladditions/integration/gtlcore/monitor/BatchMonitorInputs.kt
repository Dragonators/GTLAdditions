package com.gtladd.gtladditions.integration.gtlcore.monitor

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient
import com.gtladd.gtladditions.mixin.gtlcore.integration.MachineMonitorEntriesAccessor
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.nbt.ListTag
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.integration.monitor.MachineMonitorEntries

/** Holds paid inputs by reference until a monitor first asks for this batch. */
class BatchMonitorInputs {
    private var entries: ObjectArrayList<Any>? = ObjectArrayList()
    private var recipe: GTRecipe? = null
    private var materialized: ListTag? = null

    fun addInputs(inputs: Map<RecipeCapability<*>, List<Content>>) {
        if (inputs.isNotEmpty()) entries!!.add(inputs)
    }

    fun addInput(item: ItemStack) {
        if (!item.isEmpty) entries!!.add(item)
    }

    fun addInput(fluid: FluidStack) {
        if (!fluid.isEmpty) entries!!.add(fluid)
    }

    fun bind(recipe: GTRecipe): BatchMonitorInputs {
        this.recipe = recipe
        return this
    }

    fun matches(recipe: GTRecipe): Boolean = this.recipe === recipe

    fun materializeCopy(): ListTag {
        materialized?.let { return it.copy() }

        val rows = ListTag()
        for (entry in entries!!) {
            when (entry) {
                is ItemStack -> append(rows, ItemRecipeCapability.CAP, SizedIngredient.create(entry))
                is FluidStack -> append(rows, FluidRecipeCapability.CAP, FluidIngredient.of(entry))
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    MachineMonitorEntriesAccessor.invokeAppend(
                        rows,
                        entry as Map<RecipeCapability<*>, List<Content>>,
                        false
                    )
                }
            }
        }

        val merged = MachineMonitorEntries.merge(rows)
        materialized = merged
        entries = null

        // Core narrows each monitor's Items/Fluids in place against its target's hatches.
        return merged.copy()
    }

    private fun append(rows: ListTag, capability: RecipeCapability<*>, ingredient: Any) {
        val chance = ChanceLogic.getMaxChancedValue()
        val content = Content(ingredient, chance, chance, 0, null, null)
        MachineMonitorEntriesAccessor.invokeAppend(rows, mapOf(capability to listOf(content)), false)
    }
}