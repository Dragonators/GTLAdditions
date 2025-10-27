package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.Content
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gtladd.gtladditions.api.machine.data.ParallelData
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import org.gtlcore.gtlcore.api.recipe.IGTRecipe
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues
import java.math.BigInteger

class HeartOfTheUniverse(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder, *args) {
    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return object : GTLAddMultipleRecipesLogic(this) {
            override fun getGTRecipe(): GTRecipe? {
                if (!checkBeforeWorking()) return null

                val wirelessTrait = getMachine().wirelessNetworkEnergyHandler
                if (wirelessTrait == null || !wirelessTrait.isOnline()) return null

                val parallelData = calculateParallels()
                if (parallelData == null) return null

                return buildFinalWirelessRecipe(parallelData, wirelessTrait)
            }

            override fun buildFinalWirelessRecipe(
                parallelData: ParallelData,
                wirelessTrait: IWirelessNetworkEnergyHandler
            ): WirelessGTRecipe? {
                val itemOutputs = ObjectArrayList<Content?>()
                val fluidOutputs = ObjectArrayList<Content?>()

                var totalEu = BigInteger.ZERO
                var index = 0

                for (r in parallelData.recipeList) {
                    var r = r
                    var parallelTotalEu = BigInteger.valueOf(RecipeHelper.getOutputEUt(r))
                        .multiply(BigInteger.valueOf(r.duration.toLong()))

                    val p = parallelData.parallels[index++]
                    if (p > 1) {
                        r = r.copy(ContentModifier.multiplier(p.toDouble()), false)
                        parallelTotalEu = parallelTotalEu.multiply(BigInteger.valueOf(p))
                    }
                    IGTRecipe.of(r).realParallels = p

                    r = modifyInputAndOutput(r)
                    if (RecipeRunnerHelper.handleRecipeInput(machine, r)) {
                        totalEu = totalEu.add(parallelTotalEu)
                        r.outputs[ItemRecipeCapability.CAP]?.let { itemOutputs.addAll(it) }
                        r.outputs[FluidRecipeCapability.CAP]?.let { fluidOutputs.addAll(it) }
                    }
                }

                if (itemOutputs.isEmpty() && fluidOutputs.isEmpty() && totalEu.signum() == 0) {
                    if (recipeStatus == null || recipeStatus.isSuccess) RecipeResult.of(
                        this.machine,
                        RecipeResult.FAIL_FIND
                    )
                    return null
                }

                val eut = totalEu.divide(BigInteger.valueOf(20))
                return buildWirelessRecipe(itemOutputs, fluidOutputs, 20, eut)
            }

            override fun checkRecipe(recipe: GTRecipe): Boolean {
                return RecipeRunnerHelper.matchRecipe(machine, recipe) &&
                        recipe.tickOutputs.isNotEmpty() &&
                        recipe.tickInputs.isEmpty()
            }

            override fun getMultipleThreads(): Int {
                return Ints.saturatedCast(1L + getMachine().additionalThread)
            }
        }
    }

    override fun getMaxParallel(): Int {
        return 1
    }

    override fun createConfigurators() : IFancyConfigurator? {
        return null
    }

    override fun addEnergyDisplay(textList: MutableList<Component?>) {

        wirelessNetworkEnergyHandler?.let { it ->
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
}
