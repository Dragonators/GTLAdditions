package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.api.machine.logic.UpkeepRecipeLogic
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKI
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKII
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIII
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine.NEXUS_SATELLITE_FACTORY_MKIV
import com.gtladd.gtladditions.common.machine.multiblock.controller.module.LightHunterSpaceStationModuleBase
import com.gtladd.gtladditions.common.machine.trait.LightHunterParadoxEffectsTrait
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.LightHunterInteriorLighting
import com.gtladd.gtladditions.utils.module.LightHunterSpaceStationPosHelper
import com.gtladd.gtladditions.utils.module.ModuleConnectionRegistry
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import org.gtlcore.gtlcore.api.machine.IGTLRenderDisabled
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

    private var registeredSlots: Array<BlockPos>? = null

    @field:Persisted
    @field:DescSynced
    override var astralArrayCount: Int = 0

    @field:DescSynced
    val paradoxEffects = LightHunterParadoxEffectsTrait(this)
    private var mam = 0
    private var interiorLightSubscription: TickableSubscription? = null
    private var interiorLightingPending = false

    // ========================================
    // Recipe logic
    // ========================================

    @Suppress("unused")
    fun supportsBatchProcessing(): Boolean = false

    @Suppress("unused")
    fun canConfigureBatchProcessing(): Boolean = false

    fun unlockParadoxical(): Boolean = astralArrayCount == MAX_ASTRAL_ARRAY_COUNT

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = UpkeepRecipeLogic(this, UPKEEP_RECIPE)

    override fun notifyStatusChanged(oldStatus: RecipeLogic.Status, newStatus: RecipeLogic.Status) {
        super.notifyStatusChanged(oldStatus, newStatus)
        if (newStatus == RecipeLogic.Status.WORKING) {
            modules.forEach { it.onConnected(this) }
        }
    }

    override fun increaseAstralArrayCount(amount: Int): Int {
        val actualIncrease = minOf(amount, MAX_ASTRAL_ARRAY_COUNT - astralArrayCount)
        if (actualIncrease > 0) {
            astralArrayCount += actualIncrease
            if (unlockParadoxical()) paradoxEffects.notifyParadoxUnlocked()
        }
        return actualIncrease
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun onStructureFormed() {
        super.onStructureFormed()
        val world = level as? ServerLevel ?: return
        if (registeredSlots == null) {
            registeredSlots = moduleScanPositions.also { MODULE_CONNECTIONS.register(world.dimension(), this, it) }
        }

        scanAndConnectModules()
        startInteriorLighting()
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        stopInteriorLighting()
        releaseSlots()
    }

    override fun onPartUnload() {
        super.onPartUnload()
        releaseSlots()
        interiorLightSubscription?.unsubscribe()
        interiorLightSubscription = null
        interiorLightingPending = false
    }

    override fun onMachineRemoved() {
        stopInteriorLighting()
        releaseSlots()
    }

    override fun onLoad() {
        super.onLoad()
        paradoxEffects.initializeState(unlockParadoxical())
        if (isFormed) startInteriorLighting()
    }

    override fun onUnload() {
        releaseSlots()
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

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<LightHunterSpaceStation, *>> = modules

    override fun getModuleScanPositions(): Array<BlockPos> = LightHunterSpaceStationPosHelper.calculateModulePositions(pos, frontFacing)

    fun tryConnectModule(slot: Int, module: LightHunterSpaceStationModuleBase): Boolean {
        if (!isFormed || !module.isFormed || !module.isRecipeLogicAvailable || registeredSlots?.getOrNull(slot) != module.pos) return false
        val previous = module.getHost()
        if (previous != null && previous !== this && previous.isFormed) return false
        if (previous !== this) module.connectToHost(this)
        return true
    }

    override fun scanAndConnectModules() {
        val world = level as? ServerLevel ?: return
        registeredSlots?.forEachIndexed { slot, pos ->
            MODULE_CONNECTIONS.moduleAt(world.dimension(), pos)?.let { tryConnectModule(slot, it) }
        }
    }

    private fun releaseSlots() {
        val slots = registeredSlots ?: return
        (level as? ServerLevel)?.let { MODULE_CONNECTIONS.unregister(it.dimension(), this, slots) }
        registeredSlots = null
        safeClearModules()
    }

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

    // ========================================
    // GUI
    // ========================================

    private fun getMAM(): Int = mam.also {
        if (offsetTimer % 20 == 0L) mam = formedModuleCount
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

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        IGTLRenderDisabled.attachConfigurator(configuratorPanel, this as IGTLRenderDisabled)
    }

    // ========================================
    // Metadata
    // ========================================

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MODULE_CONNECTIONS = ModuleConnectionRegistry<LightHunterSpaceStation, LightHunterSpaceStationModuleBase>()

        private const val MAX_ASTRAL_ARRAY_COUNT = 64
        val UPKEEP_EUT: Long = GTValues.V[GTValues.UXV]
        private val UPKEEP_RECIPE = GTRecipeBuilder.ofRaw().EUt(UPKEEP_EUT).duration(UpkeepRecipeLogic.CYCLE_TICKS).buildRawRecipe()
        private val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            LightHunterSpaceStation::class.java,
            WorkableMultiblockMachine.MANAGED_FIELD_HOLDER
        )
    }
}
