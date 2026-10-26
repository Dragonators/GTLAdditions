package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.povstalec.sgjourney.common.data.StargateNetwork;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StargateNetwork.class)
public abstract class StargateNetworkMixin {

    @ModifyExpressionValue(method = "createConnection",
                           at = @At(value = "FIELD",
                                    target = "Lnet/povstalec/sgjourney/common/data/StargateNetwork;requireEnergy:Z",
                                    opcode = Opcodes.GETSTATIC),
                           remap = false,
                           require = 1,
                           allow = 1)
    private boolean gtladditions$disableConnectionEnergy(boolean original) {
        return false;
    }
}