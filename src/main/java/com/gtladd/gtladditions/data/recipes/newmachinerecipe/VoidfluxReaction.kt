package com.gtladd.gtladditions.data.recipes.newmachinerecipe;

import org.gtlcore.gtlcore.common.data.machines.MultiBlockMachineA;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;

import java.util.function.Consumer;
import java.util.stream.IntStream;

public class VoidfluxReaction {

    public VoidfluxReaction() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        for (String dimension : new String[] { "overworld", "nether", "end" }) {
            for (int tier = GTValues.UEV; tier < GTValues.OpV; tier++) {
                final String voltageName = GTValues.VN[tier].toLowerCase();
                GTLAddRecipeBuilder builder = new GTLAddRecipeBuilder(String.format("%s_air_collector_%d", dimension, tier - 8), GTLAddRecipesTypes.VOIDFLUX_REACTION)
                        .notConsumable("kubejs:" + dimension + "_data", 64).notConsumable("gtceu:" + voltageName + "_fluid_regulator");
                final int[] durations = { 20, 200 };
                int finalTier = tier - 3;
                IntStream.range(0, 2).forEach(mode -> {
                    GTRecipeBuilder modeBuilder = ((mode == 0) ?
                            builder.copy(GTLAdditions.id(String.format("%s_%s_air_collector_%d", voltageName, dimension, mode))).circuitMeta(1) :
                            builder.copy(GTLAdditions.id(String.format("%s_%s_air_collector_%d", voltageName, dimension, mode))).notConsumable(MultiBlockMachineA.COOLING_TOWER.asStack()));
                    setAir(modeBuilder, dimension, mode, (int) Math.pow(4, finalTier - 5));
                    modeBuilder.duration(durations[mode]).EUt(GTValues.VA[finalTier]).save(provider);
                });
            }
        }
    }

    private static void setAir(GTRecipeBuilder builder, String s, int i, int j) {
        switch (s) {
            case "overworld" -> {
                if (i == 0) builder.outputFluids(GTMaterials.Air.getFluid(10000L * j));
                else builder.outputFluids(GTMaterials.LiquidAir.getFluid(10000L * j));
            }
            case "nether" -> {
                if (i == 0) builder.outputFluids(GTMaterials.NetherAir.getFluid(10000L * j));
                else builder.outputFluids(GTMaterials.LiquidNetherAir.getFluid(10000L * j));
            }
            case "end" -> {
                if (i == 0) builder.outputFluids(GTMaterials.EnderAir.getFluid(10000L * j));
                else builder.outputFluids(GTMaterials.LiquidEnderAir.getFluid(10000L * j));
            }
        }
    }
}
