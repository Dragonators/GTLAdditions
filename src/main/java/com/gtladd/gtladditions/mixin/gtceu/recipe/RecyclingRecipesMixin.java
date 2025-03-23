package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.data.recipe.misc.RecyclingRecipes;
import com.gtladd.gtladditions.api.recipe.GTLAddRecipesTypes;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

@Mixin(RecyclingRecipes.class)
public class RecyclingRecipesMixin {
    @Inject(method = "registerExtractorRecycling",
            at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/data/chemical/ChemicalHelper;getUnificationEntry(Lnet/minecraft/world/level/ItemLike;)Lcom/gregtechceu/gtceu/api/data/chemical/material/stack/UnificationEntry;", shift = At.Shift.AFTER), remap = false)
    private static void registerExtractorRecycling(Consumer<FinishedRecipe> provider, ItemStack input, List<MaterialStack> materials, int multiplier, @Nullable TagPrefix prefix, CallbackInfo ci) {
        MaterialStack ms;
        if (prefix != null && prefix.secondaryMaterials().isEmpty()) {
            ms = ChemicalHelper.getMaterial(input);
            if (ms != null && ms.material() != null) {
                Material m = ms.material();
                if (m.hasProperty(PropertyKey.FLUID) && m.getFluid() != null && prefix == TagPrefix.dust) {
                        ResourceLocation itemPath = BuiltInRegistries.ITEM.getKey(input.getItem());
                        GTRecipeBuilder builder = GTLAddRecipesTypes.MOLECULAR_DECONSTRUCTION.recipeBuilder("molecular_deconstruction_" + itemPath.getPath())
                                .inputItems(TagPrefix.dust, m)
                                .outputFluids(m.getFluid(144L))
                                .duration((int)Math.max(1L, ms.amount() * ms.material().getMass() / 3628800L)).EUt((long) GTValues.VA[1] * (long)multiplier / 4L);
                        builder.save(provider);
                }
            }
        }
    }
}
