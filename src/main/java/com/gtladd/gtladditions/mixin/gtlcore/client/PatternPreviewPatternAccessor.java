package com.gtladd.gtladditions.mixin.gtlcore.client;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;

import appeng.api.stacks.AEItemKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;

@Mixin(targets = "org.gtlcore.gtlcore.api.gui.PatternPreviewWidget$MBPattern", remap = false)
public interface PatternPreviewPatternAccessor {

    @Accessor("controllerBase")
    IMultiController getControllerBase();

    @Accessor("materials")
    Map<BlockPos, AEItemKey> getMaterials();

    @Accessor("hasModule")
    boolean getHasModule();

    @Accessor("blockMap")
    Map<BlockPos, BlockInfo> getBlockMap();

    @Accessor("moduleOnlyBlocks")
    Set<BlockPos> getModuleOnlyBlocks();
}