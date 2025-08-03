package com.gtladd.gtladditions.mixin.ae2;

import appeng.me.storage.ExternalStorageFacade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExternalStorageFacade.class)
public interface ExternalStorageFacadeAccessor {

    @Accessor(value = "changeListener", remap = false)
    Runnable getChangeListener();
}
