package com.gtladd.gtladditions.data.Recipes.NewMachineRecipe;

import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.Recipe.GTLAddRecipesTypes;
import com.gtladd.gtladditions.api.Registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;
import java.util.stream.IntStream;

public class VoidfluxReaction {

    private static final String[] DIMENSIONS = { "overworld", "nether", "end" };
    private static final int BASE_TIER = 8;

    public VoidfluxReaction() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        for (String dimension : DIMENSIONS) {
            for (int tier = GTValues.UHV; tier < GTValues.MAX; tier++) {
                final int amplifier = (int) Math.pow(2, tier - BASE_TIER);
                final String voltageName = GTValues.VN[tier].toLowerCase();
                GTLAddRecipeBuilder builder = new GTLAddRecipeBuilder(
                        String.format("%s_air_collector_%d", dimension, tier - BASE_TIER),
                        GTLAddRecipesTypes.VOIDFLUX_REACTION).notConsumable("kubejs:" + dimension + "_data", 64)
                        .notConsumable("gtceu:" + voltageName + "_fluid_regulator");
                final int[] durations = { 20, 200 };
                int finalTier = tier - BASE_TIER;
                IntStream.range(0, 2).forEach(mode -> {
                    GTRecipeBuilder modeBuilder = ((mode == 0) ?
                            builder.copy(String.format("%s_%s_air_collector_%d", voltageName, dimension, mode)).circuitMeta(1) :
                            builder.copy(String.format("%s_%s_air_collector_%d", voltageName, dimension, mode)).notConsumable(MultiBlockMachineA.COOLING_TOWER.asStack()));
                    setAir(modeBuilder, dimension, mode, amplifier);
                    modeBuilder.duration(durations[mode])
                            .EUt(GTValues.VA[finalTier])
                            .save(provider);
                });
            }
        }
    }

    private static void setAir(GTRecipeBuilder builder, String s, int i, int j) {
        switch (s) {
            case "overworld" -> {
                if (i == 0) builder.outputFluids(GTMaterials.Air.getFluid(100000L * j));
                else builder.outputFluids(GTMaterials.LiquidAir.getFluid(100000L * j));
            }
            case "nether" -> {
                if (i == 0) builder.outputFluids(GTMaterials.NetherAir.getFluid(100000L * j));
                else builder.outputFluids(GTMaterials.LiquidNetherAir.getFluid(100000L * j));
            }
            case "end" -> {
                if (i == 0) builder.outputFluids(GTMaterials.EnderAir.getFluid(100000L * j));
                else builder.outputFluids(GTMaterials.LiquidEnderAir.getFluid(100000L * j));
            }
        }
    }
}
