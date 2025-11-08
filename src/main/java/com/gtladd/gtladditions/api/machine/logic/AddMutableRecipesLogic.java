package com.gtladd.gtladditions.api.machine.logic;

import org.gtlcore.gtlcore.api.recipe.IGTRecipe;
import org.gtlcore.gtlcore.api.recipe.IParallelLogic;
import org.gtlcore.gtlcore.api.recipe.RecipeResult;

import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.gtladd.gtladditions.api.machine.IWirelessThreadModifierParallelMachine;
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipe;
import com.gtladd.gtladditions.api.recipe.WirelessGTRecipeBuilder;
import com.gtladd.gtladditions.common.record.ParallelData;
import com.gtladd.gtladditions.common.record.RecipeData;
import com.gtladd.gtladditions.utils.CommonUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.handleRecipeInput;
import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipeInput;

public class AddMutableRecipesLogic<T extends WorkableElectricMultiblockMachine & IRecipeLogicMachine & IWirelessThreadModifierParallelMachine> extends MutableRecipesLogic<T> {

    public AddMutableRecipesLogic(T machine) {
        super(machine);
    }

    @Override
    protected @Nullable GTRecipe getRecipe() {
        if (!machine.hasProxies()) return null;
        if (getMachine().getOverclockVoltage() <= 0) return null;

        final var parallelData = calculateParallels();
        if (parallelData == null) return null;

        final var wirelessTrait = getMachine().getWirelessNetworkEnergyHandler();
        return wirelessTrait != null ? buildFinalWirelessRecipe(parallelData, wirelessTrait) : buildFinalNormalRecipe(parallelData);
    }

    @Nullable
    protected ParallelData calculateParallels() {
        var recipes = this.lookupRecipeSet();
        int length = recipes.size();
        if (length == 0) return null;

        long totalParallel = (long) getMachine().getMaxParallel() * getMultipleThreads();
        long remaining = totalParallel;
        long[] parallels = new long[length];
        int index = 0;
        var queue = new ObjectArrayFIFOQueue<RecipeData>(length);
        var recipeList = new ObjectArrayList<GTRecipe>(length);

        for (var r : recipes) {
            if (r == null) continue;
            var pair = calculateParallel(machine, r, totalParallel);
            long p = pair.firstLong();
            if (p <= 0) continue;
            recipeList.add(r);
            parallels[index] = Math.min(p, totalParallel / length);
            if (p > parallels[index]) queue.enqueue(new RecipeData(index, p - parallels[index]));
            remaining -= parallels[index++];
        }

        return CommonUtils.getParallelData(length, remaining, parallels, queue, recipeList);
    }

    @Nullable
    protected GTRecipe buildFinalNormalRecipe(ParallelData parallelData) {
        long maxEUt = getMachine().getOverclockVoltage();

        final var itemOutputs = new ObjectArrayList<Content>();
        final var fluidOutputs = new ObjectArrayList<Content>();
        GTRecipe recipe = GTRecipeBuilder.ofRaw().buildRawRecipe();
        recipe.outputs.put(ItemRecipeCapability.CAP, itemOutputs);
        recipe.outputs.put(FluidRecipeCapability.CAP, fluidOutputs);

        double euMultiplier = this.getEuMultiplier();
        double totalEu = 0;
        int index = 0;

        for (var r : parallelData.recipeList) {
            if (parallelData.parallels[index] > 1) r = r.copy(ContentModifier.multiplier(parallelData.parallels[index]), false);
            IGTRecipe.of(r).setRealParallels(parallelData.parallels[index++]);
            r = IParallelLogic.getRecipeOutputChance(machine, r);
            if (matchRecipeInput(machine, r) && handleRecipeInput(machine, r)) {
                totalEu += getRecipeEut(r) * r.duration * euMultiplier;
                var item = r.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) itemOutputs.addAll(item);
                var fluid = r.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) fluidOutputs.addAll(fluid);
            }
            if (totalEu / maxEUt > 20 * 500) break;
        }

        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
            if (getRecipeStatus() == null || getRecipeStatus().isSuccess()) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND);
            return null;
        }

        double d = totalEu / maxEUt;
        long eut = d > 20 ? maxEUt : (long) (maxEUt * d / 20);
        recipe.tickInputs.put(EURecipeCapability.CAP, List.of(new Content(eut, 10000, 10000, 0, null, null)));
        recipe.duration = (int) Math.max(d, 20);
        IGTRecipe.of(recipe).setHasTick(true);
        return recipe;
    }

    @Nullable
    protected WirelessGTRecipe buildFinalWirelessRecipe(ParallelData parallelData, @NotNull IWirelessNetworkEnergyHandler wirelessTrait) {
        final BigInteger maxTotalEu = wirelessTrait.getMaxAvailableEnergy();
        final double euMultiplier = this.getEuMultiplier();
        final var itemOutputs = new ObjectArrayList<Content>();
        final var fluidOutputs = new ObjectArrayList<Content>();

        BigInteger totalEu = BigInteger.ZERO;
        int index = 0;

        for (var r : parallelData.recipeList) {
            BigInteger parallelEUt = BigInteger.valueOf(RecipeHelper.getInputEUt(r));

            final long p = parallelData.parallels[index++];
            if (p > 1) {
                r = r.copy(ContentModifier.multiplier(p), false);
                parallelEUt = parallelEUt.multiply(BigInteger.valueOf(p));
            }
            IGTRecipe.of(r).setRealParallels(p);

            var tempTotalEu = totalEu.add(BigDecimal.valueOf(r.duration * euMultiplier).multiply(new BigDecimal(parallelEUt)).toBigInteger());
            if (tempTotalEu.compareTo(maxTotalEu) > 0) {
                if (totalEu.signum() == 0) RecipeResult.of(machine, RecipeResult.FAIL_NO_ENOUGH_EU_IN);
                break;
            }

            r = IParallelLogic.getRecipeOutputChance(machine, r);
            if (matchRecipeInput(machine, r) && handleRecipeInput(machine, r)) {
                totalEu = tempTotalEu;
                var item = r.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) itemOutputs.addAll(item);
                var fluid = r.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) fluidOutputs.addAll(fluid);
            }
        }

        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) {
            if (getRecipeStatus() == null || getRecipeStatus().isSuccess()) RecipeResult.of(this.machine, RecipeResult.FAIL_FIND);
            return null;
        }

        BigInteger eut = totalEu.divide(BigInteger.valueOf(20)).negate();
        return WirelessGTRecipeBuilder
                .ofRaw()
                .output(ItemRecipeCapability.CAP, itemOutputs)
                .output(FluidRecipeCapability.CAP, fluidOutputs)
                .duration(20)
                .setWirelessEut(eut)
                .buildRawRecipe();
    }

    protected @NotNull Set<GTRecipe> lookupRecipeSet() {
        var iter = this.lookupRecipeIterator();
        var recipeSet = new ObjectOpenHashSet<GTRecipe>();
        while (iter.hasNext()) recipeSet.add(iter.next());
        recipeSet.remove(null);
        return recipeSet;
    }
}
