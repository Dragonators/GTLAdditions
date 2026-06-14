package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine;
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachineBase;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import net.minecraft.world.item.ItemStack;

import appeng.api.crafting.IPatternDetails;
import com.gtladd.gtladditions.common.machine.multiblock.part.MESuperPatternBufferPartMachine;
import com.gtladd.gtladditions.integration.ae2.MEBufferPatternHelperExtensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MEPatternBufferPartMachine.class)
public abstract class MEPatternBufferPartMachineMixin extends MEPatternBufferPartMachineBase {

    @Shadow(remap = false)
    @Final
    protected InternalSlot[] internalInventory;

    @Shadow(remap = false)
    protected boolean keepByProduct;

    protected MEPatternBufferPartMachineMixin(IMachineBlockEntity holder, IO io) {
        super(holder, io);
    }

    /**
     * @author Dragons
     * @reason Allows GTLAdditions' ME Super Pattern Buffer to expose Forge of the Antichrist-adjusted AE patterns.
     */
    @SuppressWarnings("ConstantValue")
    @Overwrite(remap = false)
    private IPatternDetails getRealPattern(int index, ItemStack stack) {
        if (stack.isEmpty()) return null;

        var slot = internalInventory[index];
        if ((Object) this instanceof MESuperPatternBufferPartMachine superBuffer && superBuffer.isFOAModeEnabled()) {
            return MEBufferPatternHelperExtensions.processForgeOfTheAntichristPattern(
                    realPatternHelper,
                    stack,
                    slot.getCacheManager()::setCircuitCache,
                    getLevel(),
                    keepByProduct,
                    superBuffer.getFOAPatternOutputMultiplier());
        }

        return realPatternHelper.processPatternWithCircuit(
                stack,
                slot.getCacheManager()::setCircuitCache,
                getLevel(),
                keepByProduct);
    }
}