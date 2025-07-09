package com.gtladd.gtladditions.api.recipeslogic;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.IDistinctMachine;
import org.gtlcore.gtlcore.api.machine.trait.ILockRecipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.gtladd.gtladditions.api.machine.ILimitedDuration;
import com.hepdd.gtmthings.common.block.machine.trait.CatalystFluidStackHandler;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;

import javax.annotation.Nullable;

import static org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*;

public class GTLAddMultipleRecipesLogic extends RecipeLogic implements ILockRecipe {

    protected final ParallelMachine parallel;

    private final ILimitedDuration limited;

    public GTLAddMultipleRecipesLogic(ParallelMachine parallel) {
        super((IRecipeLogicMachine) parallel);
        this.parallel = parallel;
        limited = (ILimitedDuration) parallel;
    }

    @Override
    public WorkableElectricMultiblockMachine getMachine() {
        return (WorkableElectricMultiblockMachine) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        lastRecipe = null;
        var match = this.isLock() ? getLockrecipe() : getRecipe();
        if (match != null && matchRecipeOutput(machine, match)) {
            setupRecipe(match);
        }
    }

    @Nullable
    protected GTRecipe getRecipe() {
        if (!machine.hasProxies()) return null;
        Set<GTRecipe> recipes = this.machine.getRecipeType().getLookup().findRecipeCollisions(machine);
        int length = 0;
        if (recipes != null) length = recipes.size();
        if (length == 0) return null;
        long maxEUt = getMachine().getOverclockVoltage();
        long totalEu = 0;
        int parallel = this.parallel.getMaxParallel();
        long[] parallels = new long[length];
        int index = 0;
        int nullAmount = 0;
        long remaining = parallel * 64L;
        ObjectArrayFIFOQueue<RecipeData> queue = new ObjectArrayFIFOQueue<>();
        List<GTRecipe> recipeList = new ObjectArrayList<>(length);
        for (var r : recipes) {
            if (matchRecipe(machine, r) && checkRecipe(r)) {
                long p = LongParallelLogic.getMaxParallel(this.machine, r, parallel * 64L);
                recipeList.add(r);
                parallels[index] = Math.min(p, parallel * 64L / length);
                if (p > parallels[index]) queue.enqueue(new RecipeData(index, p - parallels[index]));
                remaining -= parallels[index++];
            } else nullAmount++;
        }
        if (nullAmount == length) return null;
        while (remaining > 0 && !queue.isEmpty()) {
            RecipeData recipeData = queue.dequeue();
            long canGive = remaining / (queue.size() + 1);
            if (canGive > 0) {
                long give = Math.min(recipeData.remainingWant, canGive);
                parallels[recipeData.index] += give;
                remaining -= give;
                long newRemaining = recipeData.remainingWant - give;
                if (newRemaining > 0) queue.enqueue(new RecipeData(recipeData.index, newRemaining));
            } else break;
        }
        index = 0;
        GTRecipe recipe = GTRecipeBuilder.ofRaw().buildRawRecipe();
        recipe.outputs.put(ItemRecipeCapability.CAP, new ArrayList<>());
        recipe.outputs.put(FluidRecipeCapability.CAP, new ArrayList<>());
        for (GTRecipe r : recipeList) {
            if (parallels[index] > 1) r = r.copy(ContentModifier.multiplier(parallels[index++]), false);
            if (handleRecipeInput(machine, r)) {
                totalEu += RecipeHelper.getInputEUt(r) * r.duration;
                List<Content> item = r.outputs.get(ItemRecipeCapability.CAP);
                if (item != null) recipe.outputs.get(ItemRecipeCapability.CAP).addAll(item);
                List<Content> fluid = r.outputs.get(FluidRecipeCapability.CAP);
                if (fluid != null) recipe.outputs.get(FluidRecipeCapability.CAP).addAll(fluid);
            }
        }
        if (recipe.outputs.get(ItemRecipeCapability.CAP).equals(new ArrayList<>()) &&
                recipe.outputs.get(FluidRecipeCapability.CAP).equals(new ArrayList<>()))
            return null;
        int minDuration = limited.getLimitedDuration();
        double d = (double) totalEu / maxEUt;
        long eut = d > minDuration ? maxEUt : (long) (maxEUt * d / minDuration);
        recipe.tickInputs.put(EURecipeCapability.CAP, List.of(
                new Content(eut, 10000, 10000, 0, null, null)));
        recipe.duration = (int) Math.max(d, minDuration);
        return recipe;
    }

