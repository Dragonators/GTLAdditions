package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gtladd.gtladditions.api.machine.gui.MultiblockDisplayText
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.recipe.RecipeResult

abstract class RRFWorkableModuleMachine(holder: IMachineBlockEntity, vararg args: Any?) :
    WorkableElectricMultiblockMachine(holder, *args),
    IModularMachineModule<RecursiveReverseArray, RRFWorkableModuleMachine>,
    IMachineLife {

    @Persisted
    private var hostPosition: BlockPos? = null

    private var host: RecursiveReverseArray? = null
    private var arrayTickSubscription: TickableSubscription? = null

    // ========================================
    // Recursive reverse buff
    // ========================================

    open fun isReadyForRecursiveReverseBuff(): Boolean = isFormed && recipeLogic.isWorking

    // ========================================
    // Module connection
    // ========================================

    override fun getHostPosition(): BlockPos? = hostPosition

    override fun setHostPosition(pos: BlockPos?) {
        hostPosition = pos
    }

    override fun getHost(): RecursiveReverseArray? = host

    override fun setHost(host: RecursiveReverseArray?) {
        this.host = host
    }

    override fun getHostType(): Class<RecursiveReverseArray> = RecursiveReverseArray::class.java

    override fun getHostScanPositions(): Array<BlockPos> = arrayOf(
        pos.offset(33, 7, 0),
        pos.offset(-33, 7, 0),
        pos.offset(0, 7, 33),
        pos.offset(0, 7, -33)
    )

    override fun onConnected(host: RecursiveReverseArray) {
        recipeLogic.updateTickSubscription()
    }

    override fun onDisconnected() {
        failDisconnectedRecipe()
    }

    // ========================================
    // Recipe gating
    // ========================================

    override fun beforeWorking(recipe: GTRecipe?): Boolean {
        if (isConnectedToHost) return true
        failDisconnectedRecipe()
        return false
    }

    private fun failDisconnectedRecipe() {
        getRecipeLogic().interruptRecipe()
        RecipeResult.of(this, FAIL_NOT_CONNECTED_TO_RECURSIVE_REVERSE_ARRAY)
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun onStructureFormed() {
        super.onStructureFormed()
        if (!findAndConnectToHost()) removeFromHost(host)
        updateArrayTickSubscription()
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        removeFromHost(host)
        updateArrayTickSubscription()
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(host)
    }

    override fun onMachineRemoved() {
        removeFromHost(host)
    }

    open fun moduleTick() = Unit

    private fun updateArrayTickSubscription() {
        if (isFormed) {
            arrayTickSubscription = subscribeServerTick(arrayTickSubscription, ::moduleArrayServerTick)
        } else {
            arrayTickSubscription?.unsubscribe()
            arrayTickSubscription = null
        }
    }

    private fun moduleArrayServerTick() {
        moduleTick()
        updateArrayTickSubscription()
    }

    // ========================================
    // GUI
    // ========================================

    override fun addDisplayText(textList: MutableList<Component>) {
        MultiblockDisplayText.builder(textList, isFormed())
            .setWorkingStatus(recipeLogic.isWorkingEnabled, recipeLogic.isActive)
            .addEnergyUsageLine(energyContainer)
            .addEnergyTierLine(tier)
            .addMachineModeLine(recipeType)
            .addWorkingStatusLine()
            .addProgressLine(recipeLogic.progressPercent)
            .addRecipeStatus(recipeLogic as IRecipeStatus)
            .addComponent(
                "gtladditions.machine.recursive_reverse_array.module.bound".toComponent(
                    Component.literal(if (isConnectedToHost) "✓" else "x")
                        .withStyle(if (isConnectedToHost) ChatFormatting.GREEN else ChatFormatting.RED)
                )
            )
    }

    // ========================================
    // Metadata
    // ========================================

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            RRFWorkableModuleMachine::class.java,
            WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER
        )

        val FAIL_NOT_CONNECTED_TO_RECURSIVE_REVERSE_ARRAY: RecipeResult = RecipeResult.fail(
            "gtladditions.recipe.fail.not_connected_to_recursive_reverse_array".toComponent
        )
    }
}