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
import com.gtladd.gtladditions.common.machine.muiltblock.controller.MacroAtomicResonantFragmentStripper;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.SpaceInfinityIntegratedOreProcessor;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.module.ForgeOfTheAntichristModuleBase;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.module.HelioFusionExoticizer;
import com.gtladd.gtladditions.common.machine.muiltblock.controller.module.LightHunterSpaceStationModuleBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Objects;
import java.util.Optional;

import static com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponent;
import static org.gtlcore.gtlcore.common.data.GTLRecipeTypes.ELEMENT_COPYING_RECIPES;

@Mixin(ParallelProvider.class)
public abstract class ParallelProviderMixin {

    @Unique
    private static final long INFINITY_FEATURE = -114514;

    /**
     * @author Draongs
     * @reason Support Add Machine
     */
    @Overwrite(remap = false)
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getServerData().contains("parallel")) {
            long parallel = blockAccessor.getServerData().getLong("parallel");
            if (parallel > 0) {
                iTooltip.add(Component.translatable(
                        "gtceu.multiblock.parallel",
                        Component.literal(parallel + "").withStyle(ChatFormatting.DARK_PURPLE)));
            } else if (parallel == INFINITY_FEATURE) {
                iTooltip.add(Component.translatable(
                        "gtceu.multiblock.parallel",
                        createLanguageRainbowComponent(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel"))));
            }
        }
        if (blockAccessor.getServerData().contains("threads")) {
            int threads = blockAccessor.getServerData().getInt("threads");
            if (threads > 0) {
                iTooltip.add(Component.translatable(
                        "gtladditions.multiblock.threads",
                        Component.literal(String.valueOf(threads)).withStyle(ChatFormatting.GOLD)));
            } else if (threads == INFINITY_FEATURE) {
                iTooltip.add(Component.translatable(
                        "gtladditions.multiblock.threads",
                        createLanguageRainbowComponent(Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel"))));

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
                compoundTag.putLong("parallel", parallelHatch.getCurrentParallel());
            } else if (blockEntity.getMetaMachine() instanceof WorkableMultiblockMachine workableElectricMultiblockMachine && workableElectricMultiblockMachine.isFormed()) {
                if (workableElectricMultiblockMachine instanceof GTLAddWorkableElectricMultipleRecipesMachine addMachine) {
                    if (workableElectricMultiblockMachine instanceof ForgeOfTheAntichrist) {
                        compoundTag.putLong("parallel", INFINITY_FEATURE);
                        compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else if (workableElectricMultiblockMachine instanceof ForgeOfTheAntichristModuleBase) {
                        compoundTag.putLong("parallel", INFINITY_FEATURE);
                        if (!(workableElectricMultiblockMachine instanceof HelioFusionExoticizer))
                            compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else if (workableElectricMultiblockMachine instanceof MacroAtomicResonantFragmentStripper atomic) {
                        compoundTag.putLong("parallel", atomic.getRecipeType() == ELEMENT_COPYING_RECIPES ? INFINITY_FEATURE : atomic.getRealParallel());
                        compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else if (workableElectricMultiblockMachine instanceof SpaceInfinityIntegratedOreProcessor) {
                        compoundTag.putLong("parallel", INFINITY_FEATURE);
                        compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else if (workableElectricMultiblockMachine instanceof LightHunterSpaceStationModuleBase lightHunterSpaceStationModuleBase && lightHunterSpaceStationModuleBase.isConnectedToHost() && Objects.requireNonNull(lightHunterSpaceStationModuleBase.getHost()).unlockParadoxical()) {
                        compoundTag.putLong("parallel", INFINITY_FEATURE);
                        compoundTag.putLong("threads", INFINITY_FEATURE);
                    } else {
                        compoundTag.putLong("parallel", addMachine.getMaxParallel());
                        compoundTag.putLong("threads", addMachine.getRecipeLogic().getMultipleThreads());
                    }
                } else {
                    var logic = workableElectricMultiblockMachine.getRecipeLogic();
                    if (logic instanceof MultipleRecipesLogic) {
                        compoundTag.putLong("parallel", ((ParallelMachine) workableElectricMultiblockMachine).getMaxParallel());
                        compoundTag.putLong("threads", Ints.saturatedCast(64L + ((IThreadModifierMachine) workableElectricMultiblockMachine).getAdditionalThread()));
                    } else if (logic instanceof MutableRecipesLogic<?> mutableRecipesLogic) {
                        compoundTag.putLong("parallel", ((ParallelMachine) workableElectricMultiblockMachine).getMaxParallel());
                        if (mutableRecipesLogic.getMultipleThreads() > 1) compoundTag.putLong("threads", mutableRecipesLogic.getMultipleThreads());
                    } else if (blockEntity.getMetaMachine() instanceof ParallelMachine controller) {
                        compoundTag.putLong("parallel", controller.getMaxParallel());
                    } else {
                        Optional<IParallelHatch> parallelHatch = Optional.ofNullable(((IRecipeCapabilityMachine) workableElectricMultiblockMachine).getParallelHatch());
                        parallelHatch.ifPresent(iParallelHatch -> compoundTag.putLong("parallel", iParallelHatch.getCurrentParallel()));
                    }
                }
            }
        }
    }
}
