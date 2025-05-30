package com.gtladd.gtladditions.mixin.gtceu.machine;

import org.gtlcore.gtlcore.common.machine.multiblock.part.maintenance.IGravityPartMachine;

import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;

import net.minecraft.network.chat.Component;

import com.gtladd.gtladditions.common.machine.muiltblock.GTLAddMultiblockDisplayTextBuilder;
import earth.terrarium.adastra.api.planets.Planet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Objects;

@Mixin(WorkableElectricMultiblockMachine.class)
public abstract class WorkableElectricMultiblockMachineMixin extends WorkableMultiblockMachine {

    @Shadow(remap = false)
    protected EnergyContainerList energyContainer;
    @Shadow(remap = false)
    protected int tier;

    public WorkableElectricMultiblockMachineMixin(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    public void addDisplayText(List<Component> textList) {
        int numParallels = 0;
        int gravity = 50;
        for (IMultiPart part : this.getParts()) {
            if (part instanceof IParallelHatch parallelPart) numParallels = parallelPart.getCurrentParallel();
            else if (part instanceof IGravityPartMachine gravityPart) {
                if (Objects.requireNonNull(this.getLevel()).dimension() == Planet.EARTH_ORBIT) gravity = 0;
                else gravity = gravityPart.getCurrentGravity();
            }
        }
        GTLAddMultiblockDisplayTextBuilder.builder(textList, this.isFormed())
                .setWorkingStatus(this.recipeLogic.isWorkingEnabled(), this.recipeLogic.isActive())
                .addEnergyUsageLine(this.energyContainer).addEnergyTierLine(this.tier).addMachineModeLine(this.getRecipeType())
                .addParallelsLine(numParallels).addMaintenanceTierLines(this.getCleanroom()).addGravityLine(gravity)
                .addWorkingStatusLine().addProgressLine(this.recipeLogic.getProgressPercent());
        this.getDefinition().getAdditionalDisplay().accept(this, textList);
    }
}
