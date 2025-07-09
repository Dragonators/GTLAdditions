package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.ILimitedDuration
import com.gtladd.gtladditions.api.machine.gui.LimitedDurationConfigurator
import com.gtladd.gtladditions.api.recipeslogic.GTLAddMultipleRecipesLogic
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.ParallelMachine
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine
import kotlin.math.pow

class AdvancedSpaceElevatorModuleMachine(holder: IMachineBlockEntity, private val SEPMTier: Boolean) :
    WorkableElectricMultiblockMachine(holder), ParallelMachine, ILimitedDuration {
    private var limitedDuration = 20
    private var SpaceElevatorTier = 0
    private var ModuleTier = 0
    private var controller : SpaceElevatorMachine? = null

    public override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleRecipesLogic(this)
    }

    private fun getSpaceElevatorTier() {
        if (controller != null) {
            val logic : RecipeLogic? = controller !!.recipeLogic
            if (logic != null) {
                if (logic.isWorking && logic.getProgress() > 80) {
                    SpaceElevatorTier = controller !!.tier - 7
                    ModuleTier = controller !!.casingTier
                } else if (!logic.isWorking || !controller!!.isFormed) {
                    SpaceElevatorTier = 0
                    ModuleTier = 0
                }
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
                        if (logic != null && logic.getMachine()
                                .definition === AdvancedMultiBlockMachine.SPACE_ELEVATOR
                        ) {
                            if (logic.isWorking && logic.getProgress() > 80) {
                                this.SpaceElevatorTier = (logic.machine as SpaceElevatorMachine).tier - 7
                                this.ModuleTier = (logic.machine as SpaceElevatorMachine).casingTier
                            } else if (!logic.isWorking) {
                                this.SpaceElevatorTier = 0
                                this.ModuleTier = 0
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

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        tag.putInt("drLimit", limitedDuration)
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        limitedDuration = tag.getInt("drLimit")
    }

    override fun onWorking(): Boolean {
        val value = super.onWorking()
        if (this.offsetTimer % 10L == 0L) {
            this.getSpaceElevatorTier()
            if (this.SpaceElevatorTier < 1) {
                this.getRecipeLogic().interruptRecipe()
                return false
            }
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
            textList.add(Component.translatable((if (this.SpaceElevatorTier < 1) "未" else "已") + "连接正在运行的太空电梯"))
        }
    }

    override fun getMaxParallel(): Int {
        return 8.0.pow((this.ModuleTier - 1).toDouble()).toInt()
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

    companion object {
        @JvmStatic
        fun beforeWorking(machine: IRecipeLogicMachine?, recipe: GTRecipe): Boolean {
            if (machine is AdvancedSpaceElevatorModuleMachine) {
                machine.getSpaceElevatorTier()
                if (machine.SpaceElevatorTier < 1) return false
                return !machine.SEPMTier || recipe.data.getInt("SEPMTier") <= machine.ModuleTier
            }
            return false
        }
    }
}
