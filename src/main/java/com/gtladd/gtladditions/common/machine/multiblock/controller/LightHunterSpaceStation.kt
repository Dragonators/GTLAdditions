package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKI
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKII
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIII
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIV
import com.gtladd.gtladditions.common.machine.trait.LightHunterParadoxEffectsTrait
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.LightHunterInteriorLighting
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
    WorkableElectricMultiblockMachine(holder, *args),
    IModularMachineHost<LightHunterSpaceStation>,
    IAstralArrayInteractionMachine,
    IMachineLife {
    private val modules =
        ReferenceOpenHashSet<IModularMachineModule<LightHunterSpaceStation, *>>()

    @field:Persisted
    @field:DescSynced
    override var astralArrayCount: Int = 0

    @field:DescSynced
    val paradoxEffects = LightHunterParadoxEffectsTrait(this)
    private var mam = 0
    private var interiorLightSubscription: TickableSubscription? = null
    private var interiorLightingPending = false

    @Suppress("unused")
    fun supportsBatchProcessing(): Boolean = false

    @Suppress("unused")
    fun canConfigureBatchProcessing(): Boolean = false

    private fun getMAM(): Int = mam.also {
        if (offsetTimer % 20 == 0L) mam = formedModuleCount
    }

    fun unlockParadoxical(): Boolean = astralArrayCount == MAX_ASTRAL_ARRAY_COUNT

    override fun increaseAstralArrayCount(amount: Int): Int {
        val actualIncrease = minOf(amount, MAX_ASTRAL_ARRAY_COUNT - astralArrayCount)
        if (actualIncrease > 0) {
            astralArrayCount += actualIncrease
            if (unlockParadoxical()) paradoxEffects.notifyParadoxUnlocked()
        }
        return actualIncrease
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!isFormed) return
        textList.add(
            if (unlockParadoxical()) {
                createLanguageRainbowComponentOnServer(
                    "tooltip.gtladditions.industrial_array_max".toComponent
                )
            } else {
                "tooltip.gtladditions.astral_array_count".toComponent("$astralArrayCount / $MAX_ASTRAL_ARRAY_COUNT".literal.withStyle(ChatFormatting.GOLD))
            }
        )
        textList.add("tooltip.gtlcore.installed_module_count".toComponent(getMAM()))
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<LightHunterSpaceStation, *>> = modules

    override fun getModuleScanPositions(): Array<out BlockPos> = LightHunterSpaceStationPosHelper.calculateModulePositions(pos, frontFacing)

    override fun getModulesForRendering(): List<ModuleRenderInfo> = listOf(
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

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        stopInteriorLighting()
        safeClearModules()
    }

    override fun onMachineRemoved() {
        stopInteriorLighting()
        safeClearModules()
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        safeClearModules()
        scanAndConnectModules()
        startInteriorLighting()
    }

    override fun onLoad() {
        super.onLoad()
        paradoxEffects.initializeState(unlockParadoxical())
        if (isFormed) startInteriorLighting()
    }

    override fun onUnload() {
        interiorLightSubscription?.unsubscribe()
        interiorLightSubscription = null
        interiorLightingPending = false
        super.onUnload()
    }

    private fun startInteriorLighting() {
        if (level?.isClientSide != false) return
        // onLoad also runs inside chunk post-loading. Reading another chunk there can wait
        // on the unfinished load that called us; only register work for a regular server tick.
        interiorLightingPending = true
        interiorLightSubscription = subscribeServerTick(interiorLightSubscription) {
            if (!isFormed) {
                stopInteriorLighting()
            } else if (interiorLightingPending || offsetTimer % 100L == 0L) {
                interiorLightingPending = false
                LightHunterInteriorLighting.update(this, true)
            }
        }
    }

    private fun stopInteriorLighting() {
        interiorLightSubscription?.unsubscribe()
        interiorLightSubscription = null
        interiorLightingPending = false
        LightHunterInteriorLighting.update(this, false)
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