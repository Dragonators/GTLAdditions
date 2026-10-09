package com.gtladd.gtladditions.mixin.ae2.client;

import appeng.client.gui.AEBaseScreen;
import com.gtladd.gtladditions.integration.xei.TerminalSearchBoxJeiHandler;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "appeng.integration.modules.jei.GhostIngredientHandler", remap = false)
public abstract class JeiGhostIngredientHandlerMixin {

    @Inject(method = "getTargetsTyped*", at = @At("RETURN"), cancellable = true)
    private <I> void gtladditions$addSearchTargets(AEBaseScreen<?> gui, ITypedIngredient<I> ingredient,
                                                   boolean doStart, CallbackInfoReturnable<List<IGhostIngredientHandler.Target<I>>> cir) {
        var searchTargets = TerminalSearchBoxJeiHandler.INSTANCE.getTargetsTyped(gui, ingredient, doStart);
        if (searchTargets.isEmpty()) return;

        var targets = new ArrayList<>(cir.getReturnValue());
        targets.addAll(searchTargets);
        cir.setReturnValue(targets);
    }
}