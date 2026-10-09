package com.gtladd.gtladditions.mixin.buildinggadgets2;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.direwolf20.buildinggadgets2.common.items.GadgetCutPaste;
import com.gtladd.gtladditions.integration.buildinggadgets2.BuildingGadgetsWirelessEnergySupport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GadgetCutPaste.class)
public class GadgetCutPasteMixin {

    @ModifyConstant(method = "cutAndStore",
                    constant = @Constant(intValue = 100000),
                    remap = false,
                    require = 1)
    private int gtladditions$cutLimit(int original) {
        return 1000000;
    }

    @WrapOperation(method = "cutAndStore",
                   at = @At(value = "INVOKE",
                            target = "Lcom/direwolf20/buildinggadgets2/util/BuildingUtils;hasEnoughEnergy(Lnet/minecraft/world/item/ItemStack;I)Z"),
                   remap = false,
                   require = 1)
    private boolean gtladditions$checkWirelessCut(ItemStack gadget, int cost, Operation<Boolean> original,
                                                  @Local(name = "size") long size) {
        if (BuildingGadgetsWirelessEnergySupport.isBound(gadget)) {
            return BuildingGadgetsWirelessEnergySupport.canAffordCut(gadget, size);
        }
        return original.call(gadget, cost);
    }

    @WrapOperation(method = "cutAndStore",
                   at = {
                           @At(value = "INVOKE",
                               target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V",
                               ordinal = 1),
                           @At(value = "INVOKE",
                               target = "Lnet/minecraft/world/entity/player/Player;m_5661_(Lnet/minecraft/network/chat/Component;Z)V",
                               ordinal = 1)
                   },
                   remap = false,
                   require = 1)
    private void gtladditions$wirelessCutFailure(Player player, Component chatComponent, boolean actionBar,
                                                 Operation<Void> original,
                                                 @Local(name = "heldItem") ItemStack heldItem,
                                                 @Local(name = "size") long size) {
        if (BuildingGadgetsWirelessEnergySupport.isBound(heldItem)) {
            chatComponent = BuildingGadgetsWirelessEnergySupport.cutFailureMessage(heldItem, size);
        }
        original.call(player, chatComponent, actionBar);
    }
}