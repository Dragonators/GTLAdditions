package com.gtladd.gtladditions.mixin.gtceu.xei;

import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiRegistryImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = EmiRegistryImpl.class, remap = false)
public abstract class EmiRegistryImplMixin {

    @Inject(method = "addWorkstation", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtladditions$hideLegacyMachines(EmiRecipeCategory category, EmiIngredient workstation, CallbackInfo ci) {
        if (workstation instanceof EmiStack stack &&
                (stack.getKey() == MultiBlockMachine.INSTANCE.getAPOCALYPTIC_TORSION_QUANTUM_MATRIX().getItem() ||
                        stack.getKey() == MultiBlockMachine.INSTANCE.getHEART_OF_THE_UNIVERSE().getItem() ||
                        stack.getKey() == MultiBlockMachine.INSTANCE.getATOMIC_TRANSMUTATIOON_CORE().getItem() ||
                        stack.getKey() == MultiBlockMachine.INSTANCE.getDIMENSIONALLY_TRANSCENDENT_CHEMICAL_PLANT().getItem())) {
            ci.cancel();
        }
    }
}