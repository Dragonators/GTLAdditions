package com.gtladd.gtladditions.mixin.gtceu.common.machine;

import com.gregtechceu.gtceu.api.capability.IObjectHolder;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.research.ResearchStationMachine;

import net.minecraft.world.item.ItemStack;

import com.gtladd.gtladditions.utils.CloudNetworkManager;
import com.hepdd.gtmthings.api.capability.IBindable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "com.gregtechceu.gtceu.common.machine.multiblock.electric.research.ResearchStationMachine$ResearchStationRecipeLogic")
public abstract class ResearchStationRecipeLogicMixin {

    @Shadow(remap = false)
    public abstract ResearchStationMachine getMachine();

    @Inject(method = "onRecipeFinish", at = @At("TAIL"), require = 1, remap = false)
    private void gtladditions$uploadResearchData(CallbackInfo ci) {
        ResearchStationMachine machine = getMachine();
        if (!(machine instanceof IBindable bindable)) return;

        UUID teamId = bindable.getUUID();
        if (teamId == null) return;

        IObjectHolder holder = machine.getObjectHolder();
        ItemStack dataStack = holder.getDataItem(false);
        if (dataStack.isEmpty()) return;

        if (CloudNetworkManager.INSTANCE.tryUploadResearchData(dataStack, teamId)) {
            holder.setDataItem(ItemStack.EMPTY);
        }
    }
}