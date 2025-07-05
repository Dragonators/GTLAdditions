package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.misc.GCyMRecipes;

import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;

import net.minecraft.data.recipes.FinishedRecipe;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(GCyMRecipes.class)
public class GCyMRecipesMixin {

    @Inject(method = "registerManual", at = @At("HEAD"), remap = false)
    private static void registerManual(Consumer<FinishedRecipe> provider, CallbackInfo ci) {
        GTLAddRecipesTypes.CHAOTIC_ALCHEMY.recipeBuilder(GTLAdditions.id("nickel_zinc_ferrite"))
                .inputItems(TagPrefix.dust, GTMaterials.Nickel).inputItems(TagPrefix.dust, GTMaterials.Zinc).inputItems(TagPrefix.dust, GTMaterials.Iron, 4)
                .circuitMeta(6).inputFluids(GTMaterials.Oxygen.getFluid(8000L))
                .outputFluids(GTMaterials.NickelZincFerrite.getFluid(864L))
                .duration(1800).EUt(GTValues.VA[2]).blastFurnaceTemp(1500).save(provider);
    }

    @Inject(method = "registerBinaryAlloy", at = @At("HEAD"), remap = false)
    private static void registerBinaryAlloy(@NotNull Material input1, int input1Amount, @NotNull Material input2, int input2Amount,
                                            @NotNull Material output, int outputAmount, int duration, Consumer<FinishedRecipe> provider, CallbackInfo ci) {
        GTLAddRecipesTypes.CHAOTIC_ALCHEMY.recipeBuilder(GTLAdditions.id(output.getName()))
                .inputItems(TagPrefix.dust, input1, input1Amount).inputItems(TagPrefix.dust, input2, input2Amount).circuitMeta(input1Amount + input2Amount)
                .outputFluids(output.getFluid(144L * (long) outputAmount))
                .duration(duration * 3 / 4).EUt(16L).blastFurnaceTemp(FluidHelper.getTemperature(output.getFluid(1L))).save(provider);
    }

    @Inject(method = "registerTrinaryAlloy", at = @At("HEAD"), remap = false)
    private static void registerTrinaryAlloy(@NotNull Material input1, int input1Amount, @NotNull Material input2, int input2Amount, @NotNull Material input3, int input3Amount,
                                             @NotNull Material output, int outputAmount, int duration, Consumer<FinishedRecipe> provider, CallbackInfo ci) {
        GTLAddRecipesTypes.CHAOTIC_ALCHEMY.recipeBuilder(GTLAdditions.id(output.getName()))
                .inputItems(TagPrefix.dust, input1, input1Amount).inputItems(TagPrefix.dust, input2, input2Amount).inputItems(TagPrefix.dust, input3, input3Amount)
                .circuitMeta(input1Amount + input2Amount + input3Amount)
                .outputFluids(output.getFluid(144L * (long) outputAmount))
                .duration(duration * 3 / 4).EUt(16L).blastFurnaceTemp(FluidHelper.getTemperature(output.getFluid(1L))).save(provider);
    }
}
