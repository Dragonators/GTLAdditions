package com.gtladd.gtladditions.common.machine.hatch;

import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.util.Mth;

import com.gtladd.gtladditions.api.gui.widget.SafeIntInputWidget;
import org.jetbrains.annotations.NotNull;

public class SuperParallelHatchPartMachine extends MultiblockPartMachine implements IFancyUIMachine, IParallelHatch {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER;
    private static final int maxParallel = Integer.MAX_VALUE;
    private static final int MIN_PARALLEL = 1;
    @Persisted
    private int currentParallel = maxParallel;

    public SuperParallelHatchPartMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    public void setCurrentParallel(int parallelAmount) {
        this.currentParallel = Mth.clamp(parallelAmount, 1, maxParallel);

        for (IMultiController controller : this.getControllers()) {
            if (controller instanceof IRecipeLogicMachine rlm) {
                rlm.getRecipeLogic().markLastRecipeDirty();
            }
        }
    }

    public Widget createUIWidget() {
        WidgetGroup parallelAmountGroup = new WidgetGroup(0, 0, 100, 20);
        parallelAmountGroup.addWidget((new SafeIntInputWidget(this::getCurrentParallel, this::setCurrentParallel)).setMin(1).setMax(maxParallel));
        return parallelAmountGroup;
    }

    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public boolean canShared() {
        return true;
    }

    public int getCurrentParallel() {
        return this.currentParallel;
    }

    static {
        MANAGED_FIELD_HOLDER = new ManagedFieldHolder(SuperParallelHatchPartMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);
    }
}
