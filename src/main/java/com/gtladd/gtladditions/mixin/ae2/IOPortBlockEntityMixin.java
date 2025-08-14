package com.gtladd.gtladditions.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.config.OperationMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.storage.cells.StorageCell;
import appeng.blockentity.storage.IOPortBlockEntity;
import appeng.util.ConfigManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IOPortBlockEntity.class)
public abstract class IOPortBlockEntityMixin {

    @Shadow(remap = false)
    @Final
    private ConfigManager manager;

    @Shadow(remap = false)
    @Final
    private IActionSource mySrc;

    private static final int MAX_TRANSFER_LOOPS = 8192;

    /**
     * @author Dragons
     * @reason 防止单tick循环过多卡死
     */
    @Overwrite(remap = false)
    private long transferContents(IGrid grid, StorageCell cellInv, long itemsToMove) {
        var networkInv = grid.getStorageService().getInventory();

        KeyCounter srcList;
        MEStorage src, destination;
        if (this.manager.getSetting(Settings.OPERATION_MODE) == OperationMode.EMPTY) {
            src = cellInv;
            srcList = cellInv.getAvailableStacks();
            destination = networkInv;
        } else {
            src = networkInv;
            srcList = grid.getStorageService().getCachedInventory();
            destination = cellInv;
        }

        var energy = grid.getEnergyService();
        boolean didStuff;
        int loopBudget = MAX_TRANSFER_LOOPS;

        do {
            if (loopBudget-- <= 0) {
                return itemsToMove;
            }

            didStuff = false;

            for (var srcEntry : srcList) {
                var totalStackSize = srcEntry.getLongValue();
                if (totalStackSize > 0) {
                    var what = srcEntry.getKey();
                    var possible = destination.insert(what, totalStackSize, Actionable.SIMULATE, this.mySrc);

                    if (possible > 0) {
                        possible = Math.min(possible, itemsToMove * what.getAmountPerOperation());

                        possible = src.extract(what, possible, Actionable.MODULATE, this.mySrc);
                        if (possible > 0) {
                            var inserted = StorageHelper.poweredInsert(energy, destination, what, possible, this.mySrc);

                            if (inserted < possible) {
                                src.insert(what, possible - inserted, Actionable.MODULATE, this.mySrc);
                            }

                            if (inserted > 0) {
                                itemsToMove -= Math.max(1, inserted / what.getAmountPerOperation());
                                didStuff = true;
                            }

                            break;
                        }
                    }
                }
            }
        } while (itemsToMove > 0 && didStuff);

        return itemsToMove;
    }
}
