package com.gtladd.gtladditions.mixin.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;

import net.minecraft.world.item.crafting.Ingredient;

import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.config.ConfigHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(GTRecipe.class)
public abstract class GTRecipeMixin {

    /**
     * @author Dragons
     * @reason Item输出上限由Int更改为Long
     */
    @Overwrite(remap = false)
    public Map<RecipeCapability<?>, List<Content>> copyContents(Map<RecipeCapability<?>, List<Content>> contents,
                                                                @Nullable ContentModifier modifier) {
        Map<RecipeCapability<?>, List<Content>> copyContents = new HashMap<>(contents.size());
        for (var entry : contents.entrySet()) {
            var contentList = entry.getValue();
            var cap = entry.getKey();
            if (contentList != null && !contentList.isEmpty()) {
                List<Content> contentsCopy = new ArrayList<>();
                for (Content content : contentList) {
                    if (content.getContent() instanceof SizedIngredient sizedIngredient) {
                        int chance = content.chance;
                        int maxChance = content.maxChance;
                        int tierChanceBoost = content.tierChanceBoost;
                        String slotName = content.slotName;
                        String uiName = content.uiName;
                        long amount = sizedIngredient.getAmount();

                        if (modifier != null && chance != 0) {
                            amount = modifier.apply(amount).longValue();
                        } else {
                            contentsCopy.add(new Content(SizedIngredient.copy(sizedIngredient), chance, maxChance, tierChanceBoost, slotName, uiName));
                            continue;
                        }

                        int max = Integer.MAX_VALUE;
                        int times = Ints.saturatedCast(amount / max);
                        int maxTimes = ConfigHolder.INSTANCE.performance.recipeContentMaxTimes;
                        times = Math.min(times, maxTimes);
                        int remainder = (int) (amount % max);
                        Ingredient inner = sizedIngredient.getInner();

                        for (; times > 0; times--) {
                            contentsCopy.add(new Content(SizedIngredient.create(inner, max), chance, maxChance, tierChanceBoost, slotName, uiName));
                        }
                        if (remainder > 0) {
                            contentsCopy.add(new Content(SizedIngredient.create(inner, remainder), chance, maxChance, tierChanceBoost, slotName, uiName));
                        }
                    } else {
                        contentsCopy.add(content.copy(cap, modifier));
                    }
                }
                copyContents.put(entry.getKey(), contentsCopy);
            }
        }
        return copyContents;
    }
}
