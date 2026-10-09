package com.gtladd.gtladditions.mixin.buildinggadgets2;

import net.minecraft.world.item.ItemStack;

import com.direwolf20.buildinggadgets2.util.BuildingUtils;
import com.gtladd.gtladditions.integration.buildinggadgets2.BuildingGadgetsWirelessEnergySupport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BuildingUtils.class)
public class BuildingUtilsMixin {

    @WrapOperation(method = { "build", "exchange", "removeTickHandler" },
                   at = @At(value = "INVOKE",
                            target = "Lcom/direwolf20/buildinggadgets2/util/BuildingUtils;hasEnoughEnergy(Lnet/minecraft/world/item/ItemStack;)Z"),
                   remap = false,
                   require = 3,
                   expect = 3)
    private static boolean gtladditions$payBeforeQueue(ItemStack gadget, Operation<Boolean> original) {
        if (BuildingGadgetsWirelessEnergySupport.isBound(gadget)) {
            return BuildingGadgetsWirelessEnergySupport.tryPayBlock(gadget);
        }
        return original.call(gadget);
    }

    @WrapOperation(method = { "build", "exchange", "removeTickHandler" },
                   at = @At(value = "INVOKE",
                            target = "Lcom/direwolf20/buildinggadgets2/util/BuildingUtils;useEnergy(Lnet/minecraft/world/item/ItemStack;)V"),
                   remap = false,
                   require = 3,
                   expect = 3)
    private static void gtladditions$skipPrepaidBattery(ItemStack gadget, Operation<Void> original) {
        if (!BuildingGadgetsWirelessEnergySupport.isBound(gadget)) original.call(gadget);
    }
}