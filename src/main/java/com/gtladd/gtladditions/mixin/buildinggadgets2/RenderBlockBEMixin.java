package com.gtladd.gtladditions.mixin.buildinggadgets2;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.direwolf20.buildinggadgets2.common.blockentities.RenderBlockBE;
import com.gtladd.gtladditions.integration.buildinggadgets2.BuildingGadgetsMachineSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderBlockBE.class)
public abstract class RenderBlockBEMixin extends BlockEntity {

    @Shadow(remap = false)
    public CompoundTag blockEntityData;

    @Unique
    private boolean gtladditions$finishedPlacement;

    protected RenderBlockBEMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "setRealBlock", at = @At("RETURN"), remap = false, require = 1)
    private void gtladditions$finishPlacement(BlockState realBlock, CallbackInfo ci) {
        if (!gtladditions$finishedPlacement && blockEntityData != null && level instanceof ServerLevel serverLevel) {
            gtladditions$finishedPlacement = true;
            BuildingGadgetsMachineSupport.finishPlacement(serverLevel, getBlockPos(), blockEntityData);
        }
    }
}