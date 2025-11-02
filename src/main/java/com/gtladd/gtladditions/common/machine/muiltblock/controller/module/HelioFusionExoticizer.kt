package com.gtladd.gtladditions.common.machine.muiltblock.controller.module

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.api.machine.data.ParallelData
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

class HelioFusionExoticizer(holder: IMachineBlockEntity, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(
        holder,
        *args
    ) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = HelioFusionExoticizerLogic(this)

    override fun getRecipeLogic(): HelioFusionExoticizerLogic = super.getRecipeLogic() as HelioFusionExoticizerLogic

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        textList.add(
            Component.translatable(
                "gtceu.multiblock.parallel",
                GTLAddMachines.createRainbowComponent(
                    Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                )
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    companion object {
        class HelioFusionExoticizerLogic(
            parallel: HelioFusionExoticizer
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(parallel) {
            init {
                this.setReduction(0.5, 1.0)
            }

            override fun getMachine(): HelioFusionExoticizer = machine as HelioFusionExoticizer

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                if (recipes.isEmpty()) return null

                recipes.first().let { recipe ->
                    val parallel = getMaxParallel(recipe, Long.MAX_VALUE)
                    if (parallel > 0) {
                        return ParallelData(
                            listOf(
                                copyAndModifyRecipe(
                                    recipe,
                                    ContentModifier.multiplier(getMachine().host!!.recipeOutputMultiply)
                                )
                            ), longArrayOf(parallel)
                        )
                    }
                }

                return null
            }

            override fun lookupRecipeIterator(): Set<GTRecipe> {
                lockRecipe?.let {
                    return if (checkRecipe(it)) setOf(it) else setOf()
                }

                return machine.recipeType.lookup
                    .find(machine) { recipe: GTRecipe -> checkRecipe(recipe) }
                    ?.also {
                        isLock = true
                        lockRecipe = it
                    }
                    ?.let { setOf(it) } ?: setOf()
            }
        }
    }
}