package com.gtladd.gtladditions.data.recipes.newmachinerecipe;

import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;

import java.util.function.Consumer;

public class ChaosWeave {

    public ChaosWeave() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        GTLAddRecipesTypes.CHAOS_WEAVE.recipeBuilder(GTLAdditions.id("chaos_weave")).inputItems(TagPrefix.dust, GTMaterials.Stone, 64)
                .outputItems(Registries.getItemStack("kubejs:scrap_box", 24)).duration(100).EUt(GTValues.V[10]).save(provider);
    }
}
