package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.utils.MachineIO
import java.util.function.Predicate

class SpaceInfinityIntegratedOreProcessor(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricMultipleRecipesMachine(
        holder,
        *args
    ) {

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return InfinityIntegratedOreProcessorLogic(this, BEFORE_RECIPE)
    }

    override fun getRecipeLogic(): InfinityIntegratedOreProcessorLogic {
        return super.getRecipeLogic() as InfinityIntegratedOreProcessorLogic
    }

    override fun needConfirmMEStock(): Boolean = true

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
        class InfinityIntegratedOreProcessorLogic(
            parallel: SpaceInfinityIntegratedOreProcessor,
            beforeWorking: Predicate<IRecipeLogicMachine>?
        ) :
            GTLAddMultipleRecipesLogic(parallel, beforeWorking) {
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

        private val BEFORE_RECIPE = Predicate { machine: IRecipeLogicMachine ->
            if (machine is SpaceInfinityIntegratedOreProcessor) return@Predicate MachineIO.inputFluid(
                machine,
                GTLMaterials.StellarEnergyRocketFuel.getFluid(100000)
            )
            false
        }
    }
}