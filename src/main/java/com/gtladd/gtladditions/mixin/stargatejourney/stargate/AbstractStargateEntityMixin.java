package com.gtladd.gtladditions.mixin.stargatejourney.stargate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.tech.EnergyBlockEntity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractStargateEntity.class)
public abstract class AbstractStargateEntityMixin extends EnergyBlockEntity {

    public AbstractStargateEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean canGenerateEnergy) {
        super(type, pos, state, canGenerateEnergy);
    }

    // ========================================
    // Disable SG Energy
    // ========================================

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        var tempCap = super.getCapability(cap, side);
        if (tempCap.isPresent() && tempCap.resolve().isPresent()) {
            if (cap == ForgeCapabilities.ENERGY)
                return LazyOptional.empty();
        }
        return tempCap;
    }

    @Override
    public long getEnergyStored() {
        return 0;
    }

    @Override
    public long capacity() {
        return 0;
    }

    @Override
    public long maxReceive() {
        return 0;
    }

    @Override
    public long maxExtract() {
        return 0;
    }

    @Override
    protected boolean canReceiveZeroPointEnergy() {
        return false;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        return 0;
    }

    public long extractEnergy(long maxExtract, boolean simulate) {
        return 0;
    }
}
