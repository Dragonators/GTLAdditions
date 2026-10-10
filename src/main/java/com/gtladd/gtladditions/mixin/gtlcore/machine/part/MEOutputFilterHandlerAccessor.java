package com.gtladd.gtladditions.mixin.gtlcore.machine.part;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEOutputFilterHandler;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MEOutputFilterHandler.class, remap = false)
public interface MEOutputFilterHandlerAccessor {

    @Invoker("test")
    boolean invokeItemTest(ItemStack stack);

    @Invoker("test")
    boolean invokeFluidTest(FluidStack stack);
}