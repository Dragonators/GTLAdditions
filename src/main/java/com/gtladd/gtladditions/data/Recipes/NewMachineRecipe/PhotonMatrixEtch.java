package com.gtladd.gtladditions.data.Recipes.NewMachineRecipe;

import org.gtlcore.gtlcore.common.data.GTLMaterials;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.Recipe.GTLAddRecipesTypes;
import com.gtladd.gtladditions.api.Registry.GTLAddRecipeBuilder;

import java.util.function.Consumer;

public class PhotonMatrixEtch {

    public PhotonMatrixEtch() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        addRecipe("raw_photon_carrying_wafer", "kubejs:rutherfordium_neutronium_wafer", "kubejs:lithography_mask", "gtceu:yellow_glass_lens", 240, 8, GTLMaterials.Photoresist, provider);
        addRecipe("nm_wafer", "kubejs:rutherfordium_neutronium_wafer", "kubejs:lithography_mask", "gtceu:orange_glass_lens", 400, 7, GTLMaterials.Photoresist, provider);
        addRecipe("pm_wafer", "kubejs:taranium_wafer", "kubejs:lithography_mask", "gtceu:lime_glass_lens", 800, 8, GTLMaterials.EuvPhotoresist, provider);
        addRecipe("fm_wafer", "kubejs:pm_wafer", "kubejs:grating_lithography_mask", "gtceu:pink_glass_lens", 1080, 9, GTLMaterials.GammaRaysPhotoresist, provider);
        addRecipe("prepared_cosmic_soc_wafer", "kubejs:taranium_wafer", "kubejs:lithography_mask", "gtceu:black_glass_lens", 2160, 10, GTLMaterials.GammaRaysPhotoresist, provider);
        addRecipe("high_precision_crystal_soc", "gtceu:crystal_soc", "kubejs:lithography_mask", "gtceu:cyan_glass_lens", 960, 9, GTLMaterials.EuvPhotoresist, provider);
    }

    private static void addRecipe(String id, String input, String notitem_1, String notitem_2, int duration, int EUt, Material Fluid, Consumer<FinishedRecipe> provider) {
        new GTLAddRecipeBuilder(id, GTLAddRecipesTypes.PHOTON_MATRIX_ETCH)
                .inputItems(input).notConsumable(notitem_1).notConsumable(notitem_2)
                .inputFluids(Fluid.getFluid(50))
                .outputItems("kubejs:" + id)
                .TierEUtVA(EUt).duration(duration).cleanroom(CleanroomType.CLEANROOM)
                .save(provider);
    }
}
