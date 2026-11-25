package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKI
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKII
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIII
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIV
import com.gtladd.gtladditions.common.machine.multiblock.controller.module.SubspaceCorridorHubIndustrialArrayModuleBase
import com.gtladd.gtladditions.common.machine.trait.NexusEffectsTrait
import com.gtladd.gtladditions.utils.SubspaceNexusLayout
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo

class SubspaceCorridorNexusIndustrialArray(
    holder: IMachineBlockEntity,
    vararg args: Any?
) : SubspaceCorridorHubIndustrialArray(holder, *args) {
    @field:DescSynced
    var moduleConnections = LongArray(3)
        private set

    @field:DescSynced
    var moduleActivity = LongArray(3)
        private set

    @field:Persisted
    @field:DescSynced
    val nexusEffects = NexusEffectsTrait(this)

    override fun getModuleScanPositions(): Array<BlockPos> = SubspaceNexusLayout.modulePositions(pos, frontFacing)

    override fun acceptsModuleInSlot(slot: Int, module: SubspaceCorridorHubIndustrialArrayModuleBase): Boolean =
        module.frontFacing == SubspaceNexusLayout.moduleFacing(frontFacing, slot)

    fun onModuleConnected(module: SubspaceCorridorHubIndustrialArrayModuleBase) =
        setModuleSignals(module, connected = true, working = module.recipeLogic.isWorking)

    fun onModuleDisconnected(module: SubspaceCorridorHubIndustrialArrayModuleBase) =
        setModuleSignals(module, connected = false, working = false)

    fun onModuleActivityChanged(module: SubspaceCorridorHubIndustrialArrayModuleBase, working: Boolean) =
        setModuleSignals(module, connected = true, working = working)

    private fun setModuleSignals(module: SubspaceCorridorHubIndustrialArrayModuleBase, connected: Boolean, working: Boolean) {
        val index = SubspaceNexusLayout.moduleIndex(pos, frontFacing, module.pos)
        if (index < 0) return

        val word = index / 64
        val bit = 1L shl (index % 64)
        fun update(bits: LongArray, value: Boolean): LongArray {
            val next = if (value) bits[word] or bit else bits[word] and bit.inv()
            return if (next == bits[word]) bits else bits.copyOf().also { it[word] = next }
        }

        val connections = update(moduleConnections, connected)
        val activity = update(moduleActivity, connected && working)
        if (connections !== moduleConnections) moduleConnections = connections
        if (activity !== moduleActivity) moduleActivity = activity
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        nexusEffects.wake()
    }

    override fun notifyStatusChanged(oldStatus: RecipeLogic.Status, newStatus: RecipeLogic.Status) {
        super.notifyStatusChanged(oldStatus, newStatus)
        if ((oldStatus == RecipeLogic.Status.WORKING) != (newStatus == RecipeLogic.Status.WORKING)) nexusEffects.wake()
    }

    override fun increaseAstralArrayCount(amount: Int): Int {
        val wasUnlocked = unlockParadoxical()
        val installed = super.increaseAstralArrayCount(amount)
        if (!wasUnlocked && unlockParadoxical()) nexusEffects.onParadoxUnlocked()
        return installed
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        nexusEffects.wake()
    }

    override fun getModulesForRendering(): List<ModuleRenderInfo> {
        val definitions = listOf(
            NEXUS_SATELLITE_FACTORY_MKI,
            NEXUS_SATELLITE_FACTORY_MKII,
            NEXUS_SATELLITE_FACTORY_MKIII,
            NEXUS_SATELLITE_FACTORY_MKIV
        )

        return SubspaceNexusLayout.modulePositions(BlockPos.ZERO, Direction.EAST).mapIndexed { index, offset ->
            ModuleRenderInfo(
                offset,
                Direction.EAST,
                Direction.UP,
                SubspaceNexusLayout.moduleFacing(Direction.EAST, index),
                Direction.UP,
                definitions[index % definitions.size]
            )
        }
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private val MANAGED_FIELD_HOLDER = ManagedFieldHolder(
            SubspaceCorridorNexusIndustrialArray::class.java,
            SubspaceCorridorHubIndustrialArray.MANAGED_FIELD_HOLDER
        )
    }
}
