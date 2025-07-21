package com.gtladd.gtladditions.common.machine.muiltblock

import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import java.util.function.BiConsumer
import kotlin.math.min
import kotlin.math.pow

object GTLAddMultiBlockMachineModifier {
    @JvmField
    val DRACONIC_COLLAPSE_CORE_MODIFIER: Array<RecipeModifier?> = arrayOf(
        RecipeModifier { machine: MetaMachine?, recipe: GTRecipe?, params: OCParams?, result: OCResult? ->
            GTRecipeModifiers.accurateParallel(
                machine, recipe!!,
                8.0.pow(((machine as WorkableElectricMultiblockMachine).getTier() - 10).toDouble()).toInt(),
                false
            ).getFirst()
        },
        GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.PERFECT_OVERCLOCK)
    )
    @JvmField
    val MULTIPLERECIPES_COIL_PARALLEL: BiConsumer<IMultiController?, MutableList<Component?>?> =
        BiConsumer { controller: IMultiController?, components: MutableList<Component?>? ->
            if (controller is GTLAddCoilWorkableElectricMultipleRecipesMultiblockMachine) {
                if (controller.isFormed()) {
                    components!!.add(
                        Component.translatable(
                            "gtceu.multiblock.parallel",
                            Component.translatable(
                                FormattingUtil.formatNumbers(
                                    min(
                                        Int.Companion.MAX_VALUE,
                                        2.0.pow(controller.coilType.coilTemperature.toDouble() / 900.0)
                                            .toInt()))).withStyle(ChatFormatting.DARK_PURPLE))
                            .withStyle(ChatFormatting.GRAY)
                    )
                }
            }
        }
    @JvmField
    val INT_MAX_PARALLEL: BiConsumer<IMultiController?, MutableList<Component?>?> =
        BiConsumer { controller: IMultiController?, components: MutableList<Component?>? ->
            if (controller!!.isFormed) {
                components!!.add(
                    Component.translatable(
                        "gtceu.multiblock.parallel", Component.literal("2147483647")
                            .withStyle(ChatFormatting.DARK_PURPLE))
                        .withStyle(ChatFormatting.GRAY)
                )
            }
        }
    @JvmField
    val DRACONIC_COLLAPSE_CORE_ADDTEXT: BiConsumer<IMultiController?, MutableList<Component?>?> =
        BiConsumer { controller: IMultiController?, components: MutableList<Component?>? ->
            if (controller!!.isFormed) {
                components!!.add(
                    Component
                        .translatable(
                            "gtceu.multiblock.parallel",
                            Component.translatable(
                                FormattingUtil.formatNumbers(
                                    8.0.pow(((controller as WorkableElectricMultiblockMachine).getTier() - 10).toDouble())))
                                .withStyle(ChatFormatting.DARK_PURPLE)
                        )
                        .withStyle(ChatFormatting.GRAY)
                )
            }
        }
}
