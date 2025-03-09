package com.gtladd.gtladditions.data.recipes.newmachinerecipe;

import org.gtlcore.gtlcore.common.data.GTLMaterials;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.api.recipe.GTLAddRecipesTypes;

import java.util.function.Consumer;

public class StellarLgnition {

    public StellarLgnition() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        addRecipe("argon", GTMaterials.Argon, 36000, provider);
        addRecipe("helium", GTMaterials.Helium, 36000, provider);
        addRecipe("iron", GTMaterials.Iron, 52000, provider);
        addRecipe("nickel", GTMaterials.Nickel, 52000, provider);
        addRecipe("nitrogen", GTMaterials.Nitrogen, 21600, provider);
        addRecipe("oxygen", GTMaterials.Oxygen, 32000, provider);
        addRecipe("silver", GTMaterials.Silver, 56000, provider);
        addRecipe("vibranium", GTLMaterials.Vibranium, 72000, provider);
        addRecipe("mithril", GTLMaterials.Mithril, 64000, provider);
        addRecipe("starmetal", GTLMaterials.Starmetal, 72000, provider);
        addRecipe("orichalcum", GTLMaterials.Orichalcum, 56000, provider);
        addRecipe("infuscolium", GTLMaterials.Infuscolium, 48000, provider);
        addRecipe("enderium", GTLMaterials.Enderium, 81000, provider);
    }

    private static void addRecipe(String name, Material material, int temperature, Consumer<FinishedRecipe> provider) {
        GTLAddRecipesTypes.STELLAR_LGNITION.recipeBuilder(name).circuitMeta(1)
                .inputFluids(material.getFluid(10000))
                .outputFluids(material.getFluid(FluidStorageKeys.PLASMA, 10000))
                .blastFurnaceTemp(temperature)
                .EUt(GTValues.VA[GTValues.OpV]).duration(100).save(provider);
    }
}
