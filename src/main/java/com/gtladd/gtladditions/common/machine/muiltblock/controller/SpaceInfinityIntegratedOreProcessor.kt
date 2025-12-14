package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

class SpaceInfinityIntegratedOreProcessor(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricMultipleRecipesMachine(
        holder,
        *args
    ) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return InfinityIntegratedOreProcessorLogic(this)
    }

    override fun getRecipeLogic(): InfinityIntegratedOreProcessorLogic {
        return super.getRecipeLogic() as InfinityIntegratedOreProcessorLogic
    }

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        textList.add(
            Component.translatable(
                "gtceu.multiblock.parallel",
                createRainbowComponent(
                    Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                )
            ).withStyle(ChatFormatting.GRAY)
        )
        textList.add(
            Component.translatable(
                "gtladditions.multiblock.threads",
                createRainbowComponent(
                    Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                )
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    companion object {
        class InfinityIntegratedOreProcessorLogic(parallel: SpaceInfinityIntegratedOreProcessor) :
            GTLAddMultipleRecipesLogic(parallel) {
            override fun getMachine(): SpaceInfinityIntegratedOreProcessor {
                return super.getMachine() as SpaceInfinityIntegratedOreProcessor
            }

            override fun calculateParallels(): ParallelData? {
                val recipes = lookupRecipeIterator()
                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes, machine,
                    getParallelLimitForRecipe = { Long.MAX_VALUE },
                    getMaxParallelForRecipe = ::getMaxParallel
                )
            }
        }
    }
}