package com.gtladd.gtladditions.mixin.ae2;

import net.minecraftforge.fluids.FluidStack;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jei.FluidIngredientConverter;
import com.gtladd.gtladditions.integration.xei.LongFluidStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FluidIngredientConverter.class)
public abstract class FluidIngredientConverterMixin {

    /**
     * @author Dragons
     * @reason Support Long
     */
    @Overwrite(remap = false)
    public @Nullable FluidStack getIngredientFromStack(GenericStack stack) {
        AEKey var3 = stack.what();
        if (var3 instanceof AEFluidKey fluidKey) {
            return new LongFluidStack(fluidKey.getFluid(), stack.amount(), fluidKey.getTag());
        } else {
            return null;
        }
    }

    /**
     * @author Dragons
     * @reason Support Long
     */
    @Overwrite(remap = false)
    public @Nullable GenericStack getStackFromIngredient(FluidStack ingredient) {
        AEFluidKey key = AEFluidKey.of(ingredient);
        return key == null ? null : new GenericStack(key, ingredient instanceof LongFluidStack longFluidStack ? longFluidStack.getLongAmount() : ingredient.getAmount());
    }
}