    private GTRecipe getLockrecipe() {
        if (!machine.hasProxies()) return null;
        if (this.getLockRecipe() == null) this.setLockRecipe(machine.getRecipeType().getLookup().findRecipe(machine));
        else if (!matchRecipe(machine, this.getLockRecipe()) && !checkRecipe(this.getLockRecipe())) return null;
        GTRecipe recipe = this.getLockRecipe();
        if (recipe == null) return null;
        recipe = parallelRecipe(recipe, this.parallel.getMaxParallel() * 64);
        int minDuration = limited.getLimitedDuration();
        long maxEUt = getMachine().getOverclockVoltage();
        double d = (double) RecipeHelper.getInputEUt(recipe) * recipe.duration / maxEUt;
        long eut = d > minDuration ? maxEUt : (long) (maxEUt * d / minDuration);
        RecipeHelper.setInputEUt(recipe, eut);
        recipe.duration = (int) Math.max(d, minDuration);
        return recipe;
    }

    protected GTRecipe parallelRecipe(GTRecipe recipe, int max) {
        int maxMultipliers = Integer.MAX_VALUE;
        for (var cap : recipe.inputs.keySet()) {
            if (cap.doMatchInRecipe()) {
                int currentMultiplier = cap.getMaxParallelRatio(machine, recipe, max);
                if (currentMultiplier < maxMultipliers) maxMultipliers = currentMultiplier;
            }
        }
        if (maxMultipliers > 1) recipe = recipe.copy(ContentModifier.multiplier(maxMultipliers), false);
        return recipe;
    }

