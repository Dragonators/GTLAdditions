package com.gtladd.gtladditions.mixin.gtlcore.integration;

import org.gtlcore.gtlcore.common.item.manipulator.ManipulatorSnapshot;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;

import com.gtladd.gtladditions.common.machine.GTLAddMachines;
import com.gtladd.gtladditions.common.machine.multiblock.part.MESuperPatternBufferPartMachine;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ManipulatorSnapshot.class)
public abstract class ManipulatorProxyMixin {

    @ModifyExpressionValue(method = "capture",
                           at = @At(value = "FIELD",
                                    target = "Lorg/gtlcore/gtlcore/common/data/GTLMachines$GTAEMachines;ME_PATTERN_BUFFER_PROXY:Lcom/gregtechceu/gtceu/api/machine/MachineDefinition;",
                                    opcode = Opcodes.GETSTATIC),
                           remap = false,
                           require = 2,
                           allow = 2)
    private static MachineDefinition gtladditions$copiedProxy(MachineDefinition proxy,
                                                              @Local(name = "holder") MetaMachineBlockEntity holder) {
        if (holder.getMetaMachine() instanceof MESuperPatternBufferPartMachine) {
            return GTLAddMachines.INSTANCE.getME_SUPER_PATTERN_BUFFER_PROXY();
        }
        return proxy;
    }
}