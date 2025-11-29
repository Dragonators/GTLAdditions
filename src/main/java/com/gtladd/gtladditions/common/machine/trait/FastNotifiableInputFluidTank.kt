package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.trait.ICapabilityTrait
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.utils.FluidStackHashStrategy
import com.gtladd.gtladditions.utils.TransferHelper
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import org.gtlcore.gtlcore.integration.ae2.AEUtils
import org.gtlcore.gtlcore.utils.NumberUtils
import kotlin.math.max
import kotlin.math.min

class FastNotifiableInputFluidTank(machine: MetaMachine) : NotifiableRecipeHandlerTrait<FluidIngredient>(machine),
    ICapabilityTrait, IFluidTransfer, ITagSerializable<CompoundTag>, IContentChangeAware {

    private val fluidInventory: ObjectLinkedOpenCustomHashSet<FluidStack> =
        ObjectLinkedOpenCustomHashSet(FluidStackHashStrategy.comparingAllButAmount())

    val realSize: Int
        get() = fluidInventory.size

    fun getFluidInventory() = fluidInventory

    fun addDisplayText(textList: MutableList<Component?>) {
        for (fluidStack in fluidInventory) {
            textList.add(
                fluidStack.displayName.copy().setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD))
                    .append(
                        Component.literal(
                            if (fluidStack.amount < 1000L) fluidStack.amount
                                .toString() + "mB" else NumberUtils.formatLong(fluidStack.amount / 1000L) + "B"
                        )
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA))
                    )
            )
        }
    }

    fun isEmpty(): Boolean = fluidInventory.isEmpty()

    fun importFromNearby(vararg facings: Direction) {
        val level = machine.level ?: return
        val pos = machine.pos

        for (facing in facings) {
            TransferHelper.importToTarget(
                this,
                machine.getFluidCapFilter(facing),
                level,
                pos.relative(facing),
                facing.opposite
            )
        }
    }

    fun exportToNearby(vararg facings: Direction) {
        if (isEmpty()) return

        val level = machine.level ?: return
        val pos = machine.pos

        for (facing in facings) {
            TransferHelper.exportToTarget(
                this,
                machine.getFluidCapFilter(facing),
                level,
                pos.relative(facing),
                facing.opposite
            )
        }
    }

    override fun getHandlerIO(): IO = IO.IN

    override fun onContentsChanged() {
        notifyListeners()
    }

    override fun handleRecipeInner(
        io: IO,
        recipe: GTRecipe,
        left: MutableList<FluidIngredient>,
        slotName: String?,
        simulate: Boolean
    ): List<FluidIngredient>? {
        if (io != IO.IN) return left.ifEmpty { null }

        var changed = false
        val iter = left.listIterator()
        outer@ while (iter.hasNext()) {
            val fluidIngredient = iter.next()
            if (fluidIngredient.isEmpty) {
                iter.remove()
                continue
            }

            val fluids = fluidIngredient.getStacks()
            if (fluids.isEmpty()) {
                iter.remove()
                continue
            }

            var leftAmount = fluidIngredient.amount

            for (fluidStack in fluids) {
                val innerStack = fluidInventory.get(fluidStack) ?: continue

                val drain = min(leftAmount, innerStack.amount)
                leftAmount -= drain
                if (!simulate) {
                    if (innerStack.amount == drain)
                        fluidInventory.remove(innerStack)
                    else
                        innerStack.amount -= drain
                    changed = true
                }

                if (leftAmount <= 0L) {
                    iter.remove()
                    continue@outer
                }
            }

            fluidIngredient.amount = leftAmount
        }

        if (changed) onContentsChanged()

        return left.ifEmpty { null }
    }

    override fun getContents(): List<Any> = fluidInventory.toList()

    override fun getTotalContentAmount(): Double = fluidInventory.sumOf { it.amount }.toDouble()

    override fun getCapability(): RecipeCapability<FluidIngredient> = FluidRecipeCapability.CAP

    override fun getCapabilityIO(): IO = IO.IN

    override fun getTanks(): Int = max(fluidInventory.size, 1)

    override fun getFluidInTank(tank: Int): FluidStack =
        fluidInventory.elementAtOrNull(tank) ?: FluidStack.empty()

    @Suppress("UnstableApiUsage")
    override fun setFluidInTank(tank: Int, fluidStack: FluidStack) {
        fluidInventory.addOrGet(fluidStack).amount = fluidStack.amount
    }

    override fun getTankCapacity(tank: Int): Long = Long.MAX_VALUE

    override fun isFluidValid(tank: Int, stack: FluidStack): Boolean = true

    override fun fill(resource: FluidStack, simulate: Boolean, notifyChanges: Boolean): Long =
        fill(0, resource, simulate, notifyChanges)

    @Suppress("UnstableApiUsage")
    override fun fill(
        ignore: Int,
        resource: FluidStack,
        simulate: Boolean,
        notifyChanges: Boolean
    ): Long {
        if (resource.isEmpty) return 0

        val fluidIn: FluidStack? = fluidInventory.get(resource)
        val realFill = min(Long.MAX_VALUE - (fluidIn?.amount ?: 0), resource.amount)

        return if (simulate)
            realFill
        else
            if (realFill > 0) {
                realFill.also {
                    fluidIn?.let {
                        it.amount += realFill
                    } ?: run {
                        fluidInventory.add(resource.copy(realFill))
                    }
                    if (notifyChanges) this.onContentsChanged()
                }
            } else 0
    }

    override fun supportsFill(tank: Int): Boolean = true

    @Suppress("UnstableApiUsage")
    override fun drain(tank: Int, resource: FluidStack?, simulate: Boolean, notifyChanges: Boolean): FluidStack =
        FluidStack.empty()

    override fun drain(maxDrain: Long, simulate: Boolean): FluidStack = FluidStack.empty()

    override fun drain(maxDrain: Long, simulate: Boolean, notifyChanges: Boolean): FluidStack = FluidStack.empty()

    override fun drain(resource: FluidStack?, simulate: Boolean): FluidStack = FluidStack.empty()

    override fun drain(resource: FluidStack?, simulate: Boolean, notifyChanges: Boolean): FluidStack =
        FluidStack.empty()

    override fun supportsDrain(tank: Int): Boolean = false

    @Suppress("UnstableApiUsage")
    override fun createSnapshot(): Any {
        throw UnsupportedOperationException("Why Fabric")
    }

    @Suppress("UnstableApiUsage")
    override fun restoreFromSnapshot(snapshot: Any?) {
        throw UnsupportedOperationException("Why Fabric")
    }

    override fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()

        val fluidsTag = AEUtils.createListTag({ fluidStack ->
            CompoundTag().apply {
                fluidStack.saveToTag(this)
            }
        }, fluidInventory)
        if (!fluidsTag.isEmpty()) tag.put("fluidInventory", fluidsTag)

        return tag
    }

    override fun deserializeNBT(tag: CompoundTag) {
        fluidInventory.clear()

        val fluids = tag.getList("fluidInventory", Tag.TAG_COMPOUND.toInt())
        AEUtils.loadInventory(fluids, FluidStack::loadFromTag, fluidInventory)
    }

    override fun setOnContentsChanged(onContentChanged: Runnable?) {}

    override fun getOnContentsChanged(): Runnable = Runnable { }
}