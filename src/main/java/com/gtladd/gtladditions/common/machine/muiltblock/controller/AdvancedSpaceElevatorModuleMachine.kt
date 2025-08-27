package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.GTLAddWorkableElectricMultipleRecipesMachine
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine
import kotlin.math.pow

class AdvancedSpaceElevatorModuleMachine(holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder) {
        companion object {
            val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
                ManagedFieldHolder(AdvancedSpaceElevatorModuleMachine::class.java, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER)
        }
    private var spaceElevatorTier = 0
    private var moduleTier = 0
    private var controller : BlockPos? = null

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    private fun getSpaceElevatorTier() {
        if (controller != null) {
            val logic = GTCapabilityHelper.getRecipeLogic(level!!, controller, null)
            if (logic != null && logic.getMachine().definition === AdvancedMultiBlockMachine.SPACE_ELEVATOR) {
                if (logic.isWorking && logic.getProgress() > 80) {
                    spaceElevatorTier = (logic.machine as SpaceElevatorMachine).tier - 7
                    moduleTier = (logic.machine as SpaceElevatorMachine).casingTier
                } else if (!logic.isWorking) {
                    spaceElevatorTier = 0
                    moduleTier = 0
                }
            } else if (logic == null) {
                spaceElevatorTier = 0
                moduleTier = 0
            }
        } else {
            val level = this.level
            val pos = this.pos
            val coordinates: Array<BlockPos?> = arrayOf<BlockPos>(
                pos.offset(8, -2, 3),
                pos.offset(8, -2, -3),
                pos.offset(-8, -2, 3),
                pos.offset(-8, -2, -3),
                pos.offset(3, -2, 8),
                pos.offset(-3, -2, 8),
                pos.offset(3, -2, -8),
                pos.offset(-3, -2, -8)
            ) as Array<BlockPos?>
            for (i in coordinates) {
                if (level != null && level.getBlockState(i).block === GTLBlocks.POWER_CORE.get()) {
                    val coordinatess: Array<BlockPos?> = arrayOf<BlockPos>(
                        i!!.offset(3, 2, 0),
                        i.offset(-3, 2, 0),
                        i.offset(0, 2, 3),
                        i.offset(0, 2, -3)
                    ) as Array<BlockPos?>
                    for (j in coordinatess) {
                        val logic = GTCapabilityHelper.getRecipeLogic(level, j, null)
                        if (logic != null && logic.getMachine().definition === AdvancedMultiBlockMachine.SPACE_ELEVATOR) {
                            controller = j
                            if (logic.isWorking && logic.getProgress() > 80) {
                                this.spaceElevatorTier = (logic.machine as SpaceElevatorMachine).tier - 7
                                this.moduleTier = (logic.machine as SpaceElevatorMachine).casingTier
                            } else if (!logic.isWorking) {
                                this.spaceElevatorTier = 0
                                this.moduleTier = 0
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        getSpaceElevatorTier()
    }

    override fun onWorking(): Boolean {
        val value = super.onWorking()
        if (this.offsetTimer % 10L == 0L) {
            this.getSpaceElevatorTier()
            if (this.spaceElevatorTier < 1) getRecipeLogic().setProgress(0)
        }
        return value
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (this.isFormed) {
            if (this.offsetTimer % 10L == 0L) this.getSpaceElevatorTier()
            textList.add(
                Component.translatable("gtceu.multiblock.parallel", Component.translatable(FormattingUtil.formatNumbers(maxParallel))
                        .withStyle(ChatFormatting.DARK_PURPLE))
                    .withStyle(ChatFormatting.GRAY)
            )
            textList.add(Component.translatable((if (this.spaceElevatorTier < 1) "未" else "已") + "连接正在运行的太空电梯"))
        }
    }

    override fun getMaxParallel(): Int {
        return 8.0.pow((this.moduleTier - 1).toDouble()).toInt()
    }

}
