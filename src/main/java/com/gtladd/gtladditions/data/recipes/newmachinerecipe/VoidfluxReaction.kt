package com.gtladd.gtladditions.data.recipes.newmachinerecipe

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import net.minecraft.data.recipes.FinishedRecipe
import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA
import java.util.*
import java.util.function.Consumer
import java.util.stream.IntStream
import kotlin.math.pow

object VoidfluxReaction {
    @JvmStatic
    fun init(provider : Consumer<FinishedRecipe?>) {
        for (dimension in arrayOf("overworld", "nether", "end")) {
            for (tier in GTValues.UEV ..< GTValues.OpV) {
                val voltageName = GTValues.VN[tier].lowercase(Locale.getDefault())
                val builder = GTLAddRecipeBuilder(
                    String.format("%s_air_collector_%d", dimension, tier - 8),
                    GTLAddRecipesTypes.VOIDFLUX_REACTION
                )
                    .notConsumable("kubejs:" + dimension + "_data", 64)
                    .notConsumable("gtceu:" + voltageName + "_fluid_regulator")
                val durations = intArrayOf(20, 200)
                val finalTier = tier - 3
                IntStream.range(0, 2).forEach { mode : Int ->
                    val modeBuilder = (if (mode == 0) builder.copy(
                        GTLAdditions.id(
                            String.format(
                                "%s_%s_air_collector_%d",
                                voltageName,
                                dimension,
                                mode
                            )
                        )
                    ).circuitMeta(1) else builder.copy(
                        GTLAdditions.id(
                            String.format("%s_%s_air_collector_%d", voltageName, dimension, mode)
                        )
                    ).notConsumable(MultiBlockMachineA.COOLING_TOWER.asStack()))
                    setAir(modeBuilder, dimension, mode, 4.0.pow((finalTier - 5).toDouble()).toInt())
                    modeBuilder.duration(durations[mode]).EUt(GTValues.VA[finalTier].toLong()).save(provider)
                }
            }
        }
    }

    private fun setAir(builder : GTRecipeBuilder, s : String, i : Int, j : Int) {
        when (s) {
            "overworld" -> {
                if (i == 0) builder.outputFluids(GTMaterials.Air.getFluid(10000L * j))
                else builder.outputFluids(GTMaterials.LiquidAir.getFluid(10000L * j))
            }
            "nether" -> {
                if (i == 0) builder.outputFluids(GTMaterials.NetherAir.getFluid(10000L * j))
                else builder.outputFluids(GTMaterials.LiquidNetherAir.getFluid(10000L * j))
            }
            "end" -> {
                if (i == 0) builder.outputFluids(GTMaterials.EnderAir.getFluid(10000L * j))
                else builder.outputFluids(GTMaterials.LiquidEnderAir.getFluid(10000L * j))
            }
        }
    }
}
