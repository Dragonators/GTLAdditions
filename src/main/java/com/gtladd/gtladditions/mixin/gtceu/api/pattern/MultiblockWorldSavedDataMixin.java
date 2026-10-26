package com.gtladd.gtladditions.mixin.gtceu.api.pattern;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(value = MultiblockWorldSavedData.class, priority = 2000)
public abstract class MultiblockWorldSavedDataMixin {

    // Optional once Core itself uses addIfAbsent.
    @Redirect(method = "addAsyncLogic",
              at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CopyOnWriteArrayList;add(Ljava/lang/Object;)Z"),
              remap = false,
              require = 0,
              expect = 0)
    private boolean gtladditions$registerOnce(CopyOnWriteArrayList<IMultiController> controllers, Object controller) {
        return controllers.addIfAbsent((IMultiController) controller);
    }
}