package com.gtladd.gtladditions.mixin.gtceu.common.machine;

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.ResearchStationMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ObjectHolderMachine;

import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.utils.ObjectHolderSlotRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.gregtechceu.gtceu.common.machine.multiblock.part.ObjectHolderMachine$ObjectHolderHandler", remap = false)
public class ObjectHolderHandlerMixin {

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtladditions$blockExtractWhileResearching(int slot, int amount, boolean simulate,
                                                           CallbackInfoReturnable<ItemStack> cir) {
        ObjectHolderMachine holder = (ObjectHolderMachine) ((NotifiableItemStackHandler) (Object) this).getMachine();
        boolean researching = holder.getControllers().stream().anyMatch(controller -> controller instanceof ResearchStationMachine station &&
                ((WorkableElectricMultiblockMachine) station).getRecipeLogic().isWorking());
        if (researching) cir.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtladditions$validateResearchSlot(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(ObjectHolderSlotRules.isItemValid(slot, stack));
    }
}