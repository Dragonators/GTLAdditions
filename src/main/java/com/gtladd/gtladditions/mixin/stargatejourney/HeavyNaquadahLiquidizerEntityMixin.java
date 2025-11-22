package com.gtladd.gtladditions.mixin.stargatejourney;

import net.povstalec.sgjourney.common.block_entities.tech.HeavyNaquadahLiquidizerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(HeavyNaquadahLiquidizerEntity.class)
public abstract class HeavyNaquadahLiquidizerEntityMixin {

    /**
     * @author Dragons
     * @reason Disable
     */
    @Overwrite(remap = false)
    protected boolean hasMaterial() {
        return false;
    }
}
