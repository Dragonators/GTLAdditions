package com.gtladd.gtladditions.mixin.gtceu.api.recipe;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import com.gtladd.gtladditions.common.machine.multiblock.controller.module.ForgeOfTheAntichristModuleBase;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MultiblockState.class, priority = 2000)
public abstract class MultiblockStateMixin {

    // Target Core's overwrite only inside its formed, non-controller, non-ActiveBlock part branch.
    @Inject(method = "onBlockStateChanged",
            at = @At(value = "INVOKE",
                     target = "Lcom/gregtechceu/gtceu/api/machine/IMachineBlockEntity;getMetaMachine()Lcom/gregtechceu/gtceu/api/machine/MetaMachine;"),
            cancellable = true,
            remap = false,
            require = 1)
    private void gtladditions$ignoreFoaModuleNotification(BlockPos pos, BlockState state, CallbackInfo ci,
                                                          @Local IMachineBlockEntity machineBlockEntity) {
        if (machineBlockEntity.getMetaMachine() instanceof ForgeOfTheAntichristModuleBase) ci.cancel();
    }
}