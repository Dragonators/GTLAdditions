package com.gtladd.gtladditions.api.machine.gui

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient
import com.gtladd.gtladditions.api.machine.logic.chemical.ExoticChemicalRecipeLogic
import com.gtladd.gtladditions.api.machine.logic.chemical.ExoticChemicalUnitLogic
import com.gtladd.gtladditions.api.recipe.IWirelessGTRecipe
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import org.gtlcore.gtlcore.common.data.GTLRecipeTypes.DISTORT_RECIPES
import java.math.BigInteger
import kotlin.math.roundToInt

internal enum class ExoticChemicalUnitState {
    RUN,
    WAIT,
    PAUSE,
    IDLE,
    LOCKED
}

internal data class ExoticChemicalOutput(
    val item: ItemStack,
    val fluid: FluidStack,
    val amount: Long
) {
    fun toTag(): CompoundTag = CompoundTag().apply {
        if (item.isEmpty) {
            putString("kind", "fluid")
            put("fluid", fluid.writeToNBT(CompoundTag()))
        } else {
            putString("kind", "item")
            put("item", item.save(CompoundTag()))
        }
        putLong("amount", amount)
    }
}

internal object ExoticChemicalDisplay {

    const val PER_PAGE = 4
    const val ROW_OUTPUTS = 6

    // ========================================
    // UNIT STATUS & ENERGY
    // ========================================

    fun label(key: String, vararg args: Any?): MutableComponent = ("gtladditions.exotic_chemical.$key").toComponent(*args)

    fun progressLabel(progress: Int, duration: Int): MutableComponent = if (duration < 20) {
        "gtceu.jade.progress_tick".toComponent(progress, duration)
    } else {
        "gtceu.jade.progress_sec".toComponent((progress / 20f).roundToInt(), (duration / 20f).roundToInt())
    }

    fun recipeMode(distort: Boolean): MutableComponent = label(if (distort) "recipe.distort" else "recipe.large_chemical")

    fun state(coordinator: ExoticChemicalRecipeLogic, unitLogic: ExoticChemicalUnitLogic): ExoticChemicalUnitState =
        if (coordinator.isUnitUnlocked(unitLogic.id)) {
            operatingState(coordinator, unitLogic)
        } else {
            ExoticChemicalUnitState.LOCKED
        }

    fun operatingState(coordinator: ExoticChemicalRecipeLogic, unitLogic: ExoticChemicalUnitLogic): ExoticChemicalUnitState {
        val result = unitLogic.recipeStatus
        return when {
            !unitLogic.isRecipeLogicAvailable -> ExoticChemicalUnitState.WAIT
            coordinator.globalPaused || unitLogic.isSuspend -> ExoticChemicalUnitState.PAUSE
            coordinator.host.uuid == null -> ExoticChemicalUnitState.WAIT
            unitLogic.isWorking -> ExoticChemicalUnitState.RUN
            unitLogic.isWaiting ||
                result != null &&
                !result.isSuccess &&
                result != RecipeResult.FAIL_FIND &&
                result != RecipeResult.FAIL_INPUT -> ExoticChemicalUnitState.WAIT

            else -> ExoticChemicalUnitState.IDLE
        }
    }

    fun message(unitLogic: ExoticChemicalUnitLogic): Component? = when {
        unitLogic.isWaiting -> unitLogic.workingStatus?.reason
        unitLogic.isIdle -> unitLogic.recipeStatus?.reason
        else -> null
    }

    fun netEut(coordinator: ExoticChemicalRecipeLogic): BigInteger {
        var draw = BigInteger.ZERO

        for (unitLogic in coordinator.units) {
            val work = unitLogic.currentRecipe as? IWirelessGTRecipe ?: continue
            if (!coordinator.globalPaused &&
                unitLogic.isRecipeLogicAvailable &&
                unitLogic.isWorking &&
                coordinator.host.uuid != null
            ) {
                draw -= work.getWirelessEuTickInputs() ?: BigInteger.ZERO
            }
        }

        return draw
    }

    // ========================================
    // OUTPUT EXTRACTION
    // ========================================

    fun outputs(recipe: GTRecipe): List<ExoticChemicalOutput> = buildList {
        collectOutputs(recipe, Int.MAX_VALUE) { add(it) }
    }

    // A preview still needs the total count. Copy only the stacks its caller will actually use.
    private inline fun collectOutputs(
        recipe: GTRecipe,
        limit: Int,
        add: (ExoticChemicalOutput) -> Unit
    ): Int {
        var count = 0

        for (content in recipe.getOutputContents(ItemRecipeCapability.CAP)) {
            val ingredient = ItemRecipeCapability.CAP.of(content.content)
            val stack = ingredient.items.firstOrNull { !it.isEmpty } ?: continue
            val amount = when (ingredient) {
                is LongIngredient -> ingredient.actualAmount
                is SizedIngredient -> ingredient.amount.toLong()
                else -> stack.count.toLong()
            }
            if (amount <= 0) continue

            if (count < limit) add(ExoticChemicalOutput(stack.copyWithCount(1), FluidStack.EMPTY, amount))
            count++
        }

        for (content in recipe.getOutputContents(FluidRecipeCapability.CAP)) {
            val ingredient = FluidRecipeCapability.CAP.of(content.content)
            val stack = ingredient.getStacks().firstOrNull { !it.isEmpty } ?: continue
            val amount = ingredient.amount
            if (amount <= 0) continue

            if (count < limit) add(ExoticChemicalOutput(ItemStack.EMPTY, FluidStack(stack.fluid, 1, stack.tag), amount))
            count++
        }

        return count
    }

    // ========================================
    // JADE SERIALIZATION
    // ========================================

    fun writeJade(tag: CompoundTag, coordinator: ExoticChemicalRecipeLogic) {
        if (!coordinator.host.isFormed) return

        tag.putInt("tier", coordinator.host.structureTier)

        val eut = netEut(coordinator)
        if (eut.signum() != 0) tag.putByteArray("eut", eut.toByteArray()) else tag.remove("eut")

        val units = ListTag()
        for (unitLogic in coordinator.units) {
            if (!coordinator.isUnitUnlocked(unitLogic.id) || unitLogic.duration <= 0) continue

            val preview = ListTag()
            val count = unitLogic.currentRecipe?.let { recipe ->
                collectOutputs(recipe, ROW_OUTPUTS) { preview.add(it.toTag()) }
            } ?: 0

            units.add(
                CompoundTag().apply {
                    putInt("id", unitLogic.id)
                    putByte("state", state(coordinator, unitLogic).ordinal.toByte())
                    if (unitLogic.recipeType === DISTORT_RECIPES) putBoolean("distortRecipes", true)
                    putInt("progress", unitLogic.progress)
                    putInt("duration", unitLogic.duration)
                    putInt("outputCount", count)
                    put("outputs", preview)
                }
            )
        }

        if (units.isNotEmpty()) tag.put("units", units) else tag.remove("units")
    }
}
