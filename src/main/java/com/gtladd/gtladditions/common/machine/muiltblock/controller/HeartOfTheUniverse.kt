package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues
import java.math.BigInteger

class HeartOfTheUniverse(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder, *args) {
    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return HeartOfTheUniverseLogic(this)
    }

    override fun getMaxParallel(): Int {
        return 1
    }

    override fun createConfigurators(): IFancyConfigurator? {
        return null
    }

    override fun addEnergyDisplay(textList: MutableList<Component?>) {

        getWirelessNetworkEnergyHandler()?.let { it ->
            textList.add(
                Component.translatable(
                    "gtceu.multiblock.max_energy_per_tick",
                    if (it.isOnline) Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel") else "0",
                    if (it.isOnline) Component.literal(NewGTValues.VNF[GTValues.MAX])
                        .withStyle(ChatFormatting.OBFUSCATED) else Component.literal(NewGTValues.VNF[GTValues.ULV])
                )
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle {
                        it.withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("gtceu.multiblock.max_energy_per_tick_hover")
                                    .withStyle(ChatFormatting.GRAY)
                            )
                        )
                    })
        }
    }

    companion object {
        class HeartOfTheUniverseLogic(parallel: HeartOfTheUniverse) :
            GTLAddMultipleRecipesLogic(parallel) {
            override fun getGTRecipe(): GTRecipe? {
                if (!checkBeforeWorking()) return null

                val wirelessTrait = getMachine().getWirelessNetworkEnergyHandler()
                if (wirelessTrait == null || !wirelessTrait.isOnline) return null

                val parallelData = calculateParallels()
                if (parallelData == null) return null

                return buildFinalWirelessRecipe(parallelData, wirelessTrait)
            }

            override fun buildFinalWirelessRecipe(
                parallelData: ParallelData,
                wirelessTrait: IWirelessNetworkEnergyHandler
            ): WirelessGTRecipe? {
                val itemOutputs = ObjectArrayList<Content>()
                val fluidOutputs = ObjectArrayList<Content>()
                var totalEu = BigInteger.ZERO
                var index = 0

                for (r in parallelData.recipeList) {
                    val p = parallelData.parallels[index++]
                    var paralleledRecipe = RecipeCalculationHelper.multipleRecipe(r, p)

                    var parallelTotalEu = BigInteger.valueOf(RecipeHelper.getOutputEUt(r))
                        .multiply(BigInteger.valueOf(r.duration.toLong()))
                    if (p > 1) parallelTotalEu = parallelTotalEu.multiply(BigInteger.valueOf(p))

                    paralleledRecipe = IParallelLogic.getRecipeOutputChance(machine, paralleledRecipe)
                    if (RecipeRunnerHelper.handleRecipeInput(machine, paralleledRecipe)) {
                        totalEu = totalEu.add(parallelTotalEu)
                        RecipeCalculationHelper.collectOutputs(paralleledRecipe, itemOutputs, fluidOutputs)
                    }
                }

                if (!RecipeCalculationHelper.hasOutputs(itemOutputs, fluidOutputs) && totalEu.signum() == 0) {
                    if (recipeStatus == null || recipeStatus.isSuccess) {
                        RecipeResult.of(this.machine, RecipeResult.FAIL_FIND)
                    }
                    return null
                }

                return RecipeCalculationHelper.buildWirelessRecipe(itemOutputs, fluidOutputs, 20, totalEu.negate())
            }

            override fun checkRecipe(recipe: GTRecipe): Boolean {
                return RecipeRunnerHelper.matchRecipe(machine, recipe) &&
                        recipe.tickOutputs.isNotEmpty() &&
                        recipe.tickInputs.isEmpty()
            }

            override fun getMultipleThreads(): Int {
                return Ints.saturatedCast(1L + getMachine().getAdditionalThread())
            }
        }
    }
}
