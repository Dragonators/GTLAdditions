package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.MethodsReturnNonnullByDefault
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.util.Lazy
import org.gtlcore.gtlcore.api.pattern.util.IValueContainer
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.machine.multiblock.electric.TierCasingMachine
import org.gtlcore.gtlcore.utils.Registries
import java.text.DecimalFormat
import java.util.concurrent.ThreadLocalRandom
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.*

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
open class TaixuTurbidArray(holder: IMachineBlockEntity) : TierCasingMachine(holder, "SCTier"), IMachineModifyDrops {

    @field:Persisted
    val machineStorage: NotifiableItemStackHandler

    private var coilType: ICoilType = CoilBlock.CoilType.CUPRONICKEL
    private var height: Int = 0

    // structure
    private var frameA: Double = 0.0
    private var frameB: Double = 0.0
    private var uuAmplifierAmount: Long = 0
    private var uuMatterAmount: Long = 0

    // itemStack
    private var maxParallel: Long = 0
    private var successRateA: Double = 0.0
    private var successRateB: Double = 0.0

    init {
        this.machineStorage = createMachineStorage()
    }

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(TaixuTurbidArray::class.java, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER)

        // Constants - Frame calculations
        private const val FRAME_A_BASE = 8.0
        private const val FRAME_A_TIER_POWER = 2.0
        private const val FRAME_B_BASE = 3.8
        private const val FRAME_B_POWER_BASE = 1.3
        private const val FRAME_B_TEMP_DIVISOR = 36000.0
        private const val FRAME_B_TEMP_POWER = 0.7

        // Constants - Success rate calculations
        private const val MAX_SUCCESS_RATE = 100.0
        private const val SUCCESS_A_FACTOR = 0.1
        private const val SUCCESS_A_DIVISOR_A = 50.0
        private const val SUCCESS_A_DIVISOR_B = 100.0
        private const val SUCCESS_A_HEIGHT_DIVISOR = 9.0
        private const val SUCCESS_B_FACTOR = 0.02
        private const val SUCCESS_B_DIVISOR = 20.0
        private const val SUCCESS_B_HEIGHT_FACTOR = 7.0

        // Constants - Output calculations
        private const val OUTPUT_1_BASE = 4096
        private const val OUTPUT_1_FACTOR = 0.015
        private const val OUTPUT_1_HEIGHT_DIVISOR = 16.0
        private const val OUTPUT_2_BASE = 2250
        private const val OUTPUT_2_FACTOR = 0.06
        private const val OUTPUT_2_DIVISOR = 200.0

        // Constants - Parallel calculations
        private const val PARALLEL_BASE = 4096.0
        private const val PARALLEL_POWER_BASE = 1.621
        private const val PARALLEL_TEMP_DIVISOR = 6400.0
        private const val ASTRAL_BASE_PARALLEL = 43046721L // 3^16

        // Constants - Slot bonuses
        private const val ENDERIUM_BONUS = 0.01
        private const val DRACONIUM_BONUS = 0.05
        private const val SPACETIME_BONUS = 0.1
        private const val ETERNITY_BONUS = 0.2

        // Constants - Recipe
        private const val RECIPE_DURATION = 100
        private const val RECIPE_EU_MULTIPLIER = 524288L

        private val coil: Lazy<IntArrayList> = Lazy.of {
            IntArrayList(
                GTCEuAPI.HEATING_COILS.keys.stream()
                    .map { it.coilTemperature }
                    .sorted()
                    .toList()
            )
        }

        private val ENDERIUM_ITEM: ItemStack = Registries.getItemStack("gtceu:enderium_nanoswarm", 64)
        private val DRACONIUM_ITEM: ItemStack = Registries.getItemStack("gtceu:draconium_nanoswarm", 64)
        private val SPACETIME_ITEM: ItemStack = Registries.getItemStack("gtceu:spacetime_nanoswarm", 64)
        private val ETERNITY_ITEM: ItemStack = Registries.getItemStack("gtceu:eternity_nanoswarm", 64)
        private val ASTRAL_ARRAY_ITEM: ItemStack = GTLAddItems.ASTRAL_ARRAY.asStack()

        private val VALID_ITEMS: Set<Item> = setOf(
            ENDERIUM_ITEM.item,
            DRACONIUM_ITEM.item,
            SPACETIME_ITEM.item,
            ETERNITY_ITEM.item,
            ASTRAL_ARRAY_ITEM.item
        )

