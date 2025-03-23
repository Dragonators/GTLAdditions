package com.gtladd.gtladditions.mixin.gtceu;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gtladd.gtladditions.common.machine.muiltblock.GTLAddMultiblockDisplayTextBuilder;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.IGravityPartMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Mixin(WorkableElectricMultiblockMachine.class)
public class WorkableElectricMultiblockMachineMixin extends WorkableMultiblockMachine implements IDisplayUIMachine {
    @Shadow(remap = false)
    protected EnergyContainerList energyContainer;
    @Shadow(remap = false)
    protected int tier;
    public WorkableElectricMultiblockMachineMixin(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Inject(method = "addDisplayText",
            at = @At(value = "INVOKE", target = "Lcom/gregtechceu/gtceu/api/machine/multiblock/MultiblockDisplayText;builder(Ljava/util/List;Z)Lcom/gregtechceu/gtceu/api/machine/multiblock/MultiblockDisplayText$Builder;", shift = At.Shift.BEFORE),
            remap = false, cancellable = true)
    public void addDisplayText(List<Component> textList, CallbackInfo ci, @Local(ordinal = 0) int numParallels) {
        int gravity = 50;
        Stream Gravity = this.getParts().stream();
        Objects.requireNonNull(IGravityPartMachine.class);
        Gravity = Gravity.filter(IGravityPartMachine.class::isInstance);
        Objects.requireNonNull(IGravityPartMachine.class);
        Optional<IGravityPartMachine> optional = Gravity.map(IGravityPartMachine.class::cast).findAny();
        if (optional.isPresent()) {
            IGravityPartMachine gravityPartMachine = optional.get();
            gravity = gravityPartMachine.getCurrentGravity();
        }
        GTLAddMultiblockDisplayTextBuilder.builder(textList,  this.isFormed())
                .setWorkingStatus(this.recipeLogic.isWorkingEnabled(), this.recipeLogic.isActive())
                .addEnergyUsageLine(this.energyContainer).addEnergyTierLine(this.tier)
                .addMachineModeLine(this.getRecipeType()).addParallelsLine(numParallels)
                .addMaintenanceTierLines(this.getCleanroom()).addGravityLine(gravity)
                .addWorkingStatusLine().addProgressLine(this.recipeLogic.getProgressPercent());
        this.getDefinition().getAdditionalDisplay().accept(this, textList);
        IDisplayUIMachine.super.addDisplayText(textList);
        ci.cancel();
    }

}
