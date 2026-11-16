package com.gtladd.gtladditions.mixin.gtlcore.pattern;

import org.gtlcore.gtlcore.api.pattern.AdvancedBlockPattern;
import org.gtlcore.gtlcore.common.item.UltimateTerminalBehavior;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import net.minecraft.world.entity.player.Player;

import com.gtladd.gtladditions.api.machine.feature.IStructureTierMachine;
import com.gtladd.gtladditions.common.machine.trait.StructureModifyTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AdvancedBlockPattern.class, remap = false)
public abstract class AdvancedBlockPatternMixin {

    @Inject(method = "dismantleMultiblock", at = @At("HEAD"), require = 1)
    private void gtladditions$restoreBeforeDismantle(IMultiController controller, Player player,
                                                     UltimateTerminalBehavior.AutoBuildSetting autoBuildSetting, CallbackInfo ci) {
        // The terminal skips real air. Restore before its scan, not from a later block-update callback.
        for (var trait : controller.self().getTraits()) {
            if (trait instanceof StructureModifyTrait structureModify) structureModify.restore();
        }
    }

    @Redirect(method = "autoBuild",
              at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/machine/feature/multiblock/IMultiController;onStructureInvalid()V"))
    private void gtladditions$keepActiveTiers(IMultiController controller) {
        // Replace mode invalidates a formed controller first; a tiered one would put back every active tier.
        if (!(controller instanceof IStructureTierMachine)) controller.onStructureInvalid();
    }
}