        // ==================== Recipe Modifier ====================
        fun recipeModifier(machine: MetaMachine, recipe: GTRecipe): GTRecipe? {
            if (machine !is TaixuTurbidArray) return null

            var modifiedRecipe = recipe.copy()
            val maxParallel = IParallelLogic.getMaxParallel(machine, recipe, machine.maxParallel)
            if (maxParallel <= 0) return null

            // Build fluid outputs based on success rates
            val builder = GTRecipeBuilder(GTLAdditions.id("uu"), GTRecipeTypes.DUMMY_RECIPES)
            modifiedRecipe.outputs.put(FluidRecipeCapability.CAP, ObjectArrayList())

            val random = ThreadLocalRandom.current()

            // UU Amplifier output (UXV+)
            if (machine.tier >= GTValues.UXV &&
                random.nextDouble(MAX_SUCCESS_RATE) <= machine.successRateA
            ) {
                builder.outputFluids(GTLMaterials.UuAmplifier.getFluid(machine.uuAmplifierAmount))
            }

            // UU Matter output (MAX+)
            if (machine.tier >= GTValues.MAX &&
                random.nextDouble(MAX_SUCCESS_RATE) <= machine.successRateB
            ) {
                builder.outputFluids(GTMaterials.UUMatter.getFluid(machine.uuMatterAmount))
            }

            // Add generated outputs to recipe
            val generatedOutputs = builder.buildRawRecipe().outputs[FluidRecipeCapability.CAP]
            if (generatedOutputs != null && generatedOutputs.isNotEmpty()) {
                modifiedRecipe.outputs[FluidRecipeCapability.CAP]!!.addAll(generatedOutputs)
            }

            // Apply parallel limit based on output merging
            val minParallel = IParallelLogic.getMinParallel(machine, modifiedRecipe, maxParallel)
            if (minParallel <= 0) return null

            // Apply parallel multiplier
            modifiedRecipe = modifiedRecipe.copy(ContentModifier.multiplier(minParallel.toDouble()), false)
            modifiedRecipe.duration = RECIPE_DURATION
            RecipeHelper.setInputEUt(modifiedRecipe, RECIPE_EU_MULTIPLIER * GTValues.V[machine.tier])

            return modifiedRecipe
        }
    }

    // ==================== Initialization ====================
    private fun createMachineStorage(): NotifiableItemStackHandler {
        val handler = NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH) { _ ->
            object : ItemStackTransfer(1) {
                override fun onContentsChanged() {
                    recalculateItemCache()
                }
            }
        }
        handler.setFilter { itemStack -> filter(itemStack) }
        return handler
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        val context = this.multiblockState.matchContext

        val type = context.get<Any?>("CoilType")
        if (type is ICoilType) {
            this.coilType = type
        }

        val speedPipe = context.getOrCreate("SpeedPipeValue") { IValueContainer.noop() }.getValue()
        if (speedPipe is Int) {
            this.height = speedPipe - 2
        }

        recalculateAll()
    }

    private fun recalculateAll() {
        frameA = calculateFrameA()
        frameB = calculateFrameB()
        uuAmplifierAmount = calculateBaseOutputFluid1()
        uuMatterAmount = calculateBaseOutputFluid2()
        recalculateItemCache()
    }

    private fun recalculateItemCache() {
        val itemStack = machineStorage.storage.getStackInSlot(0)
        val item = itemStack.item
        val isAstralArray = ASTRAL_ARRAY_ITEM.`is`(item)

        val slotBonus = when {
            ENDERIUM_ITEM.`is`(item) -> ENDERIUM_BONUS * itemStack.count
            DRACONIUM_ITEM.`is`(item) -> DRACONIUM_BONUS * itemStack.count
            SPACETIME_ITEM.`is`(item) -> SPACETIME_BONUS * itemStack.count
            ETERNITY_ITEM.`is`(item) -> ETERNITY_BONUS * itemStack.count
            else -> 0.0
        }

        maxParallel = if (isAstralArray) {
            ASTRAL_BASE_PARALLEL * itemStack.count.toDouble().pow(5.0/3.0).toInt()
        } else {
            (PARALLEL_BASE * PARALLEL_POWER_BASE.pow(coilType.coilTemperature / PARALLEL_TEMP_DIVISOR)).toLong()
        }

        successRateA = if (isAstralArray) {
            MAX_SUCCESS_RATE
        } else {
            MAX_SUCCESS_RATE / (1 + exp(-SUCCESS_A_FACTOR *
                (frameA / SUCCESS_A_DIVISOR_A + frameB / SUCCESS_A_DIVISOR_B +
                height / SUCCESS_A_HEIGHT_DIVISOR))) + slotBonus
        }

        successRateB = if (isAstralArray) {
            MAX_SUCCESS_RATE
        } else {
            MAX_SUCCESS_RATE * (1 - exp(-SUCCESS_B_FACTOR *
                ((frameA + frameB) / SUCCESS_B_DIVISOR +
                cbrt(height.toDouble()) * tier / SUCCESS_B_HEIGHT_FACTOR))) + slotBonus
        }
    }

    // ==================== GUI ====================
    override fun createUIWidget(): Widget {
        val widget = super.createUIWidget()
        if (widget is WidgetGroup) {
            val size = widget.size
            widget.addWidget(
                SlotWidget(machineStorage.storage, 0, size.width - 30, size.height - 30, true, true)
                    .setBackground(GuiTextures.SLOT)
                    .setHoverTooltips(slotTooltips())
            )
        }
        return widget
    }

    private fun slotTooltips(): MutableList<Component> {
        return mutableListOf(
            Component.translatable("gtceu.machine.taixu.storage.tooltip.0"),
            Component.translatable("gtceu.machine.taixu.storage.tooltip.1"),
            Component.translatable("gtceu.machine.taixu.storage.tooltip.2"),
            Component.translatable("gtceu.machine.taixu.storage.tooltip.3"),
            Component.translatable("gtceu.machine.taixu.storage.tooltip.4"),
            Component.translatable("gtceu.machine.taixu.storage.tooltip.5"),
            Component.translatable("gtceu.machine.taixu.storage.tooltip.6")
        )
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (isFormed) {
            textList.add(Component.translatable("gtceu.machine.taixu.gui.tooltip.0", height))
            textList.add(Component.translatable("gtceu.machine.taixu.gui.tooltip.1", maxParallel))

            if (tier > GTValues.UIV) {
                val df = DecimalFormat(".00'%'")
                textList.add(Component.translatable("gtceu.machine.taixu.gui.tooltip.2", df.format(successRateA)))
                textList.add(Component.translatable("gtceu.machine.taixu.gui.tooltip.3", uuAmplifierAmount))

                if (tier > GTValues.OpV) {
                    textList.add(Component.translatable("gtceu.machine.taixu.gui.tooltip.4", df.format(
                        successRateB
                    )))
                    textList.add(Component.translatable("gtceu.machine.taixu.gui.tooltip.5", uuMatterAmount))
                }
            }
        }
    }

    // ==================== Logic ====================
    override fun beforeWorking(recipe: GTRecipe?): Boolean = true

    override fun onDrops(drops: MutableList<ItemStack?>) {
        clearInventory(machineStorage.storage)
    }

    private fun filter(itemStack: ItemStack): Boolean {
        return VALID_ITEMS.contains(itemStack.item)
    }

    // ==================== Calculation Methods ====================
    private fun calculateFrameA(): Double {
        return FRAME_A_BASE * (FRAME_A_TIER_POWER.pow(casingTier.toDouble()) - 1) *
               sqrt((GTValues.ALL_TIERS[tier] + 1).toDouble())
    }

    private fun calculateFrameB(): Double {
        val coilIndex = coil.get().indexOf(coilType.coilTemperature) + 1
        return FRAME_B_BASE * FRAME_B_POWER_BASE.pow(coilIndex.toDouble()) *
               (coilType.coilTemperature / FRAME_B_TEMP_DIVISOR).pow(FRAME_B_TEMP_POWER)
    }

    private fun calculateBaseOutputFluid1(): Long {
        return (OUTPUT_1_BASE * (1 - exp(-OUTPUT_1_FACTOR *
               (frameA * height / OUTPUT_1_HEIGHT_DIVISOR +
                frameB * ln((tier + 2).toDouble()))))).toLong()
    }

    private fun calculateBaseOutputFluid2(): Long {
        return (OUTPUT_2_BASE * tanh(sqrt(frameA * frameB) *
               (height + tier) * OUTPUT_2_FACTOR / OUTPUT_2_DIVISOR)).toLong()
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER
}