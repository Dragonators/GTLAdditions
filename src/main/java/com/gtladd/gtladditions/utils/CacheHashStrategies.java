package com.gtladd.gtladditions.utils;

import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.core.mixins.*;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import it.unimi.dsi.fastutil.Hash;

import java.util.Arrays;
import java.util.Objects;

public class CacheHashStrategies {

    public static class IngredientHashStrategy implements Hash.Strategy<Ingredient> {

        public static final IngredientHashStrategy INSTANCE = new IngredientHashStrategy();
        private static final ItemStackHashStrategy ITEM_TAG_STRATEGY = ItemStackHashStrategy.comparingAllButCount();
        private static final ItemStackHashStrategy ITEM_STRATEGY = ItemStackHashStrategy.builder().compareItem(true).build();

        @Override
        public int hashCode(Ingredient o) {
            int hashCode = 537;
            if (o instanceof StrictNBTIngredientAccessor strict) {
                hashCode *= 31 * ITEM_TAG_STRATEGY.hashCode(strict.getStack());
            } else if (o instanceof PartialNBTIngredientAccessor partial) {
                hashCode *= 31 * partial.getNbt().hashCode();
                hashCode *= 31 * partial.getItems().hashCode();
            } else if (o instanceof IntersectionIngredientAccessor intersection) {
                for (Ingredient ingredient : intersection.getChildren()) {
                    hashCode *= 31 * this.hashCode(ingredient);
                }
            } else if (o instanceof IngredientAccessor ingredient) {
                for (Ingredient.Value value : ingredient.getValues()) {
                    if (value instanceof TagValueAccessor tagValue) {
                        hashCode *= 31 * tagValue.getTag().hashCode();
                    } else {
                        for (ItemStack stack : value.getItems()) {
                            hashCode *= 31 * ITEM_STRATEGY.hashCode(stack);
                        }
                    }
                }
            }
            return hashCode;
        }

        @Override
        public boolean equals(Ingredient a, Ingredient b) {
            return IngredientEquality.ingredientEquals(a, b);
        }
    }

    /**
     * FluidIngredient Hash策略 - 忽略amount数量
     * 基于FluidIngredient源代码的完整实现，支持TagValue和FluidValue
     */
    public static class FluidIngredientHashStrategy implements Hash.Strategy<FluidIngredient> {

        public static final FluidIngredientHashStrategy INSTANCE = new FluidIngredientHashStrategy();

        @Override
        public int hashCode(FluidIngredient ingredient) {
            if (ingredient == null) return 0;

            int result = Arrays.hashCode(ingredient.values);
            result = 31 * result + Objects.hashCode(ingredient.getNbt());
            return result;
        }

        @Override
        public boolean equals(FluidIngredient a, FluidIngredient b) {
            return fluidIngredientEqualsIgnoreAmount(a, b);
        }

        private static boolean fluidIngredientEqualsIgnoreAmount(FluidIngredient a, FluidIngredient b) {
            if (a == b) return true;
            if (a == null || b == null) return false;

            if (!Objects.equals(a.getNbt(), b.getNbt())) {
                return false;
            }

            if (a.values.length != b.values.length) {
                return false;
            }

            for (FluidIngredient.Value value1 : a.values) {
                for (FluidIngredient.Value value2 : b.values) {
                    if (value1 instanceof FluidIngredient.TagValue tagValue1) {
                        if (!(value2 instanceof FluidIngredient.TagValue tagValue2)) {
                            return false;
                        }
                        if (tagValue1.getTag() != tagValue2.getTag()) {
                            return false;
                        }
                    } else if (value1 instanceof FluidIngredient.FluidValue first) {
                        if (!(value2 instanceof FluidIngredient.FluidValue second)) {
                            return false;
                        }
                        if (first.hashCode() != second.hashCode()) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }
}
