package com.gtladd.gtladditions.mixin.gtceu.client;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;

import net.minecraft.world.phys.AABB;

import com.gtladd.gtladditions.client.render.machine.MachineRenderBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GTCEu culls a machine's renderer by a box of ±viewDistance / 2 around the controller; some renderers draw beyond it.
 */
@Mixin(value = MetaMachineBlockEntity.class, remap = false)
public abstract class MetaMachineBlockEntityMixin {

    @Inject(method = "getRenderBoundingBox", at = @At("HEAD"), cancellable = true)
    private void gtladditions$machineRenderBox(CallbackInfoReturnable<AABB> cir) {
        MetaMachineBlockEntity self = (MetaMachineBlockEntity) (Object) this;
        if (self.getMetaMachine().getDefinition().getRenderer() instanceof MachineRenderBox box) {
            cir.setReturnValue(box.renderBox(self));
        }
    }
}
