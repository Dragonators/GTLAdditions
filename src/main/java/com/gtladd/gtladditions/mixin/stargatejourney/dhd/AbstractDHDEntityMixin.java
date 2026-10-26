package com.gtladd.gtladditions.mixin.stargatejourney.dhd;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.tech.EnergyBlockEntity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractDHDEntity.class)
public abstract class AbstractDHDEntityMixin extends EnergyBlockEntity {

    @Shadow(remap = false)
    @Final
    @Mutable
    protected static boolean REQUIRE_ENERGY;

    protected AbstractDHDEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void gtladditions$disableDialingEnergy(CallbackInfo ci) {
        REQUIRE_ENERGY = false;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.ENERGY ? LazyOptional.empty() : super.getCapability(cap, side);
    }

    @Inject(method = "outputEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtladditions$disableEnergyTransfer(Direction direction, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "canReceiveZeroPointEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtladditions$disableZeroPointEnergy(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}