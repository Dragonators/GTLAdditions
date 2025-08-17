package com.gtladd.gtladditions.utils;

import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.core.mixins.*;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.IntersectionIngredient;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import net.minecraftforge.common.crafting.StrictNBTIngredient;

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class IngredientEquality {

    public static final Comparator<Item> ITEM_COMPARATOR = Comparator.comparing(BuiltInRegistries.ITEM::getKey);

    public static final Comparator<Ingredient.Value> INGREDIENT_VALUE_COMPARATOR = (value1, value2) -> {
        if (value1 instanceof TagValueAccessor first) {
            if (!(value2 instanceof TagValueAccessor second)) {
                return 10;
            }
            if (first.getTag() != second.getTag()) {
                return 1;
            }
        } else if (value1 instanceof ItemValueAccessor first) {
            if (!(value2 instanceof ItemValueAccessor second)) {
                return 10;
            }
            return ITEM_COMPARATOR.compare(first.getItem().getItem(), second.getItem().getItem());
        }
        return 0;
    };

    public static final Comparator<Ingredient> INGREDIENT_COMPARATOR = new Comparator<>() {

        @Override
        public int compare(Ingredient first, Ingredient second) {
            if (first instanceof StrictNBTIngredient strict1) {
                if (second instanceof StrictNBTIngredientAccessor strict2) {
                    return strict1.test(strict2.getStack()) ? 0 : 1;
                }
                return 1;
            }
            if (first instanceof PartialNBTIngredient partial1) {
                if (second instanceof PartialNBTIngredient partial2) {
                    if (partial1.getItems().length != partial2.getItems().length)
                        return 1;
                    for (ItemStack stack : partial1.getItems()) {
                        if (!partial2.test(stack)) {
                            return 1;
                        }
                    }
                    return 0;
                }
                return 1;
            }

            if (first instanceof IntersectionIngredient intersection1) {
                if (second instanceof IntersectionIngredient intersection2) {
                    List<Ingredient> ingredients1 = Lists
                            .newArrayList(((IntersectionIngredientAccessor) intersection1).getChildren());
                    List<Ingredient> ingredients2 = Lists
                            .newArrayList(((IntersectionIngredientAccessor) intersection2).getChildren());
                    if (ingredients1.size() != ingredients2.size()) return 1;

                    ingredients1.sort(this);
                    ingredients2.sort(this);

                    for (int i = 0; i < ingredients1.size(); ++i) {
                        Ingredient ingredient1 = ingredients1.get(i);
                        Ingredient ingredient2 = ingredients2.get(i);
                        int result = compare(ingredient1, ingredient2);
                        if (result != 0) {
                            return result;
                        }
                    }
                    return 0;
                }
                return 1;
            }

            Ingredient.Value[] firstValues = ((IngredientAccessor) first).getValues();
            Ingredient.Value[] secondValues = ((IngredientAccessor) second).getValues();
            if (firstValues.length != secondValues.length) return 1;

            firstValues = firstValues.clone();
            secondValues = secondValues.clone();
            Arrays.parallelSort(firstValues, INGREDIENT_VALUE_COMPARATOR);
            Arrays.parallelSort(secondValues, INGREDIENT_VALUE_COMPARATOR);

            for (int i = 0; i < firstValues.length; ++i) {
                Ingredient.Value value1 = firstValues[i];
                Ingredient.Value value2 = secondValues[i];
                int result = INGREDIENT_VALUE_COMPARATOR.compare(value1, value2);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }
    };

    public static boolean ingredientEquals(Ingredient first, Ingredient second) {
        if (first == second) return true;
        if (first == null || second == null) return false;

        first = getInner(first);
        second = getInner(second);
        return cmp(first, second);
    }

    private static boolean cmp(Ingredient first, Ingredient second) {
        return INGREDIENT_COMPARATOR.compare(first, second) == 0;
    }

    private static Ingredient getInner(Ingredient ingredient) {
        if (ingredient instanceof SizedIngredient sizedIngredient) {
            return getInner(sizedIngredient.getInner());
        } else if (ingredient instanceof IntProviderIngredient intProviderIngredient) {
            return getInner(intProviderIngredient.getInner());
        }
        return ingredient;
    }
}
