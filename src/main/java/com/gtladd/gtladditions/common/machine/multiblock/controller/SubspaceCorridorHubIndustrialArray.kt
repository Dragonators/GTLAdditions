package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
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
import com.gtladd.gtladditions.common.machine.multiblock.controller.module.SubspaceCorridorHubIndustrialArrayModuleBase
import com.gtladd.gtladditions.utils.CommonUtils.createLanguageRainbowComponentOnServer
import com.gtladd.gtladditions.utils.IndustrialArrayPosHelper
import com.gtladd.gtladditions.utils.module.ModuleConnectionRegistry
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

open class SubspaceCorridorHubIndustrialArray(holder: IMachineBlockEntity, vararg args: Any?) :
    WorkableElectricMultiblockMachine(holder, *args),
    IModularMachineHost<SubspaceCorridorHubIndustrialArray>,
    IAstralArrayInteractionMachine,
    IMachineLife {
    private val modules = ReferenceOpenHashSet<IModularMachineModule<SubspaceCorridorHubIndustrialArray, *>>()

    private var registeredSlots: Array<BlockPos>? = null

    @field:Persisted
    override var astralArrayCount: Int = 0
    private var mam = 0

    @Suppress("unused")
    fun supportsBatchProcessing(): Boolean = false

    @Suppress("unused")
    fun canConfigureBatchProcessing(): Boolean = false

    private fun getMAM(): Int = mam.also {
        if (offsetTimer % 20 == 0L) mam = formedModuleCount
    }

    fun unlockParadoxical(): Boolean = astralArrayCount == MAX_ASTRAL_ARRAY_COUNT

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = UpkeepRecipeLogic(this, UPKEEP_RECIPE)

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        IGTLRenderDisabled.attachConfigurator(configuratorPanel, this as IGTLRenderDisabled)
    }

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
        }
        return actualIncrease
    }

    override fun addDisplayText(textList: MutableList<Component?>) {
        super.addDisplayText(textList)
        if (!isFormed) return
        textList.add(
            if (unlockParadoxical()) {
                createLanguageRainbowComponentOnServer(
                    Component.translatable("tooltip.gtladditions.industrial_array_max")
                )
            } else {
                Component.translatable(
                    "tooltip.gtladditions.astral_array_count",
                    Component.literal("$astralArrayCount / $MAX_ASTRAL_ARRAY_COUNT").withStyle(ChatFormatting.GOLD)
                )
            }
        )
        textList.add(Component.translatable("tooltip.gtlcore.installed_module_count", getMAM()))
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<SubspaceCorridorHubIndustrialArray, *>> = modules

    override fun getModuleScanPositions(): Array<BlockPos> = IndustrialArrayPosHelper.calculateModulePositions(pos, frontFacing)

    protected open fun acceptsModuleInSlot(slot: Int, module: SubspaceCorridorHubIndustrialArrayModuleBase): Boolean = true

    fun tryConnectModule(slot: Int, module: SubspaceCorridorHubIndustrialArrayModuleBase): Boolean {
        if (!isFormed ||
            !module.isRecipeLogicAvailable ||
            registeredSlots?.getOrNull(slot) != module.pos ||
            !acceptsModuleInSlot(slot, module)
        ) {
            return false
        }

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

    override fun getModulesForRendering(): List<ModuleRenderInfo?> = listOf(
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

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        releaseSlots()
    }

    override fun onPartUnload() {
        super.onPartUnload()
        releaseSlots()
    }

    override fun onMachineRemoved() {
        releaseSlots()
    }

    override fun onStructureFormed() {
        super.onStructureFormed()
        val world = level as? ServerLevel ?: return
        if (registeredSlots == null) {
            registeredSlots = moduleScanPositions.also { MODULE_CONNECTIONS.register(world.dimension(), this, it) }
        }
        scanAndConnectModules()
    }

    override fun onUnload() {
        releaseSlots()
        super.onUnload()
    }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MODULE_CONNECTIONS =
            ModuleConnectionRegistry<SubspaceCorridorHubIndustrialArray, SubspaceCorridorHubIndustrialArrayModuleBase>()

        private const val MAX_ASTRAL_ARRAY_COUNT = 512
        val UPKEEP_EUT: Long = GTValues.V[GTValues.UXV]
        private val UPKEEP_RECIPE = GTRecipeBuilder.ofRaw().EUt(UPKEEP_EUT).duration(UpkeepRecipeLogic.CYCLE_TICKS).buildRawRecipe()
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            SubspaceCorridorHubIndustrialArray::class.java,
            WorkableMultiblockMachine.MANAGED_FIELD_HOLDER
        )
    }
}
