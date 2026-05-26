package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

class SpaceScalingInstrument(holder: IMachineBlockEntity) : GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine(holder) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = SpaceScalingInstrumentLogic(this)

    override fun getRecipeLogic(): SpaceScalingInstrumentLogic = super.getRecipeLogic() as SpaceScalingInstrumentLogic

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        textList.add(
            "gtceu.multiblock.parallel".toComponent(
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).withStyle(ChatFormatting.GRAY)
        )
        textList.add(
            "gtladditions.multiblock.threads".toComponent(
                createLanguageRainbowComponentOnServer(
                    "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                )
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    companion object {
        class SpaceScalingInstrumentLogic(parallel: SpaceScalingInstrument) : GTLAddMultipleRecipesLogic(parallel) {
            override fun getMachine(): SpaceScalingInstrument = super.getMachine() as SpaceScalingInstrument

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()

                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes = recipes,
                    machine = machine,
                    getParallelLimitForRecipe = { Long.MAX_VALUE },
                    getMaxParallelForRecipe = ::getMaxParallel
                )
            }
        }
    }
}