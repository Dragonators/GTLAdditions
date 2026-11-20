package com.gtladd.gtladditions.mixin.gtceu.xei;

import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.library.load.registration.RecipeCatalystRegistration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RecipeCatalystRegistration.class, remap = false)
public abstract class RecipeCatalystRegistrationMixin {

    @Inject(method = "addRecipeCatalyst", at = @At("HEAD"), cancellable = true, require = 1)
    private <T> void gtladditions$hideLegacyMachines(IIngredientType<T> ingredientType, T ingredient, RecipeType<?>[] recipeTypes, CallbackInfo ci) {
        if (ingredient instanceof ItemStack stack &&
                (stack.is(MultiBlockMachine.INSTANCE.getAPOCALYPTIC_TORSION_QUANTUM_MATRIX().getItem()) ||
                        stack.is(MultiBlockMachine.INSTANCE.getHEART_OF_THE_UNIVERSE().getItem()) ||
                        stack.is(MultiBlockMachine.INSTANCE.getATOMIC_TRANSMUTATIOON_CORE().getItem()) ||
                        stack.is(MultiBlockMachine.INSTANCE.getDIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT().getItem()))) {
            ci.cancel();
        }
    }
}