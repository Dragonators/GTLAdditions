package com.gtladd.gtladditions.mixin.gtceu.xei;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtladd.gtladditions.integration.xei.LongFluidStack;
import mezz.jei.forge.platform.FluidHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = FluidHelper.class, priority = 2000)
public abstract class FluidHelperMixin {

    /**
     * @author Dragons
     * @reason Support Long AE Pattern
     */
    @Overwrite(remap = false)
    public FluidStack create(Fluid fluid, long amount) {
        return new LongFluidStack(fluid, amount);
    }

    /**
     * @author Dragons
     * @reason Support Long AE Pattern
     */
    @Overwrite(remap = false)
    public FluidStack create(Fluid fluid, long amount, @Nullable CompoundTag tag) {
        return new LongFluidStack(fluid, amount, tag);
    }

    /**
     * @author Dragons
     * @reason Support Long AE Pattern
     */
    @Overwrite(remap = false)
    public long getAmount(FluidStack ingredient) {
        return ingredient instanceof LongFluidStack longFluidStack ? longFluidStack.getLongAmount() : ingredient.getAmount();
    }

    /**
     * @author Dragons
     * @reason Support Long AE Pattern
     */
    @Overwrite(remap = false)
    public FluidStack copyWithAmount(FluidStack ingredient, long amount) {
        FluidStack copy = ingredient.copy();
        if (copy instanceof LongFluidStack longFluidStack) {
            longFluidStack.setLongAmount(amount);
        } else {
            copy.setAmount(Math.toIntExact(amount));
        }
        return copy;
    }
}
