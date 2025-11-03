package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.handleRecipeOutput
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.matchRecipeOutput
import org.gtlcore.gtlcore.common.data.GTLRecipeModifiers
import org.gtlcore.gtlcore.utils.Registries.getItem
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import java.util.function.BiPredicate

class BiologicalSimulationLaboratory(holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder), IMachineLife {

    @field:Persisted
    val machineStorage: NotifiableItemStackHandler = createMachineStorage()

    private var reductionEUt = 1.0
    private var reductionDuration = 1.0
    private var maxParallels = 64
    private var isMultiRecipe = false

    fun createMachineStorage(): NotifiableItemStackHandler {
        val handler = NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH) { slots ->
            object : ItemStackTransfer(1) {
                override fun getSlotLimit(slot: Int): Int = 1
                override fun onContentsChanged() {
                    recalculateParameters()
                }
            }
        }
        handler.setFilter { itemStack: ItemStack? -> this.filter(itemStack!!) }
        return handler
    }

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return BiologicalSimulationLaboratoryLogic(this)
    }

    override fun getRecipeLogic(): BiologicalSimulationLaboratoryLogic {
        return super.getRecipeLogic() as BiologicalSimulationLaboratoryLogic
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        recalculateParameters()
    }

    fun filter(itemStack: ItemStack): Boolean {
        val item = itemStack.item
        return NAN_CERTIFICATE.`is`(item) || INFUSCOLIUM_NANOSWARM.`is`(item) || ORICHALCUM_NANOSWARM.`is`(item) || RHENIUM_NANOSWARM.`is`(
            item
        )
    }

    fun isMultiRecipeEnabled(): Boolean = isMultiRecipe

    override fun createUIWidget(): Widget {
        val widget = super.createUIWidget()
        if (widget is WidgetGroup) {
            val size = widget.size
            widget.addWidget(
                SlotWidget(machineStorage.storage, 0, size.width - 30, size.height - 30, true, true)
                    .setBackground(GuiTextures.SLOT)
            )
        }
        return widget
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (this.isFormed) {
            textList.add(Component.translatable("gtceu.machine.biological_simulation_laboratory.gui.tooltip." + if (isMultiRecipe) 1 else 0))
            textList.add(
                Component.translatable(
                    "gtceu.machine.eut_multiplier.tooltip", Component.translatable(
                        FormattingUtil.formatNumbers(reductionEUt))
                )
            )
            textList.add(
                Component.translatable(
                    "gtceu.machine.duration_multiplier.tooltip", Component.translatable(
                        FormattingUtil.formatNumbers(reductionDuration))
                )
            )
        }
    }

    private fun recalculateParameters() {
        val item = machineStorage.storage.getStackInSlot(0).item
        when {
            RHENIUM_NANOSWARM.`is`(item) -> setMachineParameters(false, 2048, 0.9, 0.9)
            ORICHALCUM_NANOSWARM.`is`(item) -> setMachineParameters(false, 16384, 0.8, 0.6)
            INFUSCOLIUM_NANOSWARM.`is`(item) -> setMachineParameters(false, 262144, 0.6, 0.4)
            NAN_CERTIFICATE.`is`(item) -> setMachineParameters(true, 4194304, 0.25, 0.1)
            else -> setMachineParameters(false, 64, 1.0, 1.0)
        }
    }

    private fun setMachineParameters(isMultiRecipe: Boolean, maxParallel: Int, reductionEut: Double, reductionDuration: Double) {
        this@BiologicalSimulationLaboratory.isMultiRecipe = isMultiRecipe
        maxParallels = maxParallel
        reductionEUt = reductionEut
        this@BiologicalSimulationLaboratory.reductionDuration = reductionDuration
    }

    override fun onMachineRemoved() = clearInventory(machineStorage)

    override fun getMaxParallel(): Int = maxParallels

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    class BiologicalSimulationLaboratoryLogic(machine: BiologicalSimulationLaboratory) :
        GTLAddMultipleRecipesLogic(machine, BEFORE_RECIPE) {

        override fun getMachine(): BiologicalSimulationLaboratory {
            return super.getMachine() as BiologicalSimulationLaboratory
        }

        override fun findAndHandleRecipe() {
            lastRecipe = null
            recipeStatus = null
            val match = if (this.isNanCertificate) gtRecipe
            else this.oneRecipe
            if (match != null && matchRecipeOutput(this.machine, match)) {
                setupRecipe(match)
            }
        }

        val isNanCertificate: Boolean
            get() {
                val item = getMachine().machineStorage.storage.getStackInSlot(0)
                return item.item == getItem("gtceu:nan_certificate")
            }

        val oneRecipe: GTRecipe?
            get() {
                if (!machine.hasProxies()) return null
                val lab = getMachine()
                var recipe = machine.recipeType.lookup.find(machine, this::checkRecipe)
                if (recipe == null) return null
                recipe = ParallelLogic.applyParallel(lab, recipe,
                    parallel.maxParallel, false).first
                return GTLRecipeModifiers.reduction(lab, recipe,
                    lab.reductionEUt, lab.reductionDuration)
            }

        override fun onRecipeFinish() {
            machine.afterWorking()
            lastRecipe?.let { handleRecipeOutput(this.machine, it) }
            val match = if (this.isNanCertificate) gtRecipe else this.oneRecipe
            if (match != null && matchRecipeOutput(this.machine, match)) {
                setupRecipe(match)
                return
            }
            status = Status.IDLE
            progress = 0
            duration = 0
        }
    }

    companion object {
        private val RHENIUM_NANOSWARM: ItemStack = getItemStack("gtceu:rhenium_nanoswarm")
        private val ORICHALCUM_NANOSWARM: ItemStack = getItemStack("gtceu:orichalcum_nanoswarm")
        private val INFUSCOLIUM_NANOSWARM: ItemStack = getItemStack("gtceu:infuscolium_nanoswarm")
        private val NAN_CERTIFICATE: ItemStack = GTItems.NAN_CERTIFICATE.asStack()

        private val BEFORE_RECIPE: BiPredicate<GTRecipe, IRecipeLogicMachine> =
            BiPredicate { recipe: GTRecipe, machine: IRecipeLogicMachine ->
                val lab = machine as? BiologicalSimulationLaboratory ?: return@BiPredicate false
                val input = RecipeHelper.getInputItems(recipe)
                for (stack in input) {
                    if (stack.item == getItem("avaritia:infinity_sword") && !lab.isMultiRecipeEnabled()) {
                        RecipeResult.of(machine, RecipeResult.fail(
                            Component.translatable("gtceu.machine.biological_simulation_laboratory.recipe.tooltip.0")))
                        return@BiPredicate false
                    }
                }
                true
            }

        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(BiologicalSimulationLaboratory::class.java, GTLAddWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER)
    }
}
