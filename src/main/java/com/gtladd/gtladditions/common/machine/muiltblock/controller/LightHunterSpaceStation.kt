package com.gtladd.gtladditions.common.machine.muiltblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKI
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKII
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIII
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIV
import com.gtladd.gtladditions.utils.LightHunterSpaceStationPosHelper
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo

class LightHunterSpaceStation(holder: IMachineBlockEntity, vararg args: Any?) :
    WorkableElectricMultiblockMachine(holder, *args), IModularMachineHost<LightHunterSpaceStation>,
    IAstralArrayInteractionMachine, IMachineLife {
    private val modules: MutableSet<IModularMachineModule<LightHunterSpaceStation, *>> =
        ReferenceOpenHashSet<IModularMachineModule<LightHunterSpaceStation, *>>()

    @field:Persisted
    @field:DescSynced
    override var astralArrayCount: Int = 0
    private var mam = 0

    private fun getMAM(): Int = mam.also {
        if (offsetTimer % 20 == 0L) mam = formedModuleCount
    }

    fun unlockParadoxical(): Boolean = astralArrayCount == MAX_ASTRAL_ARRAY_COUNT

    override fun increaseAstralArrayCount(amount: Int): Int {
        val actualIncrease = minOf(amount, MAX_ASTRAL_ARRAY_COUNT - astralArrayCount)
        if (actualIncrease > 0) {
            astralArrayCount += actualIncrease
        }
        return actualIncrease
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!isFormed) return
        textList.add(
            if (unlockParadoxical()) GTLAddMachines.createRainbowComponent(
                Component.translatable("tooltip.gtladditions.industrial_array_max").string
            ) else Component.translatable(
                "tooltip.gtladditions.astral_array_count",
                Component.literal("$astralArrayCount / 64").withStyle(ChatFormatting.GOLD)
            )
        )
        textList.add(Component.translatable("tooltip.gtlcore.installed_module_count", getMAM()))
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<LightHunterSpaceStation, *>> = modules

    override fun getModuleScanPositions(): Array<out BlockPos> = LightHunterSpaceStationPosHelper.calculateModulePositions(pos, frontFacing)

    override fun getModulesForRendering(): List<ModuleRenderInfo> {
        return listOf(
            ModuleRenderInfo(
                BlockPos(-41, 0, -9),
                Direction.EAST,
                Direction.UP,
                Direction.SOUTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKI
            ),
            ModuleRenderInfo(
                BlockPos(-49, 0, -9),
                Direction.EAST,
                Direction.UP,
                Direction.SOUTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKI
            ),
            ModuleRenderInfo(
                BlockPos(-57, 0, -9),
                Direction.EAST,
                Direction.UP,
                Direction.SOUTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKII
            ),
            ModuleRenderInfo(
                BlockPos(-65, 0, -9),
                Direction.EAST,
                Direction.UP,
                Direction.SOUTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKI
            ),
            ModuleRenderInfo(
                BlockPos(-73, 0, -9),
                Direction.EAST,
                Direction.UP,
                Direction.SOUTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIV
            ),
            ModuleRenderInfo(
                BlockPos(-81, 0, -9),
                Direction.EAST,
                Direction.UP,
                Direction.SOUTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKII
            ),
            ModuleRenderInfo(
                BlockPos(-89, 0, -9),
                Direction.EAST,
                Direction.UP,
                Direction.SOUTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKI
            ),
            ModuleRenderInfo(
                BlockPos(-97, 0, -9),
                Direction.EAST,
                Direction.UP,
                Direction.SOUTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKI
            ),
            ModuleRenderInfo(
                BlockPos(-105, 0, -9),
                Direction.EAST,
                Direction.UP,
                Direction.SOUTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKII
            ),
            ModuleRenderInfo(
                BlockPos(-113, 0, -9),
                Direction.EAST,
                Direction.UP,
                Direction.SOUTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIV
            ),
            ModuleRenderInfo(
                BlockPos(-113, 0, 9),
                Direction.EAST,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKII
            ),
            ModuleRenderInfo(
                BlockPos(-105, 0, 9),
                Direction.EAST,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIV
            ),
            ModuleRenderInfo(
                BlockPos(-97, 0, 9),
                Direction.EAST,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKI
            ),
            ModuleRenderInfo(
                BlockPos(-89, 0, 9),
                Direction.EAST,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIV
            ),
            ModuleRenderInfo(
                BlockPos(-81, 0, 9),
                Direction.EAST,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIII
            ),
            ModuleRenderInfo(
                BlockPos(-73, 0, 9),
                Direction.EAST,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKII
            ),
            ModuleRenderInfo(
                BlockPos(-65, 0, 9),
                Direction.EAST,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIV
            ),
            ModuleRenderInfo(
                BlockPos(-57, 0, 9),
                Direction.EAST,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIII
            ),
            ModuleRenderInfo(
                BlockPos(-49, 0, 9),
                Direction.EAST,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKI
            ),
            ModuleRenderInfo(
                BlockPos(-41, 0, 9),
                Direction.EAST,
                Direction.UP,
                Direction.NORTH,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIII
            )
        )
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        safeClearModules()
    }

    override fun onMachineRemoved() {
        safeClearModules()
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        safeClearModules()
        scanAndConnectModules()
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        private const val MAX_ASTRAL_ARRAY_COUNT = 64
        private val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            LightHunterSpaceStation::class.java,
            WorkableMultiblockMachine.MANAGED_FIELD_HOLDER
        )
    }
}