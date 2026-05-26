package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.GTValues.VA
import com.gregtechceu.gtceu.common.data.GTMaterials.*
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions.Companion.id
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes.VOIDFLUX_REACTION
import dev.latvian.mods.kubejs.KubeJS
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.GTLItems
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.*
import java.util.function.Consumer
import java.util.stream.IntStream
import kotlin.math.pow

object VoidfluxReaction {
    fun init(provider: Consumer<FinishedRecipe?>) {
        for (dimension in arrayOf("overworld", "nether", "end")) {
            for (tier in GTValues.UEV..<GTValues.MAX) {
                val voltageName = GTValues.VN[tier].lowercase(Locale.getDefault())
                val builder = VOIDFLUX_REACTION.recipeBuilder(
                    id(String.format("%s_air_collector_%d", dimension, tier - 8))
                ).notConsumable(getItemStack("kubejs:" + dimension + "_data", 64))
                    .notConsumable(getItemStack("gtceu:" + voltageName + "_fluid_regulator"))
                val durations = intArrayOf(20, 200)
                val finalTier = tier - 3
                IntStream.range(0, 2).forEach { mode: Int ->
                    val modeBuilder = (
                        if (mode == 0) {
                            builder.copy(
                                id(String.format("%s_%s_air_collector_%d", voltageName, dimension, mode))
                            ).circuitMeta(1)
                        } else {
                            builder.copy(
                                id(String.format("%s_%s_air_collector_%d", voltageName, dimension, mode))
                            ).notConsumable(MultiBlockMachineA.COOLING_TOWER.asStack())
                        }
                        )
                    setAir(modeBuilder, dimension, mode, 4.0.pow((finalTier - 5).toDouble()).toInt())
                    modeBuilder.duration(durations[mode]).EUt(VA[finalTier].toLong()).save(provider)
                }
            }
            val voltageName = GTValues.VN[14].lowercase(Locale.getDefault())
            val builder = VOIDFLUX_REACTION.recipeBuilder(
                id(String.format("%s_air_collector_%d", dimension, 6))
            )
                .notConsumable(getItemStack("kubejs:" + dimension + "_data", 64))
                .notConsumable(GTLItems.FLUID_REGULATOR_MAX)
            val durations = intArrayOf(20, 200)
            IntStream.range(0, 2).forEach { mode: Int ->
                val modeBuilder = (
                    if (mode == 0) {
                        builder.copy(
                            id(String.format("%s_%s_air_collector_%d", voltageName, dimension, mode))
                        ).circuitMeta(1)
                    } else {
                        builder.copy(
                            id(String.format("%s_%s_air_collector_%d", voltageName, dimension, mode))
                        ).notConsumable(MultiBlockMachineA.COOLING_TOWER.asStack())
                    }
                    )
                setAir(modeBuilder, dimension, mode, 5.0.pow(6).toInt())
                modeBuilder.duration(durations[mode]).EUt(VA[11].toLong()).save(provider)
            }
        }
        for (tier in 10..<14) {
            val voltageName = GTValues.VN[tier].lowercase(Locale.getDefault())
            VOIDFLUX_REACTION.recipeBuilder(id(String.format("barnarda_air_%d", tier - 9)))
                .notConsumable(GTLAddItems.BARNARDA_DATA.asStack(64))
                .notConsumable(getItemStack("gtceu:" + voltageName + "_fluid_regulator", 8))
                .outputFluids(GTLMaterials.BarnardaAir.getFluid(200000L * 4.0.pow(tier - 9).toInt()))
                .EUt(VA[tier].toLong()).duration(100 * (15 - tier))
                .dimension(KubeJS.id("barnarda"))
                .save(provider)
        }
        VOIDFLUX_REACTION.recipeBuilder(id("barnarda_air_5"))
            .notConsumable(GTLAddItems.BARNARDA_DATA.asStack(64))
            .notConsumable(GTLItems.FLUID_REGULATOR_MAX.asStack(8))
            .outputFluids(GTLMaterials.BarnardaAir.getFluid(150000000))
            .EUt(VA[14].toLong()).duration(100)
            .dimension(KubeJS.id("barnarda"))
            .save(provider)
    }

    private fun setAir(builder: GTRecipeBuilder, s: String, i: Int, j: Int) {
        when (s) {
            "overworld" -> {
                if (i == 0) {
                    builder.outputFluids(Air.getFluid(8000L * j))
                } else {
                    builder.outputFluids(LiquidAir.getFluid(20000L * j))
                }
            }
            "nether" -> {
                if (i == 0) {
                    builder.outputFluids(NetherAir.getFluid(8000L * j))
                } else {
                    builder.outputFluids(LiquidNetherAir.getFluid(20000L * j))
                }
            }
            "end" -> {
                if (i == 0) {
                    builder.outputFluids(EnderAir.getFluid(8000L * j))
                } else {
                    builder.outputFluids(LiquidEnderAir.getFluid(20000L * j))
                }
            }
        }
    }
}