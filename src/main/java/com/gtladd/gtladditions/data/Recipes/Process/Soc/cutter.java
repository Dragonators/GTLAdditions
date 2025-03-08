package com.gtladd.gtladditions.data.Recipes.Process.Soc;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gtladd.gtladditions.Common.Items.GTLAddItems;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import org.gtlcore.gtlcore.common.data.GTLMaterials;

import java.util.function.Consumer;

public class cutter {
    public cutter() {}

    public static void init(Consumer<FinishedRecipe> provider) {
        addCutterRecipe("echo_shard_wafer", GTLAddItems.ECHO_SHARD_BOULE, GTLAddItems.ECHO_SHARD_WAFER, 0, CleanroomType.STERILE_CLEANROOM, provider);
        addCutterRecipe("outstanding_soc", GTLAddItems.CHAOS_SOC, GTLAddItems.OUTSTANDING_SOC, 1000, CleanroomType.STERILE_CLEANROOM, provider);




    }
    public static void addCutterRecipe(String id, ItemEntry<Item> input, ItemEntry<Item> output, int EUt, CleanroomType cleanroomType, Consumer<FinishedRecipe> provider) {
        GTRecipeBuilder builder = GTRecipeTypes.CUTTER_RECIPES.recipeBuilder(id)
                .inputItems(input)
                .outputItems(output, 6)
                .EUt(EUt).cleanroom(cleanroomType);
        if (EUt > GTValues.VA[GTValues.UEV]) {
            builder.inputFluids(GTLMaterials.GradePurifiedWater16.getFluid(500)).duration(450).save(provider);
            return;
        }
        for (FluidStack inputfluid : new FluidStack[] {GTLMaterials.GradePurifiedWater8.getFluid(500), GTLMaterials.GradePurifiedWater16.getFluid(250)}) {
            builder.inputFluids(inputfluid).duration(inputfluid.equals(GTLMaterials.GradePurifiedWater8.getFluid(500)) ? 900 : 450).save(provider);
        }
    }
}
