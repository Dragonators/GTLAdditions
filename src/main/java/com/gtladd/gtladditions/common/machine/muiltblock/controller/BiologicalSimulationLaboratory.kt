package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.RecipeHelper
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.IGTLAddMultiRecipe
import com.gtladd.gtladditions.api.machine.gui.LimitedDurationConfigurator
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.api.recipe.IParallelLogic
import org.gtlcore.gtlcore.api.recipe.RecipeRunnerHelper
import org.gtlcore.gtlcore.common.machine.multiblock.electric.StorageMachine
import org.gtlcore.gtlcore.utils.Registries.getItem
import org.gtlcore.gtlcore.utils.Registries.getItemStack
import kotlin.math.max

class BiologicalSimulationLaboratory(holder: IMachineBlockEntity) : StorageMachine(holder, 1), ParallelMachine,
    IGTLAddMultiRecipe {
    private var limitedDuration = 20
    override fun createRecipeLogic(vararg args: Any?): RecipeLogic {
        return BiologicalSimulationLaboratoryLogic(this)
    }

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        tag.putInt("drLimit", limitedDuration)
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        limitedDuration = tag.getInt("drLimit")
    }

    override fun filter(itemStack: ItemStack): Boolean {
        val item = itemStack.item
        return NAN_CERTIFICATE.`is`(item) || INFUSCOLIUM_NANOSWARM.`is`(item) || ORICHALCUM_NANOSWARM.`is`(item) || RHENIUM_NANOSWARM.`is`(
            item
        )
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

    override fun beforeWorking(recipe: GTRecipe?): Boolean {
        this.setparameter(this as MetaMachine)
        val input = RecipeHelper.getInputItems(recipe)
        for (itemstack in input) {
            if (itemstack.item == getItem("avaritia:infinity_sword") && !Is_MultiRecipe) return false
        }
        return super.beforeWorking(recipe)
    }

    private fun getTier(machine: MetaMachine?): Int {
        if (machine is BiologicalSimulationLaboratory) {
            val item = machine.machineStorage.storage.getStackInSlot(0).item
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

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        configuratorPanel.attachConfigurators(LimitedDurationConfigurator(this))
    }

    override fun setLimitedDuration(number: Int) {
        if (number != limitedDuration) limitedDuration = number
    }

    override fun getLimitedDuration(): Int {
        return this.limitedDuration
    }

    private class BiologicalSimulationLaboratoryLogic(machine: WorkableElectricMultiblockMachine?) :
        GTLAddMultipleRecipesLogic((machine as ParallelMachine?) !!) {
        override fun getMachine(): BiologicalSimulationLaboratory? {
            return super.getMachine() as BiologicalSimulationLaboratory?
        }

        override fun findAndHandleRecipe() {
            lastRecipe = null
            val match = if (this.isNanCertificate) gtRecipe
            else this.oneRecipe
            if (match != null && RecipeRunnerHelper.matchRecipeOutput(this.machine, match)) {
                setupRecipe(match)
            }
        }

        val isNanCertificate: Boolean
            get() {
                val item = getMachine()!!.machineStorageItem
                return item.item == getItem("gtceu:nan_certificate")
            }

        val oneRecipe: GTRecipe?
            get() {
                if (!machine.hasProxies()) return null
                var recipe = machine.recipeType.lookup.findRecipe(machine)
                if (recipe == null || RecipeHelper.getRecipeEUtTier(recipe) > getMachine()!!.getTier()) return null
                val p = IParallelLogic.getMaxParallel(machine, recipe, parallel.maxParallel.toLong())
                if (p > 1) recipe = recipe.copy(ContentModifier.multiplier(p.toDouble()), false)
                RecipeHelper.setInputEUt(recipe, max(1.0, (RecipeHelper.getInputEUt(recipe) * reDuctionEUt * p)).toLong())
                recipe.duration = max(1.0, recipe.duration.toDouble() *
                            reDuctionDuration / (1 shl (getMachine() !!.getTier() - RecipeHelper.getRecipeEUtTier(recipe)))).toInt()
                return recipe
            }

        override fun onRecipeFinish() {
            machine.afterWorking()
            if (lastRecipe != null) {
                lastRecipe!!.postWorking(this.machine)
                RecipeRunnerHelper.handleRecipeOutput(this.machine, lastRecipe!!)
            }
            val match = if (this.isNanCertificate) gtRecipe else this.oneRecipe
            if (match != null && RecipeRunnerHelper.matchRecipeOutput(this.machine, match)) {
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
        private val NAN_CERTIFICATE: ItemStack = getItemStack("gtceu:nan_certificate")
    }
}
