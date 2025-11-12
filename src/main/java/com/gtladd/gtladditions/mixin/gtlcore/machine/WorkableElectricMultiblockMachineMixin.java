package com.gtladd.gtladditions.mixin.gtlcore.machine;

import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues;
import org.gtlcore.gtlcore.utils.NumberUtils;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine;
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic;
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler;
import com.gtladd.gtladditions.utils.CommonUtils;
import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Implements(@Interface(
                       iface = IWirelessElectricMultiblockMachine.class,
                       prefix = "gTLAdditions$"))
@Mixin(WorkableElectricMultiblockMachine.class)
public abstract class WorkableElectricMultiblockMachineMixin {

    @Shadow(remap = false)
    protected EnergyContainerList energyContainer;

    @Unique
    @Nullable
    private IWirelessNetworkEnergyHandler gTLAdditions$wirelessNetworkEnergyHandler;

    @Inject(method = "onStructureInvalid", at = @At("TAIL"), remap = false)
    private void onStructureInvalid(CallbackInfo ci) {
        this.gTLAdditions$wirelessNetworkEnergyHandler = null;
    }

    @Inject(method = "onPartUnload", at = @At("TAIL"), remap = false)
    private void onPartUnload(CallbackInfo ci) {
        this.gTLAdditions$wirelessNetworkEnergyHandler = null;
    }

    @Unique
    public void gTLAdditions$setWirelessNetworkEnergyHandler(IWirelessNetworkEnergyHandler trait) {
        this.gTLAdditions$wirelessNetworkEnergyHandler = trait;
    }

    @Unique
    public @Nullable IWirelessNetworkEnergyHandler gTLAdditions$getWirelessNetworkEnergyHandler() {
        return this.gTLAdditions$wirelessNetworkEnergyHandler;
    }

    @Redirect(method = "addDisplayText",
              at = @At(value = "INVOKE",
                       target = "Lcom/gregtechceu/gtceu/api/machine/multiblock/MultiblockDisplayText$Builder;addEnergyUsageLine(Lcom/gregtechceu/gtceu/api/capability/IEnergyContainer;)Lcom/gregtechceu/gtceu/api/machine/multiblock/MultiblockDisplayText$Builder;"),
              remap = false,
              require = 0)
    private MultiblockDisplayText.Builder redirectEnergyUsageLine(
                                                                  MultiblockDisplayText.Builder builder,
                                                                  IEnergyContainer energyFormatted,
                                                                  @Local(argsOnly = true) List<Component> textList) {
        final var realThis = (WorkableElectricMultiblockMachine) (Object) this;
        if (realThis.isFormed() && gTLAdditions$wirelessNetworkEnergyHandler != null && gTLAdditions$wirelessNetworkEnergyHandler.isOnline() &&
                ((realThis.recipeLogic instanceof MutableRecipesLogic<?> mutableRecipesLogic && mutableRecipesLogic.isMultipleRecipeMode()) ||
                        realThis.recipeLogic instanceof MultipleRecipesLogic)) {

            var totalEu = gTLAdditions$wirelessNetworkEnergyHandler.getMaxAvailableEnergy();
            var longEu = NumberUtils.getLongValue(totalEu);
            var energyTier = longEu == Long.MAX_VALUE ? GTValues.MAX_TRUE : NumberUtils.getFakeVoltageTier(longEu);

            textList.add(Component.translatable(
                    "gtceu.multiblock.max_energy_per_tick",
                    CommonUtils.formatBigIntegerFixed(totalEu),
                    Component.literal(NewGTValues.VNF[energyTier]))
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("gtceu.multiblock.max_energy_per_tick_hover")
                                    .withStyle(ChatFormatting.GRAY)))));

            return builder;
        } else {
            return builder.addEnergyUsageLine(energyContainer);
        }
    }
}
