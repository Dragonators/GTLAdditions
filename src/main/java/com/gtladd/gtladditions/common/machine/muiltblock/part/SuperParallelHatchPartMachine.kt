package com.gtladd.gtladditions.common.machine.muiltblock.part

import com.gregtechceu.gtceu.api.capability.IParallelHatch
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gtladd.gtladditions.api.gui.widget.SafeIntInputWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.util.Mth

class SuperParallelHatchPartMachine(holder: IMachineBlockEntity) : MultiblockPartMachine(holder), IFancyUIMachine,
    IParallelHatch {
    @field:Persisted
    private var currentParallel: Int = MAX_PARALLEL

    fun setCurrentParallel(parallelAmount: Int) {
        this.currentParallel = Mth.clamp(parallelAmount, MIN_PARALLEL, MAX_PARALLEL)

        for (controller in this.controllers) {
            if (controller is IRecipeLogicMachine) {
                controller.recipeLogic.markLastRecipeDirty()
            }
        }
    }

    override fun createUIWidget(): Widget {
        val parallelAmountGroup = WidgetGroup(0, 0, 100, 20)
        parallelAmountGroup.addWidget(
            (SafeIntInputWidget(
                { this.currentParallel },
                { parallelAmount: Int? ->
                    this.setCurrentParallel(
                        parallelAmount!!
                    )
                })).setMin(MIN_PARALLEL).setMax(MAX_PARALLEL)
        )
        return parallelAmountGroup
    }

    override fun canShared(): Boolean = true

    override fun getCurrentParallel(): Int = this.currentParallel

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            SuperParallelHatchPartMachine::class.java,
            MultiblockPartMachine.MANAGED_FIELD_HOLDER
        )
        private const val MAX_PARALLEL = Int.MAX_VALUE
        private const val MIN_PARALLEL = 1
    }
}
