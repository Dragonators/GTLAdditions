package com.gtladd.gtladditions.integration.xei

import com.google.common.primitives.Ints
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.material.Fluid
import net.minecraftforge.fluids.FluidStack

class LongFluidStack : FluidStack {
    private var longAmount: Long

    constructor(fluid: Fluid, amount: Long) : super(fluid, Ints.saturatedCast(amount)) {
        this.longAmount = amount
    }

    constructor(fluid: Fluid, amount: Long, nbt: CompoundTag?) : super(fluid, Ints.saturatedCast(amount), nbt) {
        this.longAmount = amount
    }

    constructor(stack: FluidStack, amount: Long) : super(stack, Ints.saturatedCast(amount)) {
        this.longAmount = amount
    }

    fun getLongAmount(): Long = if (isEmpty) 0 else longAmount

    fun setLongAmount(longAmount: Long) {
        this.amount = Ints.saturatedCast(longAmount)
        this.longAmount = longAmount
    }

    override fun copy(): LongFluidStack = LongFluidStack(fluid, longAmount, tag)

    override fun containsFluid(other: FluidStack): Boolean {
        return isFluidEqual(other) && if (other is LongFluidStack) {
            this.longAmount >= other.longAmount
        } else {
            this.longAmount >= other.amount
        }
    }

    override fun isFluidStackIdentical(other: FluidStack): Boolean {
        return isFluidEqual(other) && if (other is LongFluidStack) {
            this.longAmount == other.longAmount
        } else {
            this.longAmount == other.amount.toLong()
        }
    }
}