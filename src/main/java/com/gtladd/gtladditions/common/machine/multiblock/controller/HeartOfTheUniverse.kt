package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues

class HeartOfTheUniverse(holder: IMachineBlockEntity, vararg args: Any?) : GTLAddWorkableElectricMultipleRecipesMachine(holder, *args) {
    override fun createRecipeLogic(vararg args: Any): RecipeLogic = HeartOfTheUniverseLogic(this)

    override fun getMaxParallel(): Int = 1

    override fun getLimitedDuration(): Int = 20

    override fun createConfigurators(): IFancyConfigurator? = null

    override fun addEnergyDisplay(textList: MutableList<Component?>) {
        getWirelessNetworkEnergyHandler()?.let { it ->
            textList.add(
                "gtceu.multiblock.max_energy_per_tick".toComponent(
                    if (it.isOnline) {
                        "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                    } else {
                        "0"
                    },
                    if (it.isOnline) {
                        NewGTValues.VNF[GTValues.MAX].literal.withStyle(ChatFormatting.OBFUSCATED)
                    } else {
                        NewGTValues.VNF[GTValues.ULV].literal
                    }
                ).withStyle(ChatFormatting.GRAY).withStyle {
                    it.withHoverEvent(
                        HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            "gtceu.multiblock.max_energy_per_tick_hover".toComponent
                                .withStyle(ChatFormatting.GRAY)
                        )
                    )
                }
            )
        }
    }

    companion object {
        class HeartOfTheUniverseLogic(parallel: HeartOfTheUniverse) : GTLAddMultipleRecipesLogic(parallel) {
            override fun getGTRecipe(): GTRecipe? {
                if (!checkBeforeWorking()) return null

                val wirelessTrait = getMachine().getWirelessNetworkEnergyHandler() ?: return null
                if (!wirelessTrait.isOnline) return null

                val parallelData = calculateParallels() ?: return null
                return buildFinalWirelessRecipe(parallelData, wirelessTrait)
            }

            override fun getWirelessRecipeEut(recipe: GTRecipe): Long = RecipeHelper.getOutputEUt(recipe)

            override fun isEnergyConsumer(): Boolean = false

            override fun checkRecipe(recipe: GTRecipe): Boolean = RecipeRunnerHelper.matchRecipe(machine, recipe) &&
                recipe.tickOutputs.isNotEmpty() &&
                recipe.tickInputs.isEmpty()

            override fun getMultipleThreads(): Int = Ints.saturatedCast(1L + getMachine().getAdditionalThread())
        }
    }
}