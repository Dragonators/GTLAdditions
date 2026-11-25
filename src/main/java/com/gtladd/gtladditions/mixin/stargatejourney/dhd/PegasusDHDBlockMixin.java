package com.gtladd.gtladditions.mixin.stargatejourney.dhd;

import net.povstalec.sgjourney.common.blocks.dhd.PegasusDHDBlock;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PegasusDHDBlock.class)
public abstract class PegasusDHDBlockMixin {

    @ModifyExpressionValue(method = "use",
                           at = @At(value = "INVOKE",
                                    target = "Lnet/povstalec/sgjourney/common/block_entities/dhd/PegasusDHDEntity;hasPermissions(Lnet/minecraft/world/entity/player/Player;Z)Z",
                                    remap = false),
                           require = 1,
                           allow = 1)
    private boolean gtladditions$openDialingMenu(boolean original) {
        return false;
    }
}
