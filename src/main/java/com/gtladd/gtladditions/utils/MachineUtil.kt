package com.gtladd.gtladditions.utils

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.handleRecipeInputNocache
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipeInputNocache
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient

object MachineUtil {
    fun inputFluid(machine: WorkableMultiblockMachine, stack: FluidStack): Boolean =
        input(machine).fluid(stack).execute()

    fun inputItem(machine: WorkableMultiblockMachine, stack: ItemStack, amount: Long): Boolean =
        input(machine).item(stack, amount).execute()

    fun inputItem(machine: WorkableMultiblockMachine, ingredient: LongIngredient, stack: ItemStack): Boolean =
        input(machine).item(ingredient).execute()

    fun input(machine: WorkableMultiblockMachine): InputBuilder = InputBuilder(machine)

    class InputBuilder internal constructor(private val machine: WorkableMultiblockMachine) {
        private val builder = GTRecipeBuilder.ofRaw()
        private var hasInput = false

        fun fluid(stack: FluidStack): InputBuilder {
            if (!stack.isEmpty) {
                builder.inputFluids(FluidIngredient.of(stack))
                hasInput = true
            }
            return this
        }

        fun item(stack: ItemStack): InputBuilder {
            if (!stack.isEmpty) {
                builder.inputItems(stack)
                hasInput = true
            }
            return this
        }

        fun item(stack: ItemStack, amount: Long): InputBuilder =
            item(LongIngredient.create(Ingredient.of(stack), amount))

        fun item(ingredient: LongIngredient): InputBuilder {
            if (!ingredient.isEmpty) {
                builder.inputItems(ingredient)
                hasInput = true
            }
            return this
        }

        fun execute(): Boolean {
            if (!hasInput) return true
            val recipe = builder.buildRawRecipe()
            if (matchRecipeInputNocache(machine, recipe)) {
                return handleRecipeInputNocache(machine, recipe)
            }
            return false
        }
    }

    fun outputItem(machine: WorkableMultiblockMachine, ingredient: LongIngredient): Boolean {
        if (ingredient.isEmpty) return true

        val recipe = GTRecipeBuilder.ofRaw().outputItems(ingredient).buildRawRecipe()

        return RecipeRunnerHelper.matchRecipeOutput(machine, recipe) &&
            RecipeRunnerHelper.handleRecipeOutput(machine, recipe)
    }
}