package com.gtladd.gtladditions.mixin.gtceu.recipe;

import org.gtlcore.gtlcore.integration.gtmt.NewGTValues;
import org.gtlcore.gtlcore.utils.NumberUtils;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.integration.jade.provider.RecipeLogicProvider;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleWirelessRecipesLogic;
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static net.minecraft.ChatFormatting.*;
import static net.minecraft.ChatFormatting.GREEN;
import static net.minecraft.ChatFormatting.RED;
import static org.gtlcore.gtlcore.utils.TextUtil.GTL_CORE$VC;

@Mixin(value = RecipeLogicProvider.class, priority = 1500)
public abstract class RecipeLogicProviderMixin {

    @Inject(method = "write(Lnet/minecraft/nbt/CompoundTag;Lcom/gregtechceu/gtceu/api/machine/trait/RecipeLogic;)V", at = @At("HEAD"), remap = false)
    protected void write(CompoundTag data, RecipeLogic capability, CallbackInfo ci) {
        if (capability instanceof GTLAddMultipleWirelessRecipesLogic) {
            if (capability.getLastRecipe() instanceof IWirelessGTRecipe recipe) {
                var bigIntInput = recipe.getWirelessEuTickInputs();
                if (bigIntInput != null && bigIntInput.signum() != 0) {
                    data.putByteArray("wirelessTickInputs", bigIntInput.toByteArray());
                }
            }
        }
    }

    /**
     * @author Dragons
     * @reason 提供无线跨配方机器的Tooltip
     */
    @Overwrite(remap = false)
    protected void addTooltip(CompoundTag capData, ITooltip tooltip, Player player, BlockAccessor block,
                              BlockEntity blockEntity, IPluginConfig config) {
        if (capData.getBoolean("Working")) {
            var recipeInfo = capData.getCompound("Recipe");
            if (!recipeInfo.isEmpty()) {
                var eut = recipeInfo.getLong("EUt");

                if (eut != 0) {
                    var isInput = recipeInfo.getBoolean("isInput");
                    long absEUt = Math.abs(eut);

                    // Default behavior, if this TE is not a steam machine (or somehow not instanceof
                    // IGregTechTileEntity...)
                    var tier = GTUtil.getTierByVoltage(absEUt);
                    Component text = Component.literal(NumberUtils.formatLong(absEUt)).withStyle(RED)
                            .append(Component.literal(" EU/t").withStyle(RESET)
                                    .append(Component.literal(" (").withStyle(GREEN)
                                            .append(Component
                                                    .translatable("gtceu.top.electricity",
                                                            FormattingUtil.formatNumber2Places(absEUt / ((float) GTValues.V[tier])),
                                                            GTValues.VNF[tier])
                                                    .withStyle(style -> style.withColor(GTL_CORE$VC[tier])))
                                            .append(Component.literal(")").withStyle(GREEN))));

                    if (eut > 0) {
                        if (isInput) {
                            tooltip.add(Component.translatable("gtceu.top.energy_consumption").append(" ").append(text));
                        } else {
                            tooltip.add(Component.translatable("gtceu.top.energy_production").append(" ").append(text));
                        }
                    }
                } else if (capData.contains("wirelessTickInputs", Tag.TAG_BYTE_ARRAY)) {
                    BigInteger wirelessEut = new BigInteger(capData.getByteArray("wirelessTickInputs"));
                    BigInteger abs = wirelessEut.abs();
                    long longEu = NumberUtils.getLongValue(abs);
                    var tier = longEu == Long.MAX_VALUE ? GTValues.MAX_TRUE : NumberUtils.getFakeVoltageTier(longEu);
                    Component text = Component.literal(NumberUtils.formatDouble(abs.doubleValue())).withStyle(RED)
                            .append(Component.literal(" EU/t").withStyle(RESET)
                                    .append(Component.literal(" (").withStyle(GREEN)
                                            .append(Component
                                                    .translatable("gtceu.top.electricity",
                                                            FormattingUtil.DECIMAL_FORMAT_2F.format(new BigDecimal(abs).divide(BigDecimal.valueOf(GTValues.VEX[tier]), 3, RoundingMode.DOWN).doubleValue()),
                                                            NewGTValues.VNF[tier])
                                                    .withStyle(style -> style.withColor(GTL_CORE$VC[Math.min(tier, 14)])))
                                            .append(Component.literal(")").withStyle(GREEN))));

                    if (wirelessEut.signum() < 0) {
                        tooltip.add(Component.translatable("gtceu.top.energy_consumption").append(" ").append(text));
                    } else {
                        tooltip.add(Component.translatable("gtceu.top.energy_production").append(" ").append(text));
                    }
                }
            }
            String reason = capData.getString("work_reason");
            if (reason.isEmpty()) return;
            tooltip.add(Component.translatable("gtceu.recipe.fail.reason", reason).withStyle(RED));
        } else {
            String reason = capData.getString("reason");
            if (reason.isEmpty()) return;
            tooltip.add(Component.translatable("gtceu.recipe.fail.reason", reason).withStyle(RED));
        }
    }
}
