package com.gtladd.gtladditions.mixin.gtlcore.pattern;

import org.gtlcore.gtlcore.api.pattern.AdvancedBlockPattern;
import org.gtlcore.gtlcore.common.item.UltimateTerminalBehavior;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;

import net.minecraft.world.entity.player.Player;

import com.gtladd.gtladditions.common.machine.trait.StructureModifyTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AdvancedBlockPattern.class, remap = false)
public abstract class AdvancedBlockPatternMixin {

    @ModifyVariable(method = "autoBuild", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private MultiblockState gtladditions$useBuildState(MultiblockState registeredState) {
        // Core initializes this scratch state before testing its predicates.
        return new MultiblockState(registeredState.world, registeredState.controllerPos);
    }

    @Inject(method = "dismantleMultiblock", at = @At("HEAD"), require = 1)
    private void gtladditions$restoreBeforeDismantle(IMultiController controller, Player player,
                                                     UltimateTerminalBehavior.AutoBuildSetting settings, CallbackInfo ci) {
        // The terminal skips real air. Restore before its scan, not from a later block-update callback.
        for (var trait : controller.self().getTraits()) {
            if (trait instanceof StructureModifyTrait structureModify) structureModify.restore();
        }
    }

    @Redirect(method = "dismantleMultiblock",
              at = @At(value = "INVOKE",
                       target = "Lcom/gregtechceu/gtceu/api/machine/feature/multiblock/IMultiController;getMultiblockState()Lcom/gregtechceu/gtceu/api/pattern/MultiblockState;"),
              require = 0,
              expect = 0)
    private MultiblockState gtladditions$useDismantleState(IMultiController controller) {
        // A fixed Core constructs its own probe and no longer has this invocation.
        return new MultiblockState(controller.self().getLevel(), controller.self().getPos());
    }
}