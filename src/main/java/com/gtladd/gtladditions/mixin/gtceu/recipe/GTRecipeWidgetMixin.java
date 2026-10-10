package com.gtladd.gtladditions.mixin.gtceu.recipe;

import org.gtlcore.gtlcore.utils.NumberUtils;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.integration.GTRecipeWidget;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.gtladd.gtladditions.utils.CommonUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.gtlcore.gtlcore.utils.TextUtil.GTL_CORE$VC;

@Mixin(value = GTRecipeWidget.class, priority = 2000)
public abstract class GTRecipeWidgetMixin {

    @Inject(method = "getRecipeParaText", at = @At("HEAD"), cancellable = true, remap = false)
    private static void gtladditions$recipeEnergy(GTRecipe recipe, int duration, long inputEUt, long outputEUt,
                                                  CallbackInfoReturnable<List<Component>> cir) {
        if (recipe.data.getBoolean("duration_is_total_cwu") && recipe.tickInputs.containsKey(CWURecipeCapability.CAP)) return;

        List<Component> texts = new ArrayList<>();
        if (!recipe.data.getBoolean("hide_duration")) {
            texts.add(Component.translatable("gtceu.recipe.duration", FormattingUtil.formatNumbers(duration / 20f)));
        }
        var EUt = inputEUt;
        boolean isOutput = false;
        if (EUt == 0) {
            EUt = outputEUt;
            isOutput = true;
        }
        if (EUt > 0) {
            BigInteger euTotal = BigInteger.valueOf(EUt).multiply(BigInteger.valueOf(duration));
            texts.add(Component.translatable("gtceu.recipe.total", NumberUtils.formatDouble(euTotal.doubleValue())));
            long absEUt = Math.abs(EUt);
            var tier = GTUtil.getTierByVoltage(absEUt);
            var component = Component.translatable(!isOutput ? "gtceu.recipe.eu" : "gtceu.recipe.eu_inverted",
                    NumberUtils.formatLong(EUt));
            if (!isOutput) component.append(
                    Component.literal(" (").withStyle(ChatFormatting.GREEN)
                            .append(Component
                                    .literal(CommonUtils.formatDouble((double) absEUt / GTValues.V[tier]) + "A")
                                    .withStyle(style -> style.withColor(GTL_CORE$VC[tier])))
                            .append(Component.literal(")").withStyle(ChatFormatting.GREEN)));
            texts.add(component);
        }

        cir.setReturnValue(texts);
    }
}