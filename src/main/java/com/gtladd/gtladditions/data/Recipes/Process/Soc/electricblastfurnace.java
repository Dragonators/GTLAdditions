package com.gtladd.gtladditions.data.Recipes.Process.Soc;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gtladd.gtladditions.Common.Items.GTLAddItems;
import com.gtladd.gtladditions.Common.Material.GTLAddMaterial;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import org.gtlcore.gtlcore.common.data.GTLMaterials;

import java.util.function.Consumer;

public class electricblastfurnace {
    public electricblastfurnace() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        addBlastRecipe("echo_shard_boule", GTLMaterials.Echoite, GTLAddItems.ECHO_SHARD_BOULE, GTValues.VA[GTValues.UV], 12000, 14400, provider);
        addBlastRecipe("hassium_boule", GTMaterials.Hassium, GTLAddItems.HASSIUM_BOULE, GTValues.VA[GTValues.UHV], 12000, 18000, provider);
        addBlastRecipe("starmetal_boule", GTLMaterials.Starmetal, GTLAddItems.STARMETAL_BOULE, GTValues.VA[GTValues.UEV], 12000, 21000, provider);
    }
    public static void addBlastRecipe(String name, Material input, ItemEntry<Item> output, int EUt, int duration, int temperature, Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.BLAST_RECIPES.recipeBuilder(name)
                .inputItems(GTItems.SILICON_BOULE, 64).inputItems(GTLAddMaterial.GALLIUM_OXIDE, 16).inputItems(TagPrefix.dust, input, 16)
                .inputFluids(GTMaterials.Krypton.getFluid(16000))
                .outputItems(output).EUt(EUt).duration(duration).blastFurnaceTemp(temperature)
                .save(provider);
    }
}
