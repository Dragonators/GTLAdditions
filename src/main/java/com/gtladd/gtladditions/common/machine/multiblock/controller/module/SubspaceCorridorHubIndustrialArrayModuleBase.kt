package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.multiblock.controller.SubspaceCorridorHubIndustrialArray
import com.gtladd.gtladditions.common.machine.multiblock.controller.SubspaceCorridorNexusIndustrialArray
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.recipe.RecipeCalculationHelper
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule

class SubspaceCorridorHubIndustrialArrayModuleBase(holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder),
    IModularMachineModule<SubspaceCorridorHubIndustrialArray, SubspaceCorridorHubIndustrialArrayModuleBase>,
    IMachineLife {
    private var host: SubspaceCorridorHubIndustrialArray? = null

    override fun getHostPosition(): BlockPos? = host?.pos
    override fun setHostPosition(pos: BlockPos?) = Unit
    override fun getHost(): SubspaceCorridorHubIndustrialArray? = host
    override fun setHost(host: SubspaceCorridorHubIndustrialArray?) {
        this.host = host
    }
    override fun getHostType(): Class<SubspaceCorridorHubIndustrialArray> = SubspaceCorridorHubIndustrialArray::class.java
    override fun getHostScanPositions(): Array<BlockPos> = emptyArray()

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = SubspaceCorridorHubIndustrialArrayModuleBaseLogic(this)

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!this.isFormed) return
        textList.add(
            Component.translatable(
                if (isConnectedToHost) "tooltip.gtlcore.module_installed" else "tooltip.gtlcore.module_not_installed"
            )
        )
    }

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        if (isConnectedToHost && host!!.unlockParadoxical()) {
            textList.add(
                Component.translatable(
                    "gtceu.multiblock.parallel",
                    createLanguageRainbowComponentOnServer(
                        Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel")
                    )
                ).withStyle(ChatFormatting.GRAY)
            )
            textList.add(
                Component.translatable(
                    "gtladditions.multiblock.threads",
                    createLanguageRainbowComponentOnServer(
                        Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel")
                    )
                ).withStyle(ChatFormatting.GRAY)
            )
        } else {
            super.addParallelDisplay(textList)
        }
    }

    // ===============================================
    // SubspaceCorridorHubIndustrialArray connection
    // ===============================================

    override fun findAndConnectToHost(): Boolean {
        val world = level as? ServerLevel ?: return false
        return SubspaceCorridorHubIndustrialArray.MODULE_CONNECTIONS.slotsAt(world.dimension(), pos).any {
            it.host.tryConnectModule(it.index, this)
        }
    }

    private fun unregisterModule() {
        (level as? ServerLevel)?.let {
            SubspaceCorridorHubIndustrialArray.MODULE_CONNECTIONS.removeModule(it.dimension(), pos, this)
        }
        removeFromHost(host)
    }

    override fun onConnected(host: SubspaceCorridorHubIndustrialArray) {
        (host as? SubspaceCorridorNexusIndustrialArray)?.onModuleConnected(this)
        recipeLogic.updateTickSubscription()
    }

    override fun removeFromHost(host: SubspaceCorridorHubIndustrialArray?) {
        super.removeFromHost(host)
        (host as? SubspaceCorridorNexusIndustrialArray)?.onModuleDisconnected(this)
    }

    override fun notifyStatusChanged(oldStatus: RecipeLogic.Status, newStatus: RecipeLogic.Status) {
        super.notifyStatusChanged(oldStatus, newStatus)
        if ((oldStatus == RecipeLogic.Status.WORKING) != (newStatus == RecipeLogic.Status.WORKING)) {
            (host as? SubspaceCorridorNexusIndustrialArray)?.onModuleActivityChanged(this, newStatus == RecipeLogic.Status.WORKING)
        }
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        (level as? ServerLevel)?.let { SubspaceCorridorHubIndustrialArray.MODULE_CONNECTIONS.addModule(it.dimension(), pos, this) }
        if (!findAndConnectToHost()) {
            removeFromHost(this.host)
        }
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        unregisterModule()
    }

    override fun onPartUnload() {
        super.onPartUnload()
        unregisterModule()
    }

    override fun onMachineRemoved() {
        unregisterModule()
    }

    override fun onUnload() {
        unregisterModule()
        super.onUnload()
    }

    override fun needConfirmMEStock(): Boolean = true

    companion object {
        val BEFORE_WORKING = Predicate { machine: IRecipeLogicMachine ->
            (machine as SubspaceCorridorHubIndustrialArrayModuleBase).host?.recipeLogic?.isWorking == true
        }

        open class SubspaceCorridorHubIndustrialArrayModuleBaseLogic(
            parallel: SubspaceCorridorHubIndustrialArrayModuleBase
        ) : GTLAddMultipleRecipesLogic(parallel, BEFORE_WORKING) {

            override fun getMachine(): SubspaceCorridorHubIndustrialArrayModuleBase = machine as SubspaceCorridorHubIndustrialArrayModuleBase

            override fun calculateParallels(): ParallelData? {
                if (!getMachine().host!!.unlockParadoxical()) {
                    return super.calculateParallels()
                }

                val recipes = lookupRecipeIterator()
                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes,
                    machine,
                    getParallelLimitForRecipe = { Long.MAX_VALUE },
                    getMaxParallelForRecipe = ::getMaxParallel
                )
            }
        }
    }
}
