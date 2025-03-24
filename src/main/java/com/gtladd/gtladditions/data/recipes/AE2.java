package com.gtladd.gtladditions.data.recipes;

import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;

import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

public class AE2 {

    public AE2() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder("cell_component_1k", GTRecipeTypes.FORMING_PRESS_RECIPES)
                .inputItems("gtceu:certus_quartz_plate").inputItems("ae2:logic_processor").inputItems("gtceu:redstone_plate")
                .outputItems("ae2:cell_component_1k")
                .EUt(1).duration(100).save(provider);
        new GTLAddRecipeBuilder("singularity_1", GTLRecipeTypes.MATTER_FABRICATOR_RECIPES)
                .inputItems("kubejs:scrap", 4320).circuitMeta(3)
                .outputItems("ae2:singularity")
                .TierEUtVA(9).duration(1).save(provider);
        new GTLAddRecipeBuilder("singularity_2", GTLRecipeTypes.MATTER_FABRICATOR_RECIPES)
                .inputItems("kubejs:scrap_box", 480).circuitMeta(3)
                .outputItems("ae2:singularity", 9)
                .TierEUtVA(10).duration(1).save(provider);
        new GTLAddRecipeBuilder("quartz_glassquartz_glass", GTRecipeTypes.ALLOY_SMELTER_RECIPES)
                .inputItemsTag("glass").inputItems("gtceu:certus_quartz_dust")
                .outputItems("ae2:quartz_glass")
                .EUt(7).duration(150).save(provider);
        new GTLAddRecipeBuilder("energy_cell", GTRecipeTypes.ASSEMBLER_RECIPES)
                .inputItemsTag("gems/certus_quartz", 4)
                .InputItems("4x ae2:fluix_dust").inputItems("ae2:quartz_glass")
                .outputItems("ae2:energy_cell")
                .EUt(32).duration(10).save(provider);
        new GTLAddRecipeBuilder("dense_energy_cell", GTRecipeTypes.ASSEMBLER_RECIPES)
                .InputItems("8x ae2:energy_cell").inputItems("ae2:calculation_processor")
                .outputItems("ae2:dense_energy_cell")
                .EUt(32).duration(10).save(provider);
    }
}
