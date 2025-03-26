package com.gtladd.gtladditions.api.registry;

import org.gtlcore.gtlcore.utils.Registries;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.tag.TagUtil;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.recipe.condition.CleanroomCondition;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GTLAddRecipeBuilder extends GTRecipeBuilder {

    public GTLAddRecipeBuilder(String id, GTRecipeType recipeType) {
        super(GTCEu.id(id), recipeType);
    }

    @Override
    public GTLAddRecipeBuilder circuitMeta(int configuration) {
        return (GTLAddRecipeBuilder) this.notConsumable(IntCircuitIngredient.circuitInput(configuration));
    }

    public GTLAddRecipeBuilder inputItems(String input, int number) {
        return (GTLAddRecipeBuilder) super.inputItems(new ItemStack(Registries.getItem(input), number));
    }

    public GTLAddRecipeBuilder inputItems(String input) {
        return this.inputItems(input, 1);
    }

    public GTLAddRecipeBuilder InputItems(String inputitems) {
        String[] split = inputitems.split(" ");
        return this.inputItems(split[1], Integer.parseInt(split[0].replaceAll("x", "")));
    }

    public GTLAddRecipeBuilder inputItemsTag(String input, int number) {
        return (GTLAddRecipeBuilder) super.inputItems(SizedIngredient.create(TagUtil.createItemTag(input), number));
    }

    public GTLAddRecipeBuilder inputItemsTag(String input) {
        return this.inputItemsTag(input, 1);
    }

    public GTLAddRecipeBuilder inputItemsModTag(String input, int number) {
        return (GTLAddRecipeBuilder) super.inputItems(SizedIngredient.create(TagUtil.createModItemTag(input), number));
    }

    public GTLAddRecipeBuilder inputItemsModTag(String input) {
        return this.inputItemsModTag(input, 1);
    }

    @Override
    public GTLAddRecipeBuilder inputItems(TagPrefix orePrefix, Material material, int count) {
        TagKey<Item> tag = ChemicalHelper.getTag(orePrefix, material);
        return (GTLAddRecipeBuilder) (tag == null ? this.inputItems(ChemicalHelper.get(orePrefix, material, count)) : this.inputItems(tag, count));
    }

    public GTLAddRecipeBuilder inputItems(TagPrefix orePrefix, Material material) {
        return this.inputItems(orePrefix, material, 1);
    }

    @Override
    public GTLAddRecipeBuilder inputItems(MachineDefinition machine, int count) {
        return (GTLAddRecipeBuilder) super.inputItems(machine.asStack(count));
    }

    public GTLAddRecipeBuilder inputItems(MachineDefinition machine) {
        return this.inputItems(machine, 1);
    }

    public GTLAddRecipeBuilder chancedInputItems(String input, double chance, double tierChanceBoost) {
        return (GTLAddRecipeBuilder) super.chancedInput(new ItemStack(Registries.getItem(input)), (int) chance * 100, (int) tierChanceBoost * 100);
    }

    @Override
    public GTLAddRecipeBuilder inputFluids(FluidStack input) {
        return (GTLAddRecipeBuilder) input(FluidRecipeCapability.CAP, FluidIngredient.of(
                TagUtil.createFluidTag(BuiltInRegistries.FLUID.getKey(input.getFluid()).getPath()),
                input.getAmount(), input.getTag()));
    }

    public GTLAddRecipeBuilder inputFluids(String input, int count) {
        return (GTLAddRecipeBuilder) super.inputFluids(Objects.requireNonNull(GTMaterials.get(input)).getFluid(count));
    }

    public GTLAddRecipeBuilder outputItems(String output, int number) {
        return (GTLAddRecipeBuilder) super.outputItems(new ItemStack(Registries.getItem(output), number));
    }

    public GTLAddRecipeBuilder outputItems(String output) {
        return this.outputItems(output, 1);
    }

    public GTLAddRecipeBuilder OutputItems(String outputitems) {
        String[] split = outputitems.split(" ");
        return this.outputItems(split[1], Integer.parseInt(split[0].replaceAll("x", "")));
    }

    @Override
    public GTLAddRecipeBuilder outputItems(TagPrefix orePrefix, Material material, int count) {
        return (GTLAddRecipeBuilder) this.outputItems(ChemicalHelper.get(orePrefix, material, count));
    }

    public GTLAddRecipeBuilder outputItems(TagPrefix orePrefix, Material material) {
        return this.outputItems(orePrefix, material, 1);
    }

    @Override
    public GTLAddRecipeBuilder outputItems(MachineDefinition machine, int count) {
        return (GTLAddRecipeBuilder) super.outputItems(machine.asStack(count));
    }

    public GTLAddRecipeBuilder outputItems(MachineDefinition machine) {
        return this.outputItems(machine, 1);
    }

    @Override
    public GTLAddRecipeBuilder chancedOutput(TagPrefix tag, Material mat, int chance, int tierChanceBoost) {
        return this.chancedOutput(tag, mat, 1, chance, tierChanceBoost);
    }

    @Override
    public GTLAddRecipeBuilder chancedOutput(TagPrefix tag, Material mat, int count, int chance, int tierChanceBoost) {
        return (GTLAddRecipeBuilder) super.chancedOutput(ChemicalHelper.get(tag, mat, count), chance, tierChanceBoost);
    }

    public GTLAddRecipeBuilder chancedOutputItems(String output, double chance, double tierChanceBoost) {
        return this.chancedOutputItems(output, 1, chance, tierChanceBoost);
    }

    public GTLAddRecipeBuilder chancedOutputItems(String output, int count, double chance, double tierChanceBoost) {
        return (GTLAddRecipeBuilder) super.chancedOutput(new ItemStack(Registries.getItem(output), count), (int) (chance * 100), (int) (tierChanceBoost * 100));
    }

    @Override
    public GTLAddRecipeBuilder outputFluids(FluidStack output) {
        return (GTLAddRecipeBuilder) output(FluidRecipeCapability.CAP, FluidIngredient.of(output));
    }

    public GTLAddRecipeBuilder notConsumable(String input) {
        return this.notConsumable(input, 1);
    }

    public GTLAddRecipeBuilder notConsumable(String input, int count) {
        return (GTLAddRecipeBuilder) super.notConsumable(new ItemStack(Registries.getItem(input), count));
    }

    @Override
    public GTLAddRecipeBuilder notConsumable(Supplier<? extends Item> item) {
        int lastChance = this.chance;
        this.chance = 0;
        this.inputItems(item);
        this.chance = lastChance;
        return this;
    }

    public GTLAddRecipeBuilder notConsumableFluid(FluidStack fluid) {
        return (GTLAddRecipeBuilder) super.notConsumableFluid(FluidIngredient.of(TagUtil.createFluidTag(BuiltInRegistries.FLUID.getKey(fluid.getFluid()).getPath()), fluid.getAmount()));
    }

    @Override
    public GTLAddRecipeBuilder EUt(long eu) {
        boolean lastPerTick = this.perTick;
        this.perTick = true;
        if (eu > 0L) {
            this.tickInput.remove(EURecipeCapability.CAP);
            this.inputEU(eu);
        } else if (eu < 0L) {
            this.tickOutput.remove(EURecipeCapability.CAP);
            this.outputEU(-eu);
        }
        this.perTick = lastPerTick;
        return this;
    }

    public GTLAddRecipeBuilder TierEUtV(int tier) {
        return (GTLAddRecipeBuilder) super.EUt(GTValues.V[tier]);
    }

    public GTLAddRecipeBuilder TierEUtVA(int tier) {
        return (GTLAddRecipeBuilder) super.EUt(GTValues.VA[tier]);
    }

    @Override
    public GTLAddRecipeBuilder duration(int duration) {
        this.duration = duration;
        return this;
    }

    @Override
    public GTLAddRecipeBuilder cleanroom(CleanroomType cleanroomType) {
        return (GTLAddRecipeBuilder) this.addCondition(new CleanroomCondition(cleanroomType));
    }

    @Override
    public GTLAddRecipeBuilder blastFurnaceTemp(int blastTemp) {
        return (GTLAddRecipeBuilder) this.addData("ebf_temp", blastTemp);
    }

    public void save(Consumer<FinishedRecipe> consumer) {
        super.save(consumer);
    }
}
