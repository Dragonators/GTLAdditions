package com.gtladd.gtladditions.data.recipes;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import dev.latvian.mods.kubejs.KubeJS;
import net.minecraft.resources.ResourceLocation;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;

import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;
import org.gtlcore.gtlcore.utils.Registries;

import java.util.function.Consumer;

public class Misc {

    public Misc() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("tiranium50", GTLRecipeTypes.DECAY_HASTENER_RECIPES)
                .inputFluids(GTMaterials.Titanium.getFluid(144))
                .outputFluids(GTLMaterials.Titanium50.getFluid(144))
                .TierEUtVA(13).duration(100).save(provider);
        GTLRecipeTypes.DOOR_OF_CREATE_RECIPES.recipeBuilder("command_block")
                .inputItems(Registries.getItemStack("gtceu:magnetohydrodynamicallyconstrainedstarmatter_block"))
                .outputItems(Registries.getItemStack("minecraft:command_block"))
                .dimension(new ResourceLocation("overworld"))
                .EUt(GTValues.V[14]).duration(1200).save(provider);
        GTLRecipeTypes.DOOR_OF_CREATE_RECIPES.recipeBuilder("magmatter_block")
                .inputItems(Registries.getItemStack("gtceu:magmatter_ingot", 64))
                .outputItems(Registries.getItemStack("gtceu:magmatter_block"))
                .dimension(new ResourceLocation("overworld"))
                .EUt(GTValues.V[14]).duration(1200).save(provider);
        GTLRecipeTypes.CREATE_AGGREGATION_RECIPES.recipeBuilder("chain_command_block")
                .inputItems(Registries.getItemStack("kubejs:chain_command_block_core"))
                .inputItems(Registries.getItemStack("kubejs:command_block_broken"))
                .outputItems(Registries.getItemStack("minecraft:chain_command_block"))
                .dimension(KubeJS.id("create")).CWUt(Integer.MAX_VALUE - 1)
                .EUt(GTValues.V[14]).duration(1200).save(provider);
        GTLRecipeTypes.CREATE_AGGREGATION_RECIPES.recipeBuilder("repeating_command_block")
                .inputItems(Registries.getItemStack("kubejs:repeating_command_block_core"))
                .inputItems(Registries.getItemStack("kubejs:chain_command_block_broken"))
                .outputItems(Registries.getItemStack("minecraft:repeating_command_block"))
                .dimension(KubeJS.id("create")).CWUt(Integer.MAX_VALUE - 1)
                .EUt(GTValues.V[14]).duration(1200).save(provider);
        GTRecipeTypes.EVAPORATION_RECIPES.recipeBuilder("liquid_air")
                .inputFluids(GTMaterials.LiquidAir.getFluid(100000000))
                .chancedOutput(TagPrefix.dust, GTMaterials.Ice, 2048, 9000, 0)
                .outputFluids(GTMaterials.Nitrogen.getFluid(75000000)).outputFluids(GTMaterials.Oxygen.getFluid(2000000))
                .outputFluids(GTMaterials.CarbonDioxide.getFluid(5000000)).outputFluids(GTMaterials.Helium.getFluid(2000000))
                .outputFluids(GTMaterials.Argon.getFluid(100000))
                .EUt(GTValues.V[6]).duration(2560).save(provider);
        GTRecipeTypes.EVAPORATION_RECIPES.recipeBuilder("liquid_nether_air")
                .inputFluids(GTMaterials.LiquidNetherAir.getFluid(200000000))
                .chancedOutput(TagPrefix.dust, GTMaterials.Ash, 2048, 2250, 0)
                .outputFluids(GTMaterials.CarbonMonoxide.getFluid(144000000)).outputFluids(GTMaterials.CoalGas.getFluid(20000000))
                .outputFluids(GTMaterials.HydrogenSulfide.getFluid(15000000)).outputFluids(GTMaterials.SulfurDioxide.getFluid(15000000))
                .outputFluids(GTMaterials.Helium3.getFluid(5000000)).outputFluids(GTMaterials.Neon.getFluid(1000000))
                .EUt(GTValues.V[6]).duration(2560).save(provider);
        GTRecipeTypes.EVAPORATION_RECIPES.recipeBuilder("liquid_ender_air")
                .inputFluids(GTMaterials.LiquidEnderAir.getFluid(400000000))
                .chancedOutput(TagPrefix.dust, GTMaterials.EnderPearl, 2048, 1000, 0)
                .outputFluids(GTMaterials.NitrogenDioxide.getFluid(250000000)).outputFluids(GTMaterials.Deuterium.getFluid(100000000))
                .outputFluids(GTMaterials.Helium.getFluid(30000000)).outputFluids(GTMaterials.Tritium.getFluid(20000000))
                .outputFluids(GTMaterials.Krypton.getFluid(2000000)).outputFluids(GTMaterials.Xenon.getFluid(2000000))
                .outputFluids(GTMaterials.Radon.getFluid(2000000))
                .EUt(GTValues.V[6]).duration(2560).save(provider);
    }
}
