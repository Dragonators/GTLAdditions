package com.gtladd.gtladditions.mixin.buildinggadgets2;

import com.direwolf20.buildinggadgets2.util.modes.Copy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Copy.class)
public class CopyMixin {

    @ModifyConstant(method = "collectWorld",
                    constant = @Constant(intValue = 100000),
                    remap = false,
                    require = 1)
    private int gtladditions$copyLimit(int original) {
        return 1000000;
    }
}