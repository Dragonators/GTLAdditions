package com.gtladd.gtladditions.mixin.gtceu.integration;

import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine;
import org.gtlcore.gtlcore.api.machine.trait.IRecipeCapabilityMachine;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.integration.jade.provider.ParallelProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricMultipleRecipesMachine;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.ForgeOfTheAntichrist;
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
            } else if (blockEntity.getMetaMachine() instanceof GTLAddWorkableElectricMultipleRecipesMachine addMachine) {
                if (!(blockEntity.getMetaMachine() instanceof ForgeOfTheAntichrist)) {
                    compoundTag.putInt("parallel", addMachine.getMaxParallel());
                    compoundTag.putInt("threads", addMachine.getRecipeLogic().getMultipleThreads());
                }
            } else if (blockEntity.getMetaMachine() instanceof ParallelMachine controller) {
                compoundTag.putInt("parallel", controller.getMaxParallel());
            } else if (blockEntity.getMetaMachine() instanceof IRecipeCapabilityMachine controller) {
                Optional<IParallelHatch> parallelHatch = Optional.ofNullable(controller.getParallelHatch());
                parallelHatch.ifPresent(iParallelHatch -> compoundTag.putInt("parallel", iParallelHatch.getCurrentParallel()));
            }
        }
    }
}