    @Override
    public void onRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe != null) {
            lastRecipe.postWorking(this.machine);
            handleRecipeOutput(machine, lastRecipe);
        }
        var match = this.isLock() ? getLockrecipe() : getRecipe();
        if (match != null && matchRecipeOutput(machine, match)) {
            setupRecipe(match);
            return;
        }
        setStatus(Status.IDLE);
        progress = 0;
        duration = 0;
    }

    private boolean checkRecipe(GTRecipe recipe) {
        boolean eut = RecipeHelper.getRecipeEUtTier(recipe) < getMachine().getTier();
        boolean ebf_temp = recipe.data.getInt("ebf_temp") != 0 &&
                machine instanceof CoilWorkableElectricMultiblockMachine coilMachine &&
                coilMachine.getCoilType().getCoilTemperature() > recipe.data.getInt("ebf_temp");
        return eut || ebf_temp;
    }

    record RecipeData(int index, long remainingWant) {}

    private class LongParallelLogic {

        public static long getMaxParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, long parallelAmount) {
            for (var cap : recipe.inputs.keySet()) {
                if (cap == ItemRecipeCapability.CAP) {
                    parallelAmount = Math.min(parallelAmount, getInputItemParallel(holder, recipe, parallelAmount));
                    if (parallelAmount == 0L) break;
                } else if (cap == FluidRecipeCapability.CAP) {
                    parallelAmount = Math.min(parallelAmount, getInputFluidParallel(holder, recipe, parallelAmount));
                    if (parallelAmount == 0L) break;
                }
            }
            return parallelAmount;
        }

        public static long getInputItemParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, long parallelAmount) {
            if (parallelAmount <= 1 || recipe.inputs.get(ItemRecipeCapability.CAP) == null) return parallelAmount;
            if (holder instanceof IDistinctMachine machine) {
                if (machine.getRecipeHandleParts().isEmpty()) return 0;
                Object2LongOpenCustomHashMap<ItemStack> ingredientStacks = new Object2LongOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());
                if (machine.isDistinct() && machine.getDistinctHatch() != null) {
                    for (var it = machine.getDistinctHatch().getContent(ItemRecipeCapability.CAP).object2LongEntrySet().fastIterator(); it.hasNext();) {
                        var entry = it.next();
                        ingredientStacks.computeLong((ItemStack) entry.getKey(), (k, v) -> v == null ? entry.getLongValue() : v + entry.getLongValue());
                    }
                } else {
                    Object2LongOpenCustomHashMap<ItemStack> map = new Object2LongOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());
                    for (var it : machine.getCapabilities().get(IO.IN)) {
                        for (var obj = it.getContent(ItemRecipeCapability.CAP).object2LongEntrySet().fastIterator(); obj.hasNext();) {
                            var entry = obj.next();
                            map.computeLong((ItemStack) entry.getKey(), (k, v) -> v == null ? entry.getLongValue() : v + entry.getLongValue());
                        }
                    }
                    for (var obj : map.object2LongEntrySet()) {
                        ingredientStacks.computeLong(obj.getKey(), (k, v) -> v == null ? obj.getLongValue() : v + obj.getLongValue());
                    }
                }
                Object2LongOpenHashMap<Ingredient> countableMap = new Object2LongOpenHashMap<>();
                for (Content content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
                    Ingredient recipeIngredient = ItemRecipeCapability.CAP.of(content.content);
                    int ingredientCount;
                    if (recipeIngredient instanceof SizedIngredient sizedIngredient) {
                        ingredientCount = sizedIngredient.getAmount();
                    } else if (recipeIngredient instanceof IntProviderIngredient intProviderIngredient) {
                        ingredientCount = intProviderIngredient.getSampledCount(GTValues.RNG);
                    } else {
                        ingredientCount = 1;
                    }
                    if (content.chance > 0) {
                        countableMap.addTo(recipeIngredient, ingredientCount);
                    }
                }
                if (countableMap.isEmpty()) return parallelAmount;
                long needed;
                long available;
                for (var it = Object2LongMaps.fastIterator(countableMap); it.hasNext(); parallelAmount = Math.min(parallelAmount, available / needed)) {
                    var entry = it.next();
                    needed = entry.getLongValue();
                    available = 0;
                    for (var iter = Object2LongMaps.fastIterator(ingredientStacks); iter.hasNext();) {
                        var inputItem = iter.next();
                        if (entry.getKey().test(inputItem.getKey())) {
                            available += inputItem.getLongValue();
                            break;
                        }
                    }
                    if (available < needed) {
                        parallelAmount = 0;
                        break;
                    }
                }
                return parallelAmount;
            }
            return 1;
        }

        public static long getInputFluidParallel(IRecipeCapabilityHolder holder, GTRecipe recipe, long parallelAmount) {
            if (parallelAmount <= 1 || recipe.inputs.get(FluidRecipeCapability.CAP) == null) return parallelAmount;
            if (holder instanceof IDistinctMachine machine) {
                if (machine.getRecipeHandleParts().isEmpty()) return 0;
                Object2LongOpenHashMap<FluidStack> ingredientStacks = new Object2LongOpenHashMap<>();
                if (machine.isDistinct() && machine.getDistinctHatch() != null) {
                    for (var it = machine.getDistinctHatch().getContent(FluidRecipeCapability.CAP).object2LongEntrySet().fastIterator(); it.hasNext();) {
                        var entry = it.next();
                        ingredientStacks.computeLong((FluidStack) entry.getKey(), (k, v) -> v == null ? entry.getLongValue() : v + entry.getLongValue());
                    }
                } else {
                    for (var container : machine.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP)) {
                        if (container instanceof CatalystFluidStackHandler) continue;
                        for (Object object : container.getContents()) {
                            if (object instanceof FluidStack fluidStack) {
                                ingredientStacks.computeLong(fluidStack, (k, v) -> v == null ? fluidStack.getAmount() : v + fluidStack.getAmount());
                            }
                        }
                    }
                }
                Object2LongOpenHashMap<FluidIngredient> fluidCountMap = new Object2LongOpenHashMap<>();
                for (Content content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
                    FluidIngredient fluidInput = FluidRecipeCapability.CAP.of(content.content);
                    if (content.chance > 0) {
                        fluidCountMap.addTo(fluidInput, fluidInput.getAmount());
                    }
                }
                if (fluidCountMap.isEmpty()) return parallelAmount;
                long needed;
                long available;
                for (var it = fluidCountMap.object2LongEntrySet().fastIterator(); it.hasNext(); parallelAmount = Math.min(parallelAmount, available / needed)) {
                    var entry = it.next();
                    needed = entry.getLongValue();
                    available = 0;
                    for (var iter = Object2LongMaps.fastIterator(ingredientStacks); iter.hasNext();) {
                        var inputFluid = iter.next();
                        if (entry.getKey().test(inputFluid.getKey())) {
                            available += inputFluid.getLongValue();
                            break;
                        }
                    }
                    if (available < needed) {
                        parallelAmount = 0;
                        break;
                    }
                }
                return parallelAmount;
            }
            return 1;
        }
    }
}
