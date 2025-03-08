package com.gtladd.gtladditions.data.Recipes.Process.Soc;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gtladd.gtladditions.Common.Items.GTLAddItems;
import com.gtladd.gtladditions.api.Recipe.GTLAddRecipesTypes;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.gtlcore.gtlcore.common.data.GTLMaterials;
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes;
import org.gtlcore.gtlcore.utils.Registries;

import java.util.function.Consumer;

public class engravingarray {
    public engravingarray() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        addEngravingRecipe("outstanding_soc_wafer", GTLAddItems.BIOWARE_ECHO_SHARD_BOULE, GTLMaterials.Photoresist, "kubejs:grating_lithography_mask", GTLAddItems.OUTSTANDING_SOC_WAFER, GTValues.VA[GTValues.UHV], 100, CleanroomType.STERILE_CLEANROOM, provider);
    }
    private static void addEngravingRecipe(String name, ItemEntry<Item> input, Material fluid, String noinput, ItemEntry<Item> output, int EUt, int duration, CleanroomType cleanroomType, Consumer<FinishedRecipe> provider) {
        GTLRecipeTypes.DIMENSIONAL_FOCUS_ENGRAVING_ARRAY_RECIPES.recipeBuilder(name)
                .inputItems(input).notConsumable(new ItemStack(Registries.getItem(noinput))).inputFluids(fluid.getFluid(100))
                .outputItems(output)
                .EUt(EUt).duration(duration).cleanroom(cleanroomType).save(provider);
        GTLAddRecipesTypes.PHOTON_MATRIX_ETCH.recipeBuilder(name)
                .inputItems(input).notConsumable(new ItemStack(Registries.getItem(noinput))).inputFluids(fluid.getFluid(75))
                .outputItems(output)
                .EUt(EUt / 4).duration((int) (duration * 0.75)).cleanroom(cleanroomType).save(provider);
    }
}
