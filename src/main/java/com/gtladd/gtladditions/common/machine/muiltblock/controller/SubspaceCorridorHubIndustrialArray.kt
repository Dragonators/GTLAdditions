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
import com.gtladd.gtladditions.utils.IndustrialArrayPosHelper
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

class SubspaceCorridorHubIndustrialArray(holder: IMachineBlockEntity, vararg args: Any?) :
    WorkableElectricMultiblockMachine(holder, *args), IModularMachineHost<SubspaceCorridorHubIndustrialArray>,
    IAstralArrayInteractionMachine, IMachineLife {
    private val modules: MutableSet<IModularMachineModule<SubspaceCorridorHubIndustrialArray, *>> =
        ReferenceOpenHashSet<IModularMachineModule<SubspaceCorridorHubIndustrialArray, *>>()

    @field:Persisted
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
                Component.literal("$astralArrayCount / 512").withStyle(ChatFormatting.GOLD)
            )
        )
        textList.add(Component.translatable("tooltip.gtlcore.installed_module_count", getMAM()))
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<SubspaceCorridorHubIndustrialArray, *>> = modules

    override fun getModuleScanPositions(): Array<out BlockPos?>? = IndustrialArrayPosHelper.calculateModulePositions(pos, frontFacing)

    override fun getModulesForRendering(): List<ModuleRenderInfo?> {
        return listOf(
            ModuleRenderInfo(
                BlockPos(-1, -100, -29),
                Direction.EAST,
                Direction.UP,
                Direction.EAST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKI
            ),
            ModuleRenderInfo(
                BlockPos(-2, -100, -31),
                Direction.EAST,
                Direction.UP,
                Direction.EAST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKII
            ),
            ModuleRenderInfo(
                BlockPos(-4, -100, -37),
                Direction.EAST,
                Direction.UP,
                Direction.EAST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIII
            ),
            ModuleRenderInfo(
                BlockPos(-12, -100, -41),
                Direction.EAST,
                Direction.UP,
                Direction.WEST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIV
            ),
            ModuleRenderInfo(
                BlockPos(-10, -100, -37),
                Direction.EAST,
                Direction.UP,
                Direction.WEST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIII
            ),
            ModuleRenderInfo(
                BlockPos(-9, -100, -33),
                Direction.EAST,
                Direction.UP,
                Direction.WEST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKI
            ),
            ModuleRenderInfo(
                BlockPos(-8, -100, 31),
                Direction.EAST,
                Direction.UP,
                Direction.WEST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKII
            ),
            ModuleRenderInfo(
                BlockPos(-10, -100, 37),
                Direction.EAST,
                Direction.UP,
                Direction.WEST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIII
            ),
            ModuleRenderInfo(
                BlockPos(-12, -100, 41),
                Direction.EAST,
                Direction.UP,
                Direction.WEST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIV
            ),
            ModuleRenderInfo(
                BlockPos(-7, -100, 43),
                Direction.EAST,
                Direction.UP,
                Direction.EAST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKIII
            ),
            ModuleRenderInfo(
                BlockPos(-4, -100, 37),
                Direction.EAST,
                Direction.UP,
                Direction.EAST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKI
            ),
            ModuleRenderInfo(
                BlockPos(-1, -100, 29),
                Direction.EAST,
                Direction.UP,
                Direction.EAST,
                Direction.UP,
                NEXUS_SATELLITE_FACTORY_MKII
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
        private const val MAX_ASTRAL_ARRAY_COUNT = 512
        private val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            SubspaceCorridorHubIndustrialArray::class.java,
            WorkableMultiblockMachine.MANAGED_FIELD_HOLDER
        )
    }
}