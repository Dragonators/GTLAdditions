package com.gtladd.gtladditions.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.me.storage.ExternalStorageFacade;
import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.config.ConfigHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ExternalStorageFacade.class)
public abstract class ExternalStorageFacadeMixin {

    /**
     * @author Dragons
     * @reason 流体输入上限
     */
    @Overwrite(remap = false)
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        final int max = Integer.MAX_VALUE;
        int maxTimes = ConfigHolder.INSTANCE.performance.externalStorageMaxTimes;
        int times = Math.min(Ints.saturatedCast(amount / max), maxTimes);
        int remainder = (int) (amount % max);
        long insertedTotal = 0;

        for (; times > 0; times--) {
            int insert = this.insertExternal(what, max, mode);
            if (insert == 0) {
                break;
            }
            insertedTotal += insert;
        }

        if (remainder > 0) {
            insertedTotal += this.insertExternal(what, remainder, mode);
        }

        if (insertedTotal > 0 && mode == Actionable.MODULATE) {
            Runnable changeListener = ((ExternalStorageFacadeAccessor) this).getChangeListener();
            if (changeListener != null) {
                changeListener.run();
            }
        }

        return insertedTotal;
    }

    protected abstract int insertExternal(AEKey what, int amount, Actionable mode);
}
