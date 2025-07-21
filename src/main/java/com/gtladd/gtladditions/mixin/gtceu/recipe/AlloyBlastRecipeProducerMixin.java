package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.data.recipe.misc.alloyblast.AlloyBlastRecipeProducer;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.level.material.Fluid;

import com.gtladd.gtladditions.GTLAdditions;
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(AlloyBlastRecipeProducer.class)
public abstract class AlloyBlastRecipeProducerMixin {

    @Shadow(remap = false)
    protected abstract int addInputs(@NotNull Material material, @NotNull GTRecipeBuilder builder);

    @Shadow(remap = false)
    protected abstract void buildRecipes(@NotNull BlastProperty property, @NotNull Fluid molten, int outputAmount, int componentAmount, @NotNull GTRecipeBuilder builder, Consumer<FinishedRecipe> provider);

    @Inject(method = "produce", at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/data/recipe/misc/alloyblast/AlloyBlastRecipeProducer;buildRecipes(Lcom/gregtechceu/gtceu/api/data/chemical/material/properties/BlastProperty;Lnet/minecraft/world/level/material/Fluid;IILcom/gregtechceu/gtceu/data/recipe/builder/GTRecipeBuilder;Ljava/util/function/Consumer;)V"), remap = false)
    public void produce(@NotNull Material material, @NotNull BlastProperty property, Consumer<FinishedRecipe> provider, CallbackInfo ci) {
        GTRecipeBuilder builder = this.gtladd$createBuilder(property, material);
        this.buildRecipes(property, material.getFluid(), this.addInputs(material, builder), material.getMaterialComponents().size(), builder, provider);
    }

    @Inject(method = "addFreezerRecipes", at = @At(value = "HEAD"), remap = false)
    protected void addFreezerRecipes(@NotNull Material material, @NotNull Fluid molten, int temperature, Consumer<FinishedRecipe> provider, CallbackInfo ci) {
        GTRecipeBuilder freezerBuilder = GTLAddRecipesTypes.ANTIENTROPY_CONDENSATION.recipeBuilder(GTLAdditions.id(material.getName()))
                .inputFluids(FluidStack.create(molten, 144L)).duration((int) material.getMass() * 2)
                .notConsumable(GTItems.SHAPE_MOLD_INGOT.asStack()).outputItems(TagPrefix.ingot, material);
        freezerBuilder.EUt(120).save(provider);
    }

    @Unique
    protected @NotNull GTRecipeBuilder gtladd$createBuilder(@NotNull BlastProperty property, @NotNull Material material) {
        GTRecipeBuilder builder = GTLAddRecipesTypes.CHAOTIC_ALCHEMY.recipeBuilder(GTLAdditions.id(material.getName()));
        builder.duration(property.getDurationOverride() < 0 ? Math.max(1, (int) (material.getMass() * (long) property.getBlastTemperature() / 100L)) : (int) (property.getDurationOverride() * 0.75));
        builder.EUt(property.getEUtOverride() < 0 ? GTValues.VA[2] : property.getEUtOverride() / 2);
        return builder.blastFurnaceTemp(property.getBlastTemperature());
    }
}
