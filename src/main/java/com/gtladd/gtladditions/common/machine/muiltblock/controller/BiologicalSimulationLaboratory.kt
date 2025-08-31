package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.lowdragmc.lowdraglib.gui.widget.*
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper.*
import org.gtlcore.gtlcore.utils.Registries.*
import java.util.function.BiPredicate
import kotlin.math.max

class BiologicalSimulationLaboratory(holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder) {
    private var limitedDuration = 20

    @Persisted
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
            if (this.holder.offsetTimer % 20L == 0L) this.setparameter(this)
            textList.add(
                Component.translatable(
                    "gtceu.multiblock.parallel", Component.translatable(FormattingUtil.formatNumbers(
                            Max_Parallels
                        )).withStyle(ChatFormatting.DARK_PURPLE)
                ).withStyle(ChatFormatting.GRAY)
            )
            textList.add(Component.translatable((if (Is_MultiRecipe) "已" else "未") + "解锁寰宇支配之剑的配方"))
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

    private fun setparameter(machine: MetaMachine?) {
        val tier = getTier(machine)
        when (tier) {
            1 -> setMachine(false, 2048, 0.9, 0.9)
            2 -> setMachine(false, 16384, 0.8, 0.6)
            3 -> setMachine(false, 262144, 0.6, 0.4)
            4 -> setMachine(true, 4194304, 0.25, 0.1)
            else -> setMachine(false, 64, 1.0, 1.0)
        }
    }

    private fun setMachine(isMultiRecipe: Boolean, maxParallel: Int, Reductioneut: Double, Reductionduration: Double) {
        Is_MultiRecipe = isMultiRecipe
        Max_Parallels = maxParallel
        reDuctionEUt = Reductioneut
        reDuctionDuration = Reductionduration
    }

    override fun getMaxParallel(): Int {
        return Max_Parallels
    }

    override fun setLimitedDuration(number: Int) {
        if (number != limitedDuration) limitedDuration = number
    }

    override fun getLimitedDuration(): Int {
        return this.limitedDuration
    }

    class BiologicalSimulationLaboratoryLogic(machine: WorkableElectricMultiblockMachine?) :
        GTLAddMultipleRecipesLogic((machine as ParallelMachine?) !!) {

        override fun getMachine(): BiologicalSimulationLaboratory? {
            return super.getMachine() as BiologicalSimulationLaboratory?
        }

        override fun findAndHandleRecipe() {
            lastRecipe = null
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
                var recipe = machine.recipeType.lookup.find(machine) { r: GTRecipe? ->
                    matchRecipe(machine, r!!) && BEFORE_RECIPE.test(r, machine)
                }
                if (recipe == null || recipe.data.getInt("euTier") > getMachine()!!.getTier()) return null
                val p = ParallelLogic.applyParallel(machine as MetaMachine, recipe,
                    parallel.maxParallel, false)
                recipe = p.first
                RecipeHelper.setInputEUt(recipe, max(1.0, (RecipeHelper.getInputEUt(recipe) * reDuctionEUt * p.second)).toLong())
                recipe.duration = max(1.0, recipe.duration.toDouble() *
                            reDuctionDuration / (1 shl (getMachine() !!.getTier() - RecipeHelper.getRecipeEUtTier(recipe)))).toInt()
                return recipe
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

        override fun checkRecipe(recipe: GTRecipe): Boolean {
            return super.checkRecipe(recipe) && BEFORE_RECIPE.test(recipe, machine)
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
        private val NAN_CERTIFICATE: ItemStack = getItemStack("gtceu:nan_certificate")
        private val BEFORE_RECIPE: BiPredicate<GTRecipe?, IRecipeLogicMachine?> =
            BiPredicate { recipe: GTRecipe?, machine: IRecipeLogicMachine? ->
                if (machine !is BiologicalSimulationLaboratory) return@BiPredicate false
                machine.setparameter(machine)
                val input = RecipeHelper.getInputItems(recipe!!)
                for (stack in input) {
                    if (stack.item == getItem("avaritia:infinity_sword") && !Is_MultiRecipe) {
                        RecipeResult.of(machine, RecipeResult.fail(Component.literal("该配方需要不再是菜鸟的证明来解锁")))
                        return@BiPredicate false
                    }
                }
                true
            }
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(BiologicalSimulationLaboratory::class.java, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER)
    }
}
