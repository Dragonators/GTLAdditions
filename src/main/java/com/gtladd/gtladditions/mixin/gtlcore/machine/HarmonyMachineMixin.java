package com.gtladd.gtladditions.mixin.gtlcore.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import dev.architectury.patchedmixin.staticmixin.spongepowered.asm.mixin.Overwrite;
import org.gtlcore.gtlcore.api.machine.multiblock.NoEnergyMultiblockMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.electric.HarmonyMachine;
import org.gtlcore.gtlcore.utils.MachineIO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HarmonyMachine.class)
public class HarmonyMachineMixin extends NoEnergyMultiblockMachine {
    @Shadow(remap = false) private int oc = 0;
    @Shadow(remap = false) private long hydrogen = 0L;
    @Shadow(remap = false) private long helium = 0L;
    public HarmonyMachineMixin(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }
    @Overwrite(remap = false)
    protected void StartupUpdate() {
        if (this.getOffsetTimer() % 20L == 0L) {
            this.oc = 0;
            if (this.hydrogen < 10000000000L && MachineIO.inputFluid(this, GTMaterials.Hydrogen.getFluid(100000000L))) this.hydrogen += 100000000L;
            if (this.helium < 10000000000L && MachineIO.inputFluid(this, GTMaterials.Helium.getFluid(100000000L))) this.helium += 100000000L;
            if (MachineIO.notConsumableCircuit(this, 4)) this.oc = 4;
            if (MachineIO.notConsumableCircuit(this, 3)) this.oc = 3;
            if (MachineIO.notConsumableCircuit(this, 2)) this.oc = 2;
            if (MachineIO.notConsumableCircuit(this, 1)) this.oc = 1;
        }
    }
}
