package com.gtladd.gtladditions.mixin.gtceu.machine.part;

import com.gregtechceu.gtceu.api.capability.ICleanroomReceiver;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.AutoMaintenanceHatchPartMachine;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import static org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.ICleaningRoom.DUMMY_CLEANROOM;

@Mixin(AutoMaintenanceHatchPartMachine.class)
public class AutoMaintenanceHatchPartMachineMixin extends TieredPartMachine {

    public AutoMaintenanceHatchPartMachineMixin(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
    }

    @Unique
    public void addedToController(@NotNull IMultiController controller) {
        super.addedToController(controller);
        if (controller instanceof ICleanroomReceiver receiver) receiver.setCleanroom(null);
    }

    @Unique
    public void removedFromController(@NotNull IMultiController controller) {
        super.removedFromController(controller);
        if (controller instanceof ICleanroomReceiver receiver) if (receiver.getCleanroom() == DUMMY_CLEANROOM) receiver.setCleanroom(null);
    }
}
