package com.gtladd.gtladditions.mixin.gtceu.integration;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;
import org.gtlcore.gtlcore.common.machine.trait.MultipleRecipesLogic;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.integration.jade.provider.ParallelProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import com.google.common.primitives.Ints;
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine;
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic;
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.ForgeOfTheAntichrist;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.HelioFusionExoticizer;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.MacroAtomicResonantFragmentStripper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Optional;

@Mixin(ParallelProvider.class)
public abstract class ParallelProviderMixin {

    /**
     * @author Draongs
     * @reason Support Add Machine
     */
    @Overwrite(remap = false)
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getServerData().contains("parallel")) {
            int parallel = blockAccessor.getServerData().getInt("parallel");
            if (parallel > 0) {
                iTooltip.add(Component.translatable(
                        "gtceu.multiblock.parallel",
                        Component.literal(parallel + "").withStyle(ChatFormatting.DARK_PURPLE)));
            }
            if (blockAccessor.getServerData().contains("threads")) {
                int threads = blockAccessor.getServerData().getInt("threads");
                if (threads > 0) {
                    iTooltip.add(Component.translatable(
                            "gtladditions.multiblock.threads",
                            Component.literal(threads + "").withStyle(ChatFormatting.GOLD)));
                }
            }
        }
    }

    /**
     * @author Draongs
     * @reason Support Add Machine
     */
    @Overwrite(remap = false)
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof MetaMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof IParallelHatch parallelHatch) {
                compoundTag.putInt("parallel", parallelHatch.getCurrentParallel());
            } else if (blockEntity.getMetaMachine() instanceof WorkableMultiblockMachine workableElectricMultiblockMachine && workableElectricMultiblockMachine.isFormed()) {
                if (workableElectricMultiblockMachine instanceof GTLAddWorkableElectricMultipleRecipesMachine addMachine) {
                    if (!(workableElectricMultiblockMachine instanceof ForgeOfTheAntichrist) && !(workableElectricMultiblockMachine instanceof HelioFusionExoticizer)) {
                        compoundTag.putInt("parallel", addMachine.getMaxParallel());
                        if (!(workableElectricMultiblockMachine instanceof MacroAtomicResonantFragmentStripper))
                            compoundTag.putInt("threads", addMachine.getRecipeLogic().getMultipleThreads());
                    }
                } else {
                    var logic = workableElectricMultiblockMachine.getRecipeLogic();
                    if (logic instanceof MultipleRecipesLogic) {
                        compoundTag.putInt("parallel", ((ParallelMachine) workableElectricMultiblockMachine).getMaxParallel());
                        compoundTag.putInt("threads", Ints.saturatedCast(64L + ((IThreadModifierMachine) workableElectricMultiblockMachine).getAdditionalThread()));
                    } else if (logic instanceof MutableRecipesLogic<?> mutableRecipesLogic) {
                        compoundTag.putInt("parallel", ((ParallelMachine) workableElectricMultiblockMachine).getMaxParallel());
                        if (mutableRecipesLogic.getMultipleThreads() > 1) compoundTag.putInt("threads", mutableRecipesLogic.getMultipleThreads());
                    } else if (blockEntity.getMetaMachine() instanceof ParallelMachine controller) {
                        compoundTag.putInt("parallel", controller.getMaxParallel());
                    } else {
                        Optional<IParallelHatch> parallelHatch = Optional.ofNullable(((IRecipeCapabilityMachine) workableElectricMultiblockMachine).getParallelHatch());
                        parallelHatch.ifPresent(iParallelHatch -> compoundTag.putInt("parallel", iParallelHatch.getCurrentParallel()));
                    }
                }
            }
        }
    }
}
