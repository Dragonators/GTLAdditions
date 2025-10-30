package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.logic.GTLAddMultipleRecipesLogic
import com.gtladd.gtladditions.api.machine.multiblock.GTLAddWorkableElectricMultipleRecipesMachine
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine
import org.gtlcore.gtlcore.utils.MachineUtil
import kotlin.math.pow

class AdvancedSpaceElevatorModuleMachine(holder: IMachineBlockEntity) :
    GTLAddWorkableElectricMultipleRecipesMachine(holder),
    IModularMachineModule<SpaceElevatorMachine, AdvancedSpaceElevatorModuleMachine>, IMachineLife {
    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder =
            ManagedFieldHolder(
                AdvancedSpaceElevatorModuleMachine::class.java,
                GTLAddWorkableElectricMultipleRecipesMachine.MANAGED_FIELD_HOLDER
            )
    }

    @field:DescSynced
    private var spaceElevatorTier = 0
    private var moduleTier = 0

    @field:Persisted
    private var hostPosition: BlockPos? = null
    private var host: SpaceElevatorMachine? = null

    override fun getHost(): SpaceElevatorMachine? {
        return host
    }

    override fun setHost(host: SpaceElevatorMachine?) {
        this.host = host
    }

    override fun getHostType(): Class<SpaceElevatorMachine> {
        return SpaceElevatorMachine::class.java
    }

    override fun getHostPosition(): BlockPos? {
        return hostPosition
    }

    override fun setHostPosition(pos: BlockPos?) {
        this.hostPosition = pos
    }

    override fun getFieldHolder(): ManagedFieldHolder {
        return MANAGED_FIELD_HOLDER
    }

    // ========================================
    // Update subscription for 80tick limit
    // ========================================

    private var updateSubs: TickableSubscription? = null
    private var remainingTicks: Int = 0

    private fun updateSubscription() {
        if (--this.remainingTicks > 0) {
            updateSubs = subscribeServerTick(updateSubs) { updateSubscription() }
        } else {
            update()
            updateSubs?.unsubscribe()
            updateSubs = null
        }
    }

    private fun update() {
        getSpaceElevatorTier()
        recipeLogic.updateTickSubscription()
    }

    private fun scheduleUpdate() {
        updateSubs?.unsubscribe()
        remainingTicks = 90
        updateSubscription()
    }

    // ========================================
    // Elevator connection
    // ========================================

    override fun onConnected(host: SpaceElevatorMachine) {
        if (host.recipeLogic.progress > 80) update() else scheduleUpdate()
    }

    override fun getHostScanPositions(): Array<out BlockPos?>? {
        (level as? ServerLevel)?.let { serverLevel ->
            val pos = getPos()
            val coordinates: Array<BlockPos> = arrayOf(
                pos.offset(8, -2, 3),
                pos.offset(8, -2, -3),
                pos.offset(-8, -2, 3),
                pos.offset(-8, -2, -3),
                pos.offset(3, -2, 8),
                pos.offset(-3, -2, 8),
                pos.offset(3, -2, -8),
                pos.offset(-3, -2, -8)
            )

            for (i in coordinates) {
                if (serverLevel.getBlockState(i).block === GTLBlocks.POWER_CORE.get()) {
                    return arrayOf(
                        i.offset(3, 2, 0),
                        i.offset(-3, 2, 0),
                        i.offset(0, 2, 3),
                        i.offset(0, 2, -3)
                    )
                }
            }
        }
        return MachineUtil.EMPTY_POS_ARRAY
    }

    private fun getSpaceElevatorTier() {
        host?.let { controller ->
            val logic = controller.getRecipeLogic()
            if (logic.isWorking && logic.progress > 80) {
                spaceElevatorTier = controller.tier - 7
                moduleTier = controller.casingTier
            } else {
                spaceElevatorTier = 0
                moduleTier = 0
            }
            return
        }
        spaceElevatorTier = 0
        moduleTier = 0
    }

    override fun createRecipeLogic(vararg args: Any): RecipeLogic {
        return GTLAddMultipleRecipesLogic(
            this
        ) { machine -> (machine as AdvancedSpaceElevatorModuleMachine).host != null }
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
            textList.add(Component.translatable((if (this.spaceElevatorTier < 1) "未" else "已") + "连接正在运行的太空电梯"))
        }
    }

    override fun getMaxParallel(): Int {
        return 8.0.pow((this.moduleTier - 1).toDouble()).toInt()
    }
}
