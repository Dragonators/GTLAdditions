package com.gtladd.gtladditions.mixin.buildinggadgets2;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.direwolf20.buildinggadgets2.common.items.BaseGadget;
import com.gtladd.gtladditions.integration.buildinggadgets2.BuildingGadgetsWirelessEnergySupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BaseGadget.class)
public class BaseGadgetMixin {

    // Named and production SRG names of the Minecraft Item override in the fixed 1.20.1 artifact.
    @Inject(method = { "isBarVisible", "m_142522_" }, at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void gtladditions$hideBatteryBar(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (BuildingGadgetsWirelessEnergySupport.isBound(stack)) cir.setReturnValue(false);
    }

    @Inject(method = { "appendHoverText", "m_7373_" }, at = @At("TAIL"), remap = false, require = 1)
    private void gtladditions$wirelessTooltip(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag,
                                              CallbackInfo ci) {
        if (!BuildingGadgetsWirelessEnergySupport.isBound(stack)) return;
        tooltip.removeIf(component -> component.getContents() instanceof TranslatableContents contents &&
                contents.getKey().equals("buildinggadgets2.tooltips.energy"));
        tooltip.add(Component.translatable("gtladditions.gadget.wireless.source",
                BuildingGadgetsWirelessEnergySupport.INSTANCE.ownerName(stack)).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("gtladditions.gadget.wireless.toggle").withStyle(ChatFormatting.GRAY));
    }
}