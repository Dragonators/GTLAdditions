package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.api.machine.ITimeSpaceDistorterSource
import com.gtladd.gtladditions.api.machine.logic.ApocalypticMatrixRecipeLogic
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleRecipesMachine
import com.gtladd.gtladditions.api.machine.wireless.GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine
import com.gtladd.gtladditions.common.data.StructureRenderType
import com.gtladd.gtladditions.common.machine.trait.MatrixQuantumWarpTrait
import com.gtladd.gtladditions.common.machine.trait.MatrixQuantumWarpTrait.Companion.MAX_ARRAYS
import com.gtladd.gtladditions.common.machine.trait.StructureModifyTrait
import com.gtladd.gtladditions.common.machine.trait.WorkingClockEffectsTrait
import com.gtladd.gtladditions.common.recipe.GTLAddRecipesTypes
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class ApocalypticWarpQuantumMatrix(holder: IMachineBlockEntity, vararg args: Any?) :
    GTLAddWirelessWorkableElectricMultipleTypeRecipesMachine(
        holder,
        GTLAddRecipesTypes.QUANTUM_OSCILLATION,
        *args
    ),
    ITimeSpaceDistorterSource,
    IAstralArrayInteractionMachine {

    @field:Persisted
    override var timeSpaceDistorterPos: BlockPos? = null

    override var timeSpaceDistorter: TimeSpaceDistorter? = null

    private val structureModify = StructureModifyTrait(this, StructureRenderType.APOCALYPTIC_MATRIX)

    @field:Persisted
    override var astralArrayCount = 0
        private set

    @field:DescSynced
    val matrixEffects = WorkingClockEffectsTrait(this)

    @field:Persisted
    val quantumWarpTrait = MatrixQuantumWarpTrait(this)

    override fun increaseAstralArrayCount(amount: Int): Int {
        val installed = amount.coerceIn(0, MAX_ARRAYS - astralArrayCount)
        astralArrayCount += installed
        return installed
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (isFormed) quantumWarpTrait.addDisplayText(textList)
    }

    override fun addParallelDisplay(textList: MutableList<Component?>) {
        quantumWarpTrait.addParallelDisplay(textList)
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        quantumWarpTrait.attachConfigurators(configuratorPanel)
    }

    override fun attachSideTabs(tabs: TabsWidget) {
        super.attachSideTabs(tabs)
        quantumWarpTrait.attachSideTabs(tabs)
    }

    // ========================================
    // Core overrides
    // ========================================

    override fun getMaxParallel(): Int = Int.MAX_VALUE

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = ApocalypticMatrixRecipeLogic(this)

    override fun getRecipeLogic(): ApocalypticMatrixRecipeLogic = super.getRecipeLogic() as ApocalypticMatrixRecipeLogic

    override fun needConfirmMEStock(): Boolean = true

    // ========================================
    // Wireless binding
    // ========================================

    override val bindingType: ResourceLocation = BINDING_TYPE

    // ========================================
    // Lifecycle
    // ========================================

    override fun checkPattern(): Boolean = structureModify.checkPattern()

    override fun onStructureFormed() {
        super.onStructureFormed()
        structureModify.form()
        reconnectTarget()
        matrixEffects.updateAnimation()
    }

    override fun onStructureInvalid() {
        matrixEffects.updateAnimation(false)
        super.onStructureInvalid()
        structureModify.invalidate()
        detachTarget()
    }

    override fun onMachineRemoved() {
        structureModify.restore()
        unbindTarget()
    }

    override fun onUnload() {
        super.onUnload()
        detachTarget()
    }

    override fun notifyStatusChanged(oldStatus: RecipeLogic.Status, newStatus: RecipeLogic.Status) {
        super.notifyStatusChanged(oldStatus, newStatus)
        matrixEffects.updateAnimation(newStatus == RecipeLogic.Status.WORKING)
    }

    override fun onWorking(): Boolean {
        quantumWarpTrait.onWorking()
        return super.onWorking()
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    // ========================================
    // Metadata
    // ========================================

    companion object {
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            ApocalypticWarpQuantumMatrix::class.java,
            GTLAddWirelessWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
        )
        val BINDING_TYPE: ResourceLocation = ResourceLocation("gtladditions", "apocalyptic_quantum_warp_matrix")
    }
}