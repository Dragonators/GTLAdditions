package com.gtladd.gtladditions.common.machine.multiblock.controller.module

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.multiblock.controller.LightHunterSpaceStation
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule

class LightHunterSpaceStationModuleBase(holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder),
    IModularMachineModule<LightHunterSpaceStation, LightHunterSpaceStationModuleBase>,
    IMachineLife {
    private var host: LightHunterSpaceStation? = null

    override fun getHostPosition(): BlockPos? = host?.pos
    override fun setHostPosition(pos: BlockPos?) = Unit
    override fun getHost(): LightHunterSpaceStation? = host
    override fun setHost(host: LightHunterSpaceStation?) {
        this.host = host
    }
    override fun getHostType(): Class<LightHunterSpaceStation> = LightHunterSpaceStation::class.java
    override fun getHostScanPositions(): Array<BlockPos> = emptyArray()

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = LightHunterSpaceStationModuleBaseLogic(this)

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!this.isFormed) return
        textList.add(
            (if (isConnectedToHost) "tooltip.gtlcore.module_installed" else "tooltip.gtlcore.module_not_installed").toComponent
        )
    }

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        if (isConnectedToHost && host!!.unlockParadoxical()) {
            textList.add(
                "gtceu.multiblock.parallel".toComponent(
                    createLanguageRainbowComponentOnServer(
                        "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                    )
                ).withStyle(ChatFormatting.GRAY)
            )
            textList.add(
                "gtladditions.multiblock.threads".toComponent(
                    createLanguageRainbowComponentOnServer(
                        "gtladditions.multiblock.forge_of_the_antichrist.parallel".toComponent
                    )
                ).withStyle(ChatFormatting.GRAY)
            )
        } else {
            super.addParallelDisplay(textList)
        }
    }

    // ===============================================
    // LightHunterSpaceStation connection
    // ===============================================

    override fun findAndConnectToHost(): Boolean {
        val world = level as? ServerLevel ?: return false
        return LightHunterSpaceStation.MODULE_CONNECTIONS.slotsAt(world.dimension(), pos).any {
            it.host.tryConnectModule(it.index, this)
        }
    }

    private fun unregisterModule() {
        (level as? ServerLevel)?.let {
            LightHunterSpaceStation.MODULE_CONNECTIONS.removeModule(it.dimension(), pos, this)
        }
        removeFromHost(host)
    }

    override fun onConnected(host: LightHunterSpaceStation) {
        recipeLogic.updateTickSubscription()
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        (level as? ServerLevel)?.let { LightHunterSpaceStation.MODULE_CONNECTIONS.addModule(it.dimension(), pos, this) }
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
            (machine as LightHunterSpaceStationModuleBase).host?.isActive == true
        }

        open class LightHunterSpaceStationModuleBaseLogic(
            parallel: LightHunterSpaceStationModuleBase
        ) : GTLAddMultipleRecipesLogic(parallel, BEFORE_WORKING) {

            override fun getMachine(): LightHunterSpaceStationModuleBase = machine as LightHunterSpaceStationModuleBase

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