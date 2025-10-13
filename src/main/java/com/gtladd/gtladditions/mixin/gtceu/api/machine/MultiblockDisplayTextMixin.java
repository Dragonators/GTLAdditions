package com.gtladd.gtladditions.mixin.gtceu.api.machine;

import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues;
import org.gtlcore.gtlcore.utils.NumberUtils;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic;
import com.gtladd.gtladditions.common.machine.trait.NetworkEnergyContainer;
import com.gtladd.gtladditions.mixin.gtceu.api.misc.EnergyContainerListAccessor;
import com.hepdd.gtmthings.api.misc.WirelessEnergyManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(MultiblockDisplayText.Builder.class)
public abstract class MultiblockDisplayTextMixin {

    @Shadow(remap = false)
    @Final
    private List<Component> textList;

    @Shadow(remap = false)
    @Final
    private boolean isStructureFormed;

    @Inject(method = "addEnergyUsageLine", at = @At("HEAD"), cancellable = true, remap = false)
    private void addEnergyUsageLine(IEnergyContainer energyContainer, CallbackInfoReturnable<MultiblockDisplayText.Builder> cir) {
        if (isStructureFormed && energyContainer instanceof EnergyContainerListAccessor energyContainerListAccessor) {
            for (IEnergyContainer iEnergyContainer : energyContainerListAccessor.getEnergyContainerList()) {
                if (iEnergyContainer instanceof NetworkEnergyContainer networkEnergyContainer) {
                    var uuid = networkEnergyContainer.getMachine().getUUID();
                    if (uuid == null) continue;
                    var controllers = networkEnergyContainer.getMachine().getControllers();
                    if (!controllers.isEmpty() && controllers.get(0) instanceof IRecipeLogicMachine machine && ((machine.getRecipeLogic() instanceof MutableRecipesLogic<?> mutableRecipesLogic && mutableRecipesLogic.isMultipleRecipeMode()) || machine.getRecipeLogic() instanceof MultipleRecipesLogic)) {
                        var totalEu = WirelessEnergyManager.getUserEU(uuid);
                        var longEu = NumberUtils.getLongValue(totalEu);
                        var energyTier = longEu == Long.MAX_VALUE ? GTValues.MAX_TRUE : NumberUtils.getFakeVoltageTier(longEu);

                        // Max energy per tick with hover
                        textList.add(Component.translatable(
                                "gtceu.multiblock.max_energy_per_tick",
                                String.format("%.8e", totalEu.doubleValue()),
                                Component.literal(NewGTValues.VNF[energyTier]))
                                .withStyle(ChatFormatting.GRAY)
                                .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("gtceu.multiblock.max_energy_per_tick_hover")
                                                .withStyle(ChatFormatting.GRAY)))));
                        cir.setReturnValue((MultiblockDisplayText.Builder) (Object) this);
                    } else {
                        return;
                    }
                }
            }
        }
    }
}
