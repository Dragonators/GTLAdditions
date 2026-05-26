package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.DataModuleBindingError
import com.gtladd.gtladditions.api.machine.DataModuleBindingResult
import com.gtladd.gtladditions.api.machine.IWirelessBindableSource
import com.gtladd.gtladditions.api.machine.IWirelessBindableTarget
import com.gtladd.gtladditions.common.data.RecursiveReverseBuffState
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.ForgeOfTheAntichrist
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.modular.ModularUI
import com.lowdragmc.lowdraglib.gui.widget.*
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.machine.trait.ICheckPatternMachine
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo
import java.util.function.Consumer

class RecursiveReverseArray(holder: IMachineBlockEntity) :
    MultiblockControllerMachine(holder),
    IModularMachineHost<RecursiveReverseArray>,
    IWirelessBindableTarget,
    IFancyUIMachine,
    IDisplayUIMachine,
    IMachineLife,
    ICheckPatternMachine {

    @field:DescSynced
    @field:Persisted
    private var boundForgePos: BlockPos? = null

    @Persisted
    var enabled: Boolean = true

    @Persisted
    private var buffRollCounter: Long = 0

    private var boundForge: ForgeOfTheAntichrist? = null
    private val modules: MutableSet<IModularMachineModule<RecursiveReverseArray, *>> = ObjectOpenHashSet()
    private var tickSubscription: TickableSubscription? = null

    // ========================================
    // Wireless binding
    // ========================================

    override val bindingTargetType: ResourceLocation = BINDING_TYPE

    override fun acceptsBindingSource(type: ResourceLocation): Boolean = type == ForgeOfTheAntichrist.BINDING_TYPE

    override fun bindResolvedSource(source: IWirelessBindableSource<*>): DataModuleBindingResult {
        val forge = source as? ForgeOfTheAntichrist ?: return DataModuleBindingResult.Failure(
            DataModuleBindingError.TARGET_MISMATCH,
            "gtladditions.message.suprachronal_data_module.target_mismatch".toComponent
        )

        if (forge.level?.dimension() != level?.dimension()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.TARGET_MISMATCH,
                "gtladditions.message.suprachronal_data_module.dimension_mismatch".toComponent
            )
        }
        if (!forge.isFormed() || !isFormed()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.TARGET_MISMATCH,
                "gtladditions.message.suprachronal_data_module.target_mismatch".toComponent
            )
        }

        if (boundForge != forge) {
            boundForge?.onUnbound(this)
        }
        boundForge = forge
        boundForgePos = forge.pos

        forge.onBound(this)
        return DataModuleBindingResult.BoundSuccess(source, forge.pos, getBoundMessage(source, forge.pos))
    }

    override fun unbindSource() = unbindForge(clearBinding = true)

    fun unbindForge(clearBinding: Boolean) {
        val oldForge = boundForge
        boundForge = null
        if (clearBinding) boundForgePos = null
        oldForge?.disconnectRecursiveReverseArray(this, clearBinding)
    }

    private fun reconnectSource(): Boolean {
        val forgePos = boundForgePos ?: return false
        val machine = ((level as? ServerLevel)?.getBlockEntity(forgePos) as? MetaMachineBlockEntity)
            ?.metaMachine as? ForgeOfTheAntichrist
            ?: return false
        return bindResolvedSource(machine).isSuccess
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): Set<IModularMachineModule<RecursiveReverseArray, *>> = modules

    override fun getModuleScanPositions(): Array<out BlockPos> = MODULE_OFFSETS.map { pos!!.offset(it) }.toTypedArray()

    override fun getModulesForRendering(): List<ModuleRenderInfo> = listOf(
        ModuleRenderInfo(
            BlockPos(33, 0, 7),
            Direction.UP,
            Direction.NORTH,
            Direction.EAST,
            Direction.UP,
            MultiBlockMachine.CATALYTIC_CASCADE_ARRAY
        ),
        ModuleRenderInfo(
            BlockPos(-33, 0, 7),
            Direction.UP,
            Direction.NORTH,
            Direction.WEST,
            Direction.UP,
            MultiBlockMachine.MAGNETORHEOLOGICAL_CONVERGENCE_CORE
        ),
        ModuleRenderInfo(
            BlockPos(0, 33, 7),
            Direction.UP,
            Direction.NORTH,
            Direction.UP,
            Direction.NORTH,
            MultiBlockMachine.SPACETIME_STASIS_DEVICE
        ),
        ModuleRenderInfo(
            BlockPos(0, -33, 7),
            Direction.UP,
            Direction.NORTH,
            Direction.DOWN,
            Direction.NORTH,
            MultiBlockMachine.SUPRATEMPORAL_BOOSTING_ENGINE
        )
    )

    // ========================================
    // Buff state
    // ========================================

    fun getBuffState(): RecursiveReverseBuffState {
        val context = createBuffContext()
        val buffRolledActive = context.machinesReady && context.boostReady && rollBuffPackage(context.buffProbability)
        return buildBuffState(context, buffRolledActive)
    }

    fun getBoostPreview(): BoostPreview {
        val context = createBuffContext()
        return getBoostPreview(context)
    }

    fun isStarRitualGateActive(): Boolean {
        val context = createBuffContext()
        return context.machinesReady && context.boostReady && context.boost?.isTemperatureOptimal() == true
    }

    private fun createBuffContext(): BuffContext {
        val forge = boundForge
        val boost = modules.filterIsInstance<SupratemporalBoostingEngine>().firstOrNull()
        val boostReady = boost?.isBoostWindowActive() == true
        val machinesReady = enabled && forge?.isFormed() == true && isFormed()
        val buffProbability = if (enabled && boostReady) boost.getPerfectSupratemporalBoostParameter() else 0.0
        return BuffContext(forge, boost, boostReady, machinesReady, buffProbability)
    }

    private fun getBoostPreview(context: BuffContext): BoostPreview {
        val commonActive = context.machinesReady && context.boostReady && context.buffProbability > 0.0
        return BoostPreview(
            commonActive = commonActive,
            probability = context.buffProbability,
            outputMultiplier = getPreviewOutputMultiplier(commonActive),
            activeModuleKeys = getPreviewActiveModuleKeys(context, commonActive),
            diagnostics = buildDiagnostics(context)
        )
    }

    private fun getPreviewOutputMultiplier(commonActive: Boolean): Double {
        val catalyticActive = commonActive && modules.any { it is CatalyticCascadeArray && it.isReadyForRecursiveReverseBuff() && it.hasOutputBoost() }
        return if (catalyticActive) 2.0 else 1.0
    }

    private fun getPreviewActiveModuleKeys(context: BuffContext, commonActive: Boolean): List<String> {
        if (!commonActive) return emptyList()

        val activeModules = mutableListOf<String>()
        if (context.boost != null) activeModules.add("block.gtladditions.supratemporal_boosting_engine")
        if (modules.any { it is CatalyticCascadeArray && (it.isReadyForRecursiveReverseBuff() || it.isReadyForRecursiveReverseEuBuff()) }) {
            activeModules.add("block.gtladditions.catalytic_cascade_array")
        }
        if (modules.any { it is MagnetorheologicalConvergenceCore && it.isReadyForRecursiveReverseBuff() && it.hasFocus() }) {
            activeModules.add("block.gtladditions.magnetorheological_convergence_core")
        }
        if (modules.any { it is SpacetimeStasisDevice && it.isReadyForRecursiveReverseBuff() }) {
            activeModules.add("block.gtladditions.spacetime_stasis_device")
        }
        return activeModules
    }

    private fun buildBuffState(context: BuffContext, buffRolledActive: Boolean): RecursiveReverseBuffState {
        val boost = context.boost
        val commonActive = context.machinesReady && context.boostReady && buffRolledActive
        val starRitualGateActive = context.machinesReady && context.boostReady && boost?.isTemperatureOptimal() == true
        val catalyticActive = commonActive && modules.any { it is CatalyticCascadeArray && it.isReadyForRecursiveReverseBuff() && it.hasOutputBoost() }
        val catalyticEuActive = commonActive && modules.any { it is CatalyticCascadeArray && it.isReadyForRecursiveReverseEuBuff() && it.hasEuBuff() }
        val catalyticEuMultiplier = if (catalyticEuActive) CATALYTIC_EU_MULTIPLIER else 1.0
        val magnetorheologicalActive = commonActive && modules.any { it is MagnetorheologicalConvergenceCore && it.isReadyForRecursiveReverseBuff() && it.hasFocus() }
        val stasisActive = commonActive && modules.any { it is SpacetimeStasisDevice && it.isReadyForRecursiveReverseBuff() }

        return RecursiveReverseBuffState(
            commonActive = commonActive,
            boostEngineInstalled = boost != null,
            boostEngineOptimal = boost?.isTemperatureOptimal() == true,
            boostEngineOverheated = boost?.isOverheated() == true,
            starRitualGateActive = starRitualGateActive,
            catalyticCascadeActive = catalyticActive,
            catalyticCascadeEuActive = catalyticEuActive,
            magnetorheologicalConvergenceActive = magnetorheologicalActive,
            spacetimeStasisActive = stasisActive,
            outputMultiplier = if (catalyticActive) 2.0 else 1.0,
            euMultiplier = if (commonActive) (boost?.getEuMultiplierBuff() ?: 1.0) * catalyticEuMultiplier else 1.0,
            diagnostics = buildDiagnostics(context)
        )
    }

    private fun rollBuffPackage(probability: Double): Boolean {
        if (probability >= 1.0) return true
        if (probability <= 0.0) return false
        return RandomSource.create(nextBuffRollSeed()).nextDouble() < probability
    }

    private fun nextBuffRollSeed(): Long {
        val roll = buffRollCounter++
        return (pos?.asLong() ?: 0L) xor (roll * BUFF_ROLL_COUNTER_SALT) xor BUFF_ROLL_SEED_SALT
    }

    private fun buildDiagnostics(context: BuffContext): List<String> {
        val diagnostics = mutableListOf<String>()
        if (!enabled) {
            diagnostics.add("gtladditions.machine.recursive_reverse_array.status.disabled")
            return diagnostics
        }
        val forge = context.forge
        val boost = context.boost
        if (forge == null) diagnostics.add("gtladditions.machine.recursive_reverse_array.status.no_forge")
        if (boost == null) diagnostics.add("gtladditions.machine.recursive_reverse_array.status.no_boost")
        if (boost != null) {
            if (boost.isOverheated()) {
                diagnostics.add("gtladditions.machine.recursive_reverse_array.status.boost_overheated")
            } else if (!boost.isReadyForRecursiveReverseBuff()) {
                diagnostics.add("gtladditions.machine.recursive_reverse_array.status.boost_not_running")
            }
        }
        return diagnostics
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun onStructureFormed() {
        super.onStructureFormed()
        safeClearModules()
        scanAndConnectModules()
        reconnectSource()
        updateTickSubscription()
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        safeClearModules()
        unbindForge(clearBinding = false)
        updateTickSubscription()
    }

    override fun onMachineRemoved() {
        safeClearModules()
        unbindForge(clearBinding = true)
    }

    override fun onUnload() {
        super.onUnload()
        boundForge?.disconnectRecursiveReverseArray(this, clearBinding = false)
        boundForge = null
    }

    private fun updateTickSubscription() {
        if (isFormed) {
            tickSubscription = subscribeServerTick(tickSubscription, ::updateTickSubscription)
        } else {
            tickSubscription?.unsubscribe()
            tickSubscription = null
        }
    }

    // ========================================
    // GUI
    // ========================================

    override fun addDisplayText(textList: MutableList<Component>) {
        super.addDisplayText(textList)
        val forgePos = boundForgePos
        if (forgePos == null) {
            textList.add("gtladditions.machine.recursive_reverse_array.unbound".toComponent)
        } else {
            textList.add(
                "gtladditions.machine.recursive_reverse_array.bound".toComponent(
                    Component.literal(forgePos.x.toString()).withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal(forgePos.y.toString()).withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal(forgePos.z.toString()).withStyle(ChatFormatting.LIGHT_PURPLE)
                )
            )
        }

        val boostPreview = getBoostPreview()
        textList.add(
            "gtladditions.machine.recursive_reverse_array.common_active".toComponent(
                Component.literal(if (boostPreview.commonActive) "✓" else "x")
                    .withStyle(if (boostPreview.commonActive) ChatFormatting.GREEN else ChatFormatting.RED)
            )
        )
        textList.add(
            "gtladditions.machine.recursive_reverse_array.buff_probability".toComponent(
                Component.literal("${FormattingUtil.DECIMAL_FORMAT_2F.format(boostPreview.probability * 100.0)}%")
                    .withStyle(ChatFormatting.AQUA)
            )
        )
        textList.add(
            "gtladditions.machine.recursive_reverse_array.active_modules".toComponent(
                createActiveModulesComponent(boostPreview.activeModuleKeys)
            )
        )
        boostPreview.diagnostics.forEach { textList.add(it.toComponent) }
    }

    private fun createActiveModulesComponent(activeModuleKeys: List<String>): Component {
        if (activeModuleKeys.isEmpty()) return Component.literal("-").withStyle(ChatFormatting.GOLD)

        val component = Component.empty()
        activeModuleKeys.forEachIndexed { index, key ->
            if (index > 0) component.append(Component.literal(" / ").withStyle(ChatFormatting.GOLD))
            component.append(Component.translatable(key).withStyle(ChatFormatting.GOLD))
        }
        return component
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, 190, 125)
        group.addWidget(
            DraggableScrollableWidgetGroup(4, 4, 182, 117).setBackground(screenTexture)
                .addWidget(LabelWidget(4, 5, self().blockState.block.descriptionId))
                .addWidget(
                    ComponentPanelWidget(4, 17) { addDisplayText(it) }
                        .textSupplier(if (level!!.isClientSide) null else Consumer { addDisplayText(it) })
                        .setMaxWidthLimit(150)
                )
        )
        group.setBackground(GuiTextures.BACKGROUND_INVERSE)
        return group
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        configuratorPanel.attachConfigurators(
            IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.0, 1.0, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.5, 1.0, 0.5),
                { enabled },
                { _, pressed -> enabled = pressed }
            ).setTooltipsSupplier { listOf((if (it) "behaviour.soft_hammer.enabled" else "behaviour.soft_hammer.disabled").toComponent) }
        )
        ICheckPatternMachine.attachConfigurators(configuratorPanel, self())
    }

    override fun hasButton(): Boolean = true

    override fun createUI(entityPlayer: Player): ModularUI = ModularUI(198, 208, this, entityPlayer)
        .widget(FancyMachineUIWidget(this, 198, 208))

    // ========================================
    // Metadata
    // ========================================

    override fun isRemote(): Boolean = super<MultiblockControllerMachine>.isRemote

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            RecursiveReverseArray::class.java,
            MultiblockControllerMachine.MANAGED_FIELD_HOLDER
        )

        private val MODULE_OFFSETS = arrayOf(
            BlockPos(33, -7, 0),
            BlockPos(-33, -7, 0),
            BlockPos(0, -7, 33),
            BlockPos(0, -7, -33)
        )

        val BINDING_TYPE: ResourceLocation = ResourceLocation("gtladditions", "recursive_reverse_array")
        private const val CATALYTIC_EU_MULTIPLIER = 0.15
        private const val BUFF_ROLL_COUNTER_SALT = -7046029254386353131L
        private const val BUFF_ROLL_SEED_SALT = 7640891576956012809L

        private data class BuffContext(
            val forge: ForgeOfTheAntichrist?,
            val boost: SupratemporalBoostingEngine?,
            val boostReady: Boolean,
            val machinesReady: Boolean,
            val buffProbability: Double
        )

        data class BoostPreview(
            val commonActive: Boolean,
            val probability: Double,
            val outputMultiplier: Double,
            val activeModuleKeys: List<String>,
            val diagnostics: List<String>
        )
    }
}