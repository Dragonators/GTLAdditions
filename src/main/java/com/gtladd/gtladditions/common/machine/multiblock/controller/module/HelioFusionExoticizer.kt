package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.GTRecipeType
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

class HelioFusionExoticizer(holder: IMachineBlockEntity, vararg args: Any?) :
    ForgeOfTheAntichristModuleBase(
        holder,
        *args
    ) {

    override fun createRecipeLogic(vararg args: Any): HelioFusionExoticizerLogic = HelioFusionExoticizerLogic(this)

    override fun getRecipeLogic(): HelioFusionExoticizerLogic = recipeLogic as HelioFusionExoticizerLogic

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        textList.add(
            "gtceu.multiblock.parallel".toComponent(
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    companion object {
        class HelioFusionExoticizerLogic(
            parallel: HelioFusionExoticizer
        ) : ForgeOfTheAntichristModuleBase.Companion.ForgeOfTheAntichristModuleBaseLogic(parallel, BEFORE_WORKING) {
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
                            ),
                            longArrayOf(parallel)
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

            override fun enableModify(recipeType: GTRecipeType): Boolean = true
        }
    }
}