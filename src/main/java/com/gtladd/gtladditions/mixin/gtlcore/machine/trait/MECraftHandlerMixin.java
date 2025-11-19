package com.gtladd.gtladditions.mixin.gtlcore.machine.trait;

import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient;
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MECraftHandler;
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEMolecularAssemblerIOPartMachine;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.stacks.AEItemKey;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(MECraftHandler.class)
public abstract class MECraftHandlerMixin {

    @Shadow(remap = false)
    public MEMolecularAssemblerIOPartMachine getMachine() {
        throw new AssertionError();
    }

    @Inject(method = "extractGTRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    public void extractGTRecipe(long parallelAmount, int tickDuration, CallbackInfoReturnable<GTRecipe> cir) {
        if (parallelAmount != Long.MIN_VALUE) return;
        GTRecipe output = GTRecipeBuilder.ofRaw().buildRawRecipe();
        List<Content> outputList = output.outputs.computeIfAbsent(ItemRecipeCapability.CAP, cap -> new ObjectArrayList<>());
        for (var it = Object2LongMaps.fastIterator(getMachine().getOutputItems()); it.hasNext();) {
            var entry = it.next();
            var key = entry.getKey();
            if (!(key.what() instanceof AEItemKey aeItemKey)) {
                it.remove();
                continue;
            }
            Item item = aeItemKey.getItem();
            long multiply = entry.getLongValue();

            var cont = new Content(LongIngredient.create(Ingredient.of(item), multiply * key.amount()), ChanceLogic.getMaxChancedValue(), ChanceLogic.getMaxChancedValue(), 0, null, null);
            outputList.add(cont);
            it.remove();
        }
        if (outputList.isEmpty()) cir.setReturnValue(null);
        else {
            output.duration = 1;
            cir.setReturnValue(output);
        }
    }
}
