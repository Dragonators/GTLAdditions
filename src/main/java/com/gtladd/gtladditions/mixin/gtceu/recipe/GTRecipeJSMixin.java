package com.gtladd.gtladditions.mixin.gtceu.recipe;

import com.gregtechceu.gtceu.integration.kjs.recipe.GTRecipeSchema;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import dev.latvian.mods.kubejs.item.InputItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GTRecipeSchema.GTRecipeJS.class)
public abstract class GTRecipeJSMixin {

    @Shadow(remap = false)
    public int chance;

    @Shadow(remap = false)
    public int tierChanceBoost;

    @Shadow(remap = false)
    public GTRecipeSchema.GTRecipeJS inputItems(InputItem... inputs) {
        throw new AssertionError();
    }

    @SuppressWarnings("deprecation")
    @Inject(method = "chancedInput", at = @At("HEAD"), remap = false, cancellable = true)
    private void chancedInput(InputItem stack, int chance, int tierChanceBoost, CallbackInfoReturnable<GTRecipeSchema.GTRecipeJS> cir) {
        if (stack.count == 1) {
            Ingredient ingredient = stack.ingredient;
            ItemStack[] items = ingredient.getItems();
            if (items.length > 0) {
                String itemId = BuiltInRegistries.ITEM.getKey(items[0].getItem()).toString();
                if ("kubejs:glacio_spirit".equals(itemId) && chance == 8000 && tierChanceBoost == 100) {
                    int lastChance = this.chance;
                    int lastTierChanceBoost = this.tierChanceBoost;
                    this.chance = chance;
                    this.tierChanceBoost = -tierChanceBoost;
                    this.inputItems(stack);
                    this.chance = lastChance;
                    this.tierChanceBoost = lastTierChanceBoost;
                    cir.setReturnValue((GTRecipeSchema.GTRecipeJS) (Object) this);
                }
            }
        }
    }
}
