package com.gtladd.gtladditions.mixin.ldlib;

import org.gtlcore.gtlcore.utils.Registries;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FluidStack.class)
public abstract class FluidStackMixin {

    @Shadow(remap = false)
    @Final
    private static FluidStack EMPTY;

    /**
     * @author Dragons
     * @reason Fuck LDLib
     */
    @Overwrite(remap = false)
    public static FluidStack readFromBuf(FriendlyByteBuf buf) {
        Fluid fluid = Registries.getFluid(buf.readUtf());
        long amount = buf.readVarLong(); // ????????
        CompoundTag tag = buf.readNbt();
        if (fluid == Fluids.EMPTY) return EMPTY;
        return FluidStack.create(fluid, amount, tag);
    }
}
