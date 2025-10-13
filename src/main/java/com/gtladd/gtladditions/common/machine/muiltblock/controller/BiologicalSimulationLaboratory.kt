package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
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
    GTLAddWorkableElectricMultipleRecipesMachine(holder) {

    @field:Persisted
    val machineStorage: NotifiableItemStackHandler? = createMachineStorage()

    fun createMachineStorage(): NotifiableItemStackHandler {
        val handler = NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH) { slots ->
            object : ItemStackTransfer(1) {
                override fun getSlotLimit(slot: Int): Int = 1
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

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    fun filter(itemStack: ItemStack): Boolean {
        val item = itemStack.item
        return NAN_CERTIFICATE.`is`(item) || INFUSCOLIUM_NANOSWARM.`is`(item) || ORICHALCUM_NANOSWARM.`is`(item) || RHENIUM_NANOSWARM.`is`(
            item
        )
    }

    override fun createUIWidget(): Widget {
        val widget = super.createUIWidget()
        if (widget is WidgetGroup) {
            val size = widget.size
            widget.addWidget(
                SlotWidget(machineStorage!!.storage, 0, size.width - 30, size.height - 30, true, true)
                    .setBackground(GuiTextures.SLOT)
            )
        }
        return widget
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (this.isFormed) {
            if (this.holder.offsetTimer % 20L == 0L) this.setParameter(this)
            textList.add(Component.translatable("gtceu.machine.biological_simulation_laboratory.gui.tooltip." + if (Is_MultiRecipe) 1 else 0))
            textList.add(
                Component.translatable(
                    "gtceu.machine.eut_multiplier.tooltip", Component.translatable(
                        FormattingUtil.formatNumbers(reDuctionEUt))
                )
            )
            textList.add(
                Component.translatable(
                    "gtceu.machine.duration_multiplier.tooltip", Component.translatable(
                        FormattingUtil.formatNumbers(reDuctionDuration))
                )
            )
        }
    }

    private fun getTier(machine: MetaMachine?): Int {
        if (machine is BiologicalSimulationLaboratory) {
            val item = machine.machineStorage!!.storage.getStackInSlot(0).item
            if (RHENIUM_NANOSWARM.`is`(item)) return 1
            else if (ORICHALCUM_NANOSWARM.`is`(item)) return 2
            else if (INFUSCOLIUM_NANOSWARM.`is`(item)) return 3
            else if (NAN_CERTIFICATE.`is`(item)) return 4
        }
        return 0
    }

    private fun setParameter(machine: MetaMachine?) {
        val tier = getTier(machine)
        when (tier) {
            1 -> setMachine(false, 2048, 0.9, 0.9)
            2 -> setMachine(false, 16384, 0.8, 0.6)
            3 -> setMachine(false, 262144, 0.6, 0.4)
            4 -> setMachine(true, 4194304, 0.25, 0.1)
            else -> setMachine(false, 64, 1.0, 1.0)
        }
    }

    private fun setMachine(isMultiRecipe: Boolean, maxParallel: Int, reductionEut: Double, reductionDuration: Double) {
        Is_MultiRecipe = isMultiRecipe
        Max_Parallels = maxParallel
        reDuctionEUt = reductionEut
        reDuctionDuration = reductionDuration
    }

    override fun getMaxParallel(): Int {
        return Max_Parallels
    }

    class BiologicalSimulationLaboratoryLogic(machine: BiologicalSimulationLaboratory?) :
        GTLAddMultipleRecipesLogic(machine, BEFORE_RECIPE) {

        override fun getMachine(): BiologicalSimulationLaboratory? {
            return super.getMachine() as BiologicalSimulationLaboratory?
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
                val item = getMachine()!!.machineStorage!!.storage.getStackInSlot(0)
                return item.item == getItem("gtceu:nan_certificate")
            }

        val oneRecipe: GTRecipe?
            get() {
                if (!machine.hasProxies()) return null
                var recipe = machine.recipeType.lookup.find(machine, this::checkRecipe)
                if (recipe == null) return null
                recipe = ParallelLogic.applyParallel(machine as MetaMachine, recipe,
                    parallel.maxParallel, false).first
                return GTLRecipeModifiers.reduction(this.machine as MetaMachine?, recipe,
                    reDuctionEUt, reDuctionDuration)
            }

        override fun onRecipeFinish() {
            machine.afterWorking()
            if (lastRecipe != null) {
                handleRecipeOutput(this.machine, lastRecipe!!)
            }
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
        private var reDuctionEUt = 1.0
        private var reDuctionDuration = 1.0
        private var Max_Parallels = 64
        private var Is_MultiRecipe = false
        private val RHENIUM_NANOSWARM: ItemStack = getItemStack("gtceu:rhenium_nanoswarm")
        private val ORICHALCUM_NANOSWARM: ItemStack = getItemStack("gtceu:orichalcum_nanoswarm")
        private val INFUSCOLIUM_NANOSWARM: ItemStack = getItemStack("gtceu:infuscolium_nanoswarm")
        private val NAN_CERTIFICATE: ItemStack = GTItems.NAN_CERTIFICATE.asStack()
        private val BEFORE_RECIPE: BiPredicate<GTRecipe, IRecipeLogicMachine> =
            BiPredicate { recipe: GTRecipe, machine: IRecipeLogicMachine ->
                (machine as BiologicalSimulationLaboratory).let {
                    it.setParameter(machine)
                    val input = RecipeHelper.getInputItems(recipe)
                    for (stack in input) {
                        if (stack.item == getItem("avaritia:infinity_sword") && !Is_MultiRecipe) {
                            RecipeResult.of(machine, RecipeResult.fail(
                                Component.translatable("gtceu.machine.biological_simulation_laboratory.recipe.tooltip.0")))
                            return@BiPredicate false
                        }
                    }
                    return@BiPredicate true
                }
                return@BiPredicate false
            }
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(BiologicalSimulationLaboratory::class.java, GTLAddWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER)
    }
}
