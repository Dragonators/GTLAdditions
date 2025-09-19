package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.block.ICoilType
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.api.recipe.logic.OCParams
import com.gregtechceu.gtceu.api.recipe.logic.OCResult
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic
import com.gregtechceu.gtceu.common.block.CoilBlock
import com.gregtechceu.gtceu.common.data.GTMaterials
import com.gregtechceu.gtceu.common.data.GTRecipeTypes
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gregtechceu.gtceu.utils.GTUtil
import com.hepdd.gtmthings.data.CreativeMachines
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.MethodsReturnNonnullByDefault
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.pattern.util.IValueContainer
import org.gtlcore.gtlcore.common.data.GTLMaterials
import org.gtlcore.gtlcore.common.machine.multiblock.electric.TierCasingMachine
import org.gtlcore.gtlcore.utils.Registries
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.*

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
open class TaixuTurbidArray(holder: IMachineBlockEntity) : TierCasingMachine(holder, "SCTier"), IMachineModifyDrops {
    @Persisted
    val machineStorage: NotifiableItemStackHandler
    private var coilType: ICoilType
    private var height = 0
    protected fun createMachineStorage(): NotifiableItemStackHandler {
        val handler = NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH) { slots: Int? ->
            object : ItemStackTransfer(1) {}
        }
        handler.setFilter { itemStack: ItemStack? -> this.filter(itemStack!!) }
        return handler
    }

    override fun createUIWidget(): Widget {
        val widget = super.createUIWidget()
        if (widget is WidgetGroup) {
            val size = widget.size
            widget.addWidget(
                (SlotWidget(this.machineStorage.storage, 0, size.width - 30, size.height - 30, true, true))
                    .setBackground(GuiTextures.SLOT).setHoverTooltips(this.slotTooltips())
            )
        }
        return widget
    }

    protected fun filter(itemStack: ItemStack): Boolean {
        val item = itemStack.item
        return ENDERIUM.`is`(item) || DRACONIUM.`is`(item) || SPACETIME.`is`(item) || ETERNITY.`is`(item) || CREATE.`is`(item)
    }

    override fun onDrops(drops: MutableList<ItemStack?>) {
        this.clearInventory(this.machineStorage.storage)
    }

    private fun slotTooltips(): MutableList<Component?> {
        val tooltip: MutableList<Component?> = ArrayList()
        tooltip.add(Component.literal("最多可以放入64个物品"))
        tooltip.add(Component.literal("可放入以下物品与提供对应的加成："))
        tooltip.add(Component.literal("末影纳米蜂群：0.01"))
        tooltip.add(Component.literal("龙纳米蜂群：0.05"))
        tooltip.add(Component.literal("时空纳米蜂群：0.1"))
        tooltip.add(Component.literal("永恒纳米蜂群：0.2"))
        return tooltip
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        val context = this.multiblockState.matchContext
        val type = context.get<Any?>("CoilType")
        if (type is ICoilType) this.coilType = type
        val speedPipe = context.getOrCreate("SpeedPipeValue") { IValueContainer.noop() }.getValue()
        if (speedPipe is Int) this.height = speedPipe - 2
    }

    override fun beforeWorking(recipe: GTRecipe?): Boolean {
        return true
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (this.isFormed) {
            textList.add(Component.literal("高度：" + this.height))
            textList.add(Component.literal("最大并行数：" + this.getMaxParallel()))
            if (this.energyTier > GTValues.UIV) {
                textList.add(Component.literal("UU增幅液成功概率：" + this.successRateA() + "%"))
                textList.add(Component.literal("UU增幅液基础输出量：" + this.baseOutputFluid1() + "mb"))
                if (this.energyTier > GTValues.OpV) {
                    textList.add(Component.literal("UU物质成功概率：" + this.successRateB() + "%"))
                    textList.add(Component.literal("UU物质基础输出量：" + this.baseOutputFluid2() + "mb"))
                }
            }
        }
    }

    private fun frameA(): Double {
        return 8.0 * (2.0.pow(this.casingTier.toDouble()) - 1) * sqrt((GTValues.ALL_TIERS[this.energyTier] + 1).toDouble())
    }

    private fun frameB(): Double {
        return 3.8 * 1.3.pow(
            coil[this.coilType.coilTemperature]!!.toDouble()
        ) * (this.coilType.coilTemperature / 36000.0).pow(0.7)
    }

    private fun successRateA(): Int {
        if (machineStorage.getStackInSlot(0).`is`(CREATE.item)) return 100
        return (100 / (1 + exp(-0.1 * (this.frameA() / 50 + this.frameB() / 100 + this.height / 9.0))) + this.slotAdd).roundToInt()
    }

    private fun successRateB(): Int {
        if (machineStorage.getStackInSlot(0).`is`(CREATE.item)) return 100
        return (100 * (1 - exp(-0.02 * ((this.frameA() + this.frameB()) / 20.0 + cbrt(this.height.toDouble()) * this.energyTier / 7.0))) + this.slotAdd).roundToInt()
    }

    private fun baseOutputFluid1(): Int {
        return (4096 * (1 - exp(-0.015 * (this.frameA() * this.height / 16.0 + this.frameB() * ln((this.energyTier + 2).toDouble()))))).toInt()
    }

    private fun baseOutputFluid2(): Int {
        return (2250 * tanh(sqrt(this.frameA() * this.frameB()) * (this.height + this.energyTier) * 0.06 / 200.0)).toInt()
    }

    fun getMaxParallel(): Int {
        if (machineStorage.getStackInSlot(0).`is`(CREATE.item)) return 3.shl(16)
        return (4096 * 1.621.pow((this.coilType.coilTemperature.toDouble() / 6400))).toInt()
    }

    private val energyTier: Int
        get() = GTUtil.getFloorTierByVoltage(this.maxVoltage).toInt()

    private val slotAdd: Double
        get() {
            val item = this.machineStorage.storage.getStackInSlot(0).item
            val amount = this.machineStorage.storage.getStackInSlot(0).count
            if (ENDERIUM.`is`(item)) return 0.01 * amount
            else if (DRACONIUM.`is`(item)) return 0.05 * amount
            else if (SPACETIME.`is`(item)) return 0.1 * amount
            else if (ETERNITY.`is`(item)) return 0.2 * amount
            return 0.0
        }

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    init {
        this.machineStorage = createMachineStorage()
        this.coilType = CoilBlock.CoilType.CUPRONICKEL
    }

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(TaixuTurbidArray::class.java, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER)
        private val coil: MutableMap<Int?, Int?> = HashMap<Int?, Int?>()
        private val ENDERIUM: ItemStack = Registries.getItemStack("gtceu:enderium_nanoswarm", 64)
        private val DRACONIUM: ItemStack = Registries.getItemStack("gtceu:draconium_nanoswarm", 64)
        private val SPACETIME: ItemStack = Registries.getItemStack("gtceu:spacetime_nanoswarm", 64)
        private val ETERNITY: ItemStack = Registries.getItemStack("gtceu:eternity_nanoswarm", 64)
        private val CREATE: ItemStack = CreativeMachines.CREATIVE_ENERGY_INPUT_HATCH.asStack()

        fun recipeModifier(machine: MetaMachine, recipe: GTRecipe, params: OCParams, result: OCResult): GTRecipe? {
            if (machine is TaixuTurbidArray) {
                var recipe1 = recipe.copy()
                val builder = GTRecipeBuilder(ResourceLocation.tryParse("uu"), GTRecipeTypes.DUMMY_RECIPES)
                recipe1.outputs.put(FluidRecipeCapability.CAP, ObjectArrayList())
                if (Math.random() * 100 <= machine.successRateA() && machine.energyTier >= GTValues.UXV) {
                    builder.outputFluids(GTLMaterials.UuAmplifier.getFluid(machine.baseOutputFluid1().toLong()))
                }
                if (Math.random() * 100 <= machine.successRateB() && machine.energyTier >= GTValues.MAX) {
                    builder.outputFluids(GTMaterials.UUMatter.getFluid(machine.baseOutputFluid2().toLong()))
                }
                if (builder.buildRawRecipe().outputs[FluidRecipeCapability.CAP] != null) {
                    recipe1.outputs[FluidRecipeCapability.CAP]!!
                        .addAll(builder.buildRawRecipe().outputs[FluidRecipeCapability.CAP]!!)
                }
                var maxParallel = ParallelLogic.getMaxRecipeMultiplier(recipe1, machine, machine.getMaxParallel())
                val minParallel = ParallelLogic.limitByOutputMerging(recipe1, machine, machine.getMaxParallel()
                ) { capability: RecipeCapability<*>? -> machine.canVoidRecipeOutputs(capability) }
                if (minParallel < maxParallel) maxParallel = minParallel
                recipe1 = recipe1.copy(ContentModifier.multiplier(maxParallel.toDouble()), false)
                recipe1.duration = 100
                RecipeHelper.setInputEUt(recipe1, 524288L * GTValues.V[machine.energyTier])
                return recipe1
            }
            return null
        }

        init {
            coil.put(1800, 1)
            coil.put(2700, 2)
            coil.put(3600, 3)
            coil.put(4500, 4)
            coil.put(5400, 5)
            coil.put(7200, 6)
            coil.put(9001, 7)
            coil.put(10800, 8)
            coil.put(12600, 9)
            coil.put(14400, 10)
            coil.put(16200, 11)
            coil.put(18900, 12)
            coil.put(21600, 13)
            coil.put(36000, 14)
            coil.put(62000, 15)
            coil.put(96000, 16)
        }
    }
}
