package com.gtladd.gtladditions.data.Recipes.Process.Soc;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gtladd.gtladditions.Common.Items.GTLAddItems;
import com.gtladd.gtladditions.api.Registry.GTLAddRecipeBuilder;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import org.gtlcore.gtlcore.common.data.GTLMaterials;

import java.util.function.Consumer;

public class circuitassembler {
    public circuitassembler() {}
    public static void init(Consumer<FinishedRecipe> provider) {
        addCircuitRecipe("kubejs:bioware_processor", "kubejs:bioware_printed_circuit_board", GTLAddItems.OUTSTANDING_SOC, GTMaterials.Naquadah, GTMaterials.Dubnium, "kubejs:bioware_processor", GTValues.VA[GTValues.UHV], CleanroomType.STERILE_CLEANROOM, provider);
    }
    private static void addCircuitRecipe(String name, String inputs, ItemEntry<Item> input, Material material1, Material material2, String output, int EUt, CleanroomType cleanroomType, Consumer<FinishedRecipe> provider) {
        GTLAddRecipeBuilder builder = (GTLAddRecipeBuilder) new GTLAddRecipeBuilder(name, GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES)
                .inputItems(inputs).inputItems(input).inputItems(TagPrefix.wireFine, material1, 8).inputItems(TagPrefix.bolt, material2, 8)
                .outputItems(output).EUt(EUt).cleanroom(cleanroomType);
        if (EUt > GTValues.VA[GTValues.UEV]) builder.inputFluids(GTLMaterials.SuperMutatedLivingSolder.getFluid(144));
        else for (FluidStack inputfluid : new FluidStack[]{GTLMaterials.MutatedLivingSolder.getFluid(144), GTLMaterials.SuperMutatedLivingSolder.getFluid(72)}) builder.inputFluids(inputfluid);
        builder.duration(EUt > GTValues.VA[GTValues.UEV] ? 200 : 150).save(provider);
    }
}
