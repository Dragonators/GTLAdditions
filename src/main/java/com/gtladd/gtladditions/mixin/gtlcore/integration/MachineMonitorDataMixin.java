package com.gtladd.gtladditions.mixin.gtlcore.integration;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.nbt.CompoundTag;

import com.gtladd.gtladditions.common.machine.multiblock.controller.ExoticChemicalComplex;
import com.gtladd.gtladditions.integration.gtlcore.monitor.ExoticChemicalMonitor;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.gtlcore.gtlcore.integration.monitor.MachineMonitorData", remap = false)
public abstract class MachineMonitorDataMixin {

    @Shadow
    private MetaMachine targetMachine;
    @Shadow
    private GTRecipe recipe;
    @Shadow
    private CompoundTag contents;
    @Shadow
    private int contentVersion;

    @Shadow
    abstract void invalidate();

    @Unique
    private GTRecipe[] gtladditions$sampledUnits;
    @Unique
    private int gtladditions$sampledRunningMask;

    @Inject(method = "sample",
            at = @At(value = "INVOKE",
                     target = "Lcom/gregtechceu/gtceu/api/machine/feature/IRecipeLogicMachine;getRecipeLogic()Lcom/gregtechceu/gtceu/api/machine/trait/RecipeLogic;",
                     ordinal = 0),
            cancellable = true,
            require = 1)
    private void gtladditions$sampleUnits(CallbackInfoReturnable<CompoundTag> cir,
                                          @Local(name = "machine") MetaMachine machine,
                                          @Local(name = "state") CompoundTag state) {
        if (!(machine instanceof ExoticChemicalComplex host)) return;

        var logic = host.getRecipeLogic();
        int mask = ExoticChemicalMonitor.writeState(state, logic);
        if (gtladditions$sampledUnits == null) gtladditions$sampledUnits = ExoticChemicalMonitor.sampledArray(logic);
        boolean batchesChanged = ExoticChemicalMonitor.syncBatches(logic, gtladditions$sampledUnits);

        if (targetMachine != host || batchesChanged || gtladditions$sampledRunningMask != mask) {
            contents = ExoticChemicalMonitor.contents(
                    logic,
                    state.getBoolean(ExoticChemicalMonitor.EXOTIC_MULTI_UNIT),
                    mask);
            targetMachine = host;
            recipe = null;
            gtladditions$sampledRunningMask = mask;
            contentVersion++;
        }

        state.putInt(ExoticChemicalMonitor.CONTENT_VERSION, contentVersion);
        cir.setReturnValue(state);
    }

    @Inject(method = "status", at = @At("HEAD"), require = 1)
    private void gtladditions$releaseLostTarget(CallbackInfoReturnable<CompoundTag> cir) {
        // A valid complex returns from sample above. Core's early status exits otherwise retain its old contents.
        if (targetMachine instanceof ExoticChemicalComplex) invalidate();
    }

    @Inject(method = "invalidate", at = @At("TAIL"), require = 1)
    private void gtladditions$clearSampledUnits(CallbackInfo ci) {
        gtladditions$sampledUnits = null;
        gtladditions$sampledRunningMask = 0;
    }
}
