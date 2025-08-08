package com.gtladd.gtladditions.mixin.extendedAE;

import com.glodblock.github.extendedae.common.tileentities.TileExIOPort;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(TileExIOPort.class)
public abstract class TileExIOPortMixin {

    @ModifyConstant(method = "tickingRequest", constant = @Constant(longValue = 2048L), remap = false)
    private long replaceItemsToMove(long original) {
        return Integer.MAX_VALUE * 65536L;
    }
}
