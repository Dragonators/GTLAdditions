package com.gtladd.gtladditions.common.machine.muiltblock.controller.module

import com.google.common.base.Predicate
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.common.data.ParallelData
import com.gtladd.gtladditions.common.machine.muiltblock.controller.SubspaceCorridorHubIndustrialArray
import com.gtladd.gtladditions.utils.CommonUtils.createRainbowComponent
import com.gtladd.gtladditions.utils.IndustrialArrayPosHelper
import com.gtladd.gtladditions.utils.RecipeCalculationHelper
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule

class SubspaceCorridorHubIndustrialArrayModuleBase(holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder),
    IModularMachineModule<SubspaceCorridorHubIndustrialArray, SubspaceCorridorHubIndustrialArrayModuleBase>,
    IMachineLife {
    @field:Persisted
    private var hostPosition: BlockPos? = null
    private var host: SubspaceCorridorHubIndustrialArray? = null

    override fun getHostPosition(): BlockPos? = hostPosition
    override fun setHostPosition(pos: BlockPos?) { hostPosition = pos }
    override fun getHost(): SubspaceCorridorHubIndustrialArray? = host
    override fun setHost(host: SubspaceCorridorHubIndustrialArray?) { this.host = host }
    override fun getHostType(): Class<SubspaceCorridorHubIndustrialArray> = SubspaceCorridorHubIndustrialArray::class.java
    override fun getHostScanPositions(): Array<BlockPos> = IndustrialArrayPosHelper.calculatePossibleHostPositions(pos)

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return SubspaceCorridorHubIndustrialArrayModuleBaseLogic(this)
    }

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
                    createRainbowComponent(
                        Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                    )
                ).withStyle(ChatFormatting.GRAY)
            )
            textList.add(
                Component.translatable(
                    "gtladditions.multiblock.threads",
                    createRainbowComponent(
                        Component.translatable("gtladditions.multiblock.forge_of_the_antichrist.parallel").string
                    )
                ).withStyle(ChatFormatting.GRAY)
            )
        } else super.addParallelDisplay(textList)
    }

    // ===============================================
    // SubspaceCorridorHubIndustrialArray connection
    // ===============================================

    override fun onConnected(host: SubspaceCorridorHubIndustrialArray) {
        recipeLogic.updateTickSubscription()
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        if (!findAndConnectToHost()) {
            removeFromHost(this.host)
        }
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        removeFromHost(this.host)
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(this.host)
    }

    override fun onMachineRemoved() {
        removeFromHost(this.host)
    }

    override fun needConfirmMEStock(): Boolean = true

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            SubspaceCorridorHubIndustrialArrayModuleBase::class.java,
            GTLAddWorkableElectricMultipleRecipesMachine.Companion.MANAGED_FIELD_HOLDER
        )

        val BEFORE_WORKING = Predicate { machine: IRecipeLogicMachine ->
            (machine as SubspaceCorridorHubIndustrialArrayModuleBase).host?.let { it -> return@let it.isActive } ?: false
        }

        open class SubspaceCorridorHubIndustrialArrayModuleBaseLogic(
            parallel: SubspaceCorridorHubIndustrialArrayModuleBase
        ) : GTLAddMultipleRecipesLogic(parallel, BEFORE_WORKING){

            override fun getMachine(): SubspaceCorridorHubIndustrialArrayModuleBase = machine as SubspaceCorridorHubIndustrialArrayModuleBase

            override fun calculateParallels(): ParallelData? {
                if (!getMachine().host!!.unlockParadoxical()) {
                    return super.calculateParallels()
                }

                val recipes = lookupRecipeIterator()
                return RecipeCalculationHelper.calculateParallelsWithProcessing(
                    recipes, machine,
                    getParallelLimitForRecipe = { Long.MAX_VALUE },
                    getMaxParallelForRecipe = ::getMaxParallel
                )
            }
        }
    }
}