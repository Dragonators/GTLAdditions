package com.gtladd.gtladditions.common.machine.multiblock.controller.bs

import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.IWirelessElectricMultiblockMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.gui.MultiblockDisplayText
import com.gtladd.gtladditions.api.machine.trait.IWirelessNetworkEnergyHandler
import com.gtladd.gtladditions.common.machine.GTLAddMachines
import com.gtladd.gtladditions.common.machine.multiblock.MultiBlockMachine
import com.gtladd.gtladditions.common.machine.multiblock.part.WirelessEnergyNetworkTerminalPartMachine
import com.gtladd.gtladditions.utils.CommonUtils
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineHost
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.integration.gtmt.NewGTValues
import org.gtlcore.gtlcore.utils.NumberUtils
import org.gtlcore.gtlcore.utils.TextUtil
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.function.Consumer

class BiosphereIIIController(holder: IMachineBlockEntity) :
    CoilWorkableElectricMultiblockMachine(holder),
    IModularMachineHost<BiosphereIIIController>,
    IWirelessElectricMultiblockMachine,
    IThreadModifierMachine,
    IMachineLife {
    private val modulePos = ObjectOpenHashSet<IModularMachineModule<BiosphereIIIController, *>>(8)
    private var energySample = NO_ENERGY_SAMPLE
    private var currentSampleEnergy = BigInteger.ZERO
    private var averageEnergyConsumption = BigInteger.ZERO
    private var energySource = EnergySource.NONE
    private var wirelessNetworkEnergyHandler: IWirelessNetworkEnergyHandler? = null
    private var threadPartMachine: IThreadModifierPart? = null

    private enum class EnergySource {
        NONE,
        WIRED,
        WIRELESS
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun onStructureFormed() {
        clearRuntimeState()
        super.onStructureFormed()
        safeClearModules()
        energySource = if (
            parts.filterIsInstance<WirelessEnergyNetworkTerminalPartMachine>()
                .any { it.definition == GTLAddMachines.Wireless_Energy_Network_INPUT_Terminal }
        ) {
            EnergySource.WIRELESS
        } else {
            EnergySource.WIRED
        }
        scanAndConnectModules()
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        safeClearModules()
        clearRuntimeState()
    }

    override fun onMachineRemoved() {
        safeClearModules()
        clearRuntimeState()
    }

    private fun clearRuntimeState() {
        energySource = EnergySource.NONE
        wirelessNetworkEnergyHandler = null
        threadPartMachine = null
        energySample = NO_ENERGY_SAMPLE
        currentSampleEnergy = BigInteger.ZERO
        averageEnergyConsumption = BigInteger.ZERO
    }

    // ========================================
    // Module connection
    // ========================================

    override fun getModuleSet(): MutableSet<IModularMachineModule<BiosphereIIIController, *>> = modulePos

    override fun getModuleScanPositions(): Array<BlockPos> =
        BiosphereIIIPosHelper.calculateModulePositions(pos, frontFacing)

    override fun getModulesForRendering(): List<ModuleRenderInfo> = listOf(
        ModuleRenderInfo(BlockPos(-6, 7, 0), Direction.UP, Direction.NORTH, Direction.NORTH, Direction.UP, MultiBlockMachine.BIOSPHERE_III_MODULE),
        ModuleRenderInfo(BlockPos(6, 7, 0), Direction.UP, Direction.NORTH, Direction.NORTH, Direction.UP, MultiBlockMachine.BIOSPHERE_III_MODULE),
        ModuleRenderInfo(BlockPos(8, 9, 0), Direction.UP, Direction.NORTH, Direction.EAST, Direction.UP, MultiBlockMachine.BIOSPHERE_III_MODULE),
        ModuleRenderInfo(BlockPos(-8, 9, 0), Direction.UP, Direction.NORTH, Direction.WEST, Direction.UP, MultiBlockMachine.BIOSPHERE_III_MODULE),
        ModuleRenderInfo(BlockPos(8, 15, 0), Direction.UP, Direction.NORTH, Direction.WEST, Direction.UP, MultiBlockMachine.BIOSPHERE_III_MODULE),
        ModuleRenderInfo(BlockPos(-8, 15, 0), Direction.UP, Direction.NORTH, Direction.EAST, Direction.UP, MultiBlockMachine.BIOSPHERE_III_MODULE),
        ModuleRenderInfo(BlockPos(6, 17, 0), Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.UP, MultiBlockMachine.BIOSPHERE_III_MODULE),
        ModuleRenderInfo(BlockPos(-6, 17, 0), Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.UP, MultiBlockMachine.BIOSPHERE_III_MODULE)
    )

    override fun isFormed(): Boolean = isFormed

    override fun getMaxModuleCount(): Int = 8

    override fun setWorkingEnabled(workingEnabled: Boolean) {
        super.setWorkingEnabled(workingEnabled)
        modulePos.filterIsInstance<BiosphereIIIModule>().forEach { it.recipeLogic.updateTickSubscription() }
    }

    private fun hasActiveModules(): Boolean =
        modulePos.filterIsInstance<BiosphereIIIModule>().any { it.recipeLogic.isWorking }

    internal fun getModuleRecipePower(): Long {
        val hostPower = overclockVoltage
        if (energySource == EnergySource.WIRELESS) return hostPower

        val enabledModuleCount = modulePos.count {
            it is BiosphereIIIModule && it.isFormed && it.isWorkingEnabled
        }
        return if (enabledModuleCount > 0) hostPower / enabledModuleCount else 0
    }

    // ========================================
    // Energy handling
    // ========================================

    override fun getWirelessNetworkEnergyHandler(): IWirelessNetworkEnergyHandler? =
        wirelessNetworkEnergyHandler.takeIf { energySource == EnergySource.WIRELESS }

    override fun setWirelessNetworkEnergyHandler(trait: IWirelessNetworkEnergyHandler) {
        wirelessNetworkEnergyHandler = trait
    }

    internal fun requestEnergy(energy: Long): Boolean {
        if (!canRequestEnergy()) return false

        val consumed = when (energySource) {
            EnergySource.WIRELESS -> wirelessNetworkEnergyHandler?.let { handler ->
                handler.isOnline && handler.consumeEnergy(BigInteger.valueOf(energy).negate())
            } ?: false

            EnergySource.WIRED -> consumeWiredEnergy(energy)
            EnergySource.NONE -> false
        }

        if (consumed) recordEnergyConsumption(BigInteger.valueOf(energy))
        return consumed
    }

    internal fun requestWirelessEnergy(energy: BigInteger): Boolean {
        if (!canRequestEnergy() || energySource != EnergySource.WIRELESS) return false
        val handler = wirelessNetworkEnergyHandler ?: return false
        val consumed = handler.isOnline && handler.consumeEnergy(energy)
        if (consumed) recordEnergyConsumption(energy.negate())
        return consumed
    }

    private fun canRequestEnergy(): Boolean =
        isFormed && isWorkingEnabled && level?.isClientSide == false

    private fun consumeWiredEnergy(energy: Long): Boolean {
        val container = energyContainer ?: return false
        if (container.energyStored < energy) return false

        val removed = container.removeEnergy(energy)
        if (removed == energy) return true

        if (removed > 0) container.addEnergy(removed)
        return false
    }

    // ========================================
    // Energy statistics
    // ========================================

    private fun recordEnergyConsumption(energy: BigInteger) {
        updateEnergySample()
        currentSampleEnergy = currentSampleEnergy.add(energy)
    }

    internal fun getAverageEnergyConsumption(): BigInteger {
        updateEnergySample()
        return averageEnergyConsumption
    }

    private fun updateEnergySample() {
        val currentSample = Math.floorDiv(offsetTimer, ENERGY_AVERAGE_WINDOW.toLong())
        if (energySample == NO_ENERGY_SAMPLE) {
            energySample = currentSample
            return
        }
        if (currentSample == energySample) return

        averageEnergyConsumption = if (currentSample == energySample + 1) {
            currentSampleEnergy.divide(ENERGY_AVERAGE_WINDOW_BIG_INTEGER)
        } else {
            BigInteger.ZERO
        }
        currentSampleEnergy = BigInteger.ZERO
        energySample = currentSample
    }

    // ========================================
    // Thread modifier
    // ========================================

    override fun getThreadPartMachine(): IThreadModifierPart? = threadPartMachine

    override fun setThreadPartMachine(threadModifierPart: IThreadModifierPart) {
        threadPartMachine = threadModifierPart
    }

    // ========================================
    // GUI
    // ========================================

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        configuratorPanel.attachConfigurators(
            IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.0, 1.0, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.5, 1.0, 0.5),
                { isWorkingEnabled },
                { _, pressed -> setWorkingEnabled(pressed) }
            ).setTooltipsSupplier {
                listOf(
                    if (it) {
                        "behaviour.soft_hammer.enabled".toComponent
                    } else {
                        "behaviour.soft_hammer.disabled".toComponent
                    }
                )
            }
        )
    }

    override fun attachSideTabs(sideTabs: TabsWidget) {
        sideTabs.setMainTab(this)
        CombinedDirectionalFancyConfigurator.of(self(), self())?.let(sideTabs::attachSubTab)
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, 190, 125)
        group.addWidget(
            DraggableScrollableWidgetGroup(4, 4, 182, 117).setBackground(screenTexture)
                .addWidget(LabelWidget(4, 5, self().blockState.block.descriptionId))
                .addWidget(
                    ComponentPanelWidget(4, 17, ::addDisplayText)
                        .textSupplier(if (level!!.isClientSide) null else Consumer(::addDisplayText))
                        .setMaxWidthLimit(150)
                )
        )
        group.setBackground(GuiTextures.BACKGROUND_INVERSE)
        return group
    }

    override fun addDisplayText(textList: MutableList<Component>) {
        val modulesActive = hasActiveModules()
        val averageEnergyConsumption = getAverageEnergyConsumption()
        MultiblockDisplayText.builder(textList, isFormed)
            .setWorkingStatus(recipeLogic.isWorkingEnabled, modulesActive)
            .addEnergyTierLine(tier)
            .addWorkingStatusLine()
            .addComponent(
                "gui.gtladditions.biosphere_iii_average_eut".toComponent(
                    CommonUtils.formatBigIntegerFixed(averageEnergyConsumption).literal
                        .withStyle(ChatFormatting.GOLD),
                    getEnergyTierComponent(averageEnergyConsumption)
                )
            )
            .addComponent(Component.translatable("gtceu.machine.module", modulePos.size))
    }

    // ========================================
    // Recipe logic
    // ========================================

    override fun createRecipeLogic(vararg args: Any): RecipeLogic = BSRecipeLogic(this)

    private class BSRecipeLogic(private val biosphere: BiosphereIIIController) : RecipeLogic(biosphere) {
        override fun serverTick() {
            biosphere.updateEnergySample()
            status = when {
                !biosphere.isWorkingEnabled -> Status.SUSPEND
                biosphere.hasActiveModules() -> Status.WORKING
                else -> Status.IDLE
            }
        }

        override fun updateSound() = Unit
    }

    // ========================================
    // Metadata
    // ========================================

    companion object {
        private const val ENERGY_AVERAGE_WINDOW = 20
        private const val NO_ENERGY_SAMPLE = Long.MIN_VALUE
        private val ENERGY_AVERAGE_WINDOW_BIG_INTEGER = BigInteger.valueOf(ENERGY_AVERAGE_WINDOW.toLong())

        internal fun getEnergyTierComponent(energy: BigInteger): Component {
            val longEnergy = NumberUtils.getLongValue(energy)
            val energyTier = if (longEnergy == Long.MAX_VALUE) {
                GTValues.MAX_TRUE
            } else {
                NumberUtils.getFakeVoltageTier(longEnergy)
            }
            val amperage = BigDecimal(energy).divide(
                BigDecimal.valueOf(GTValues.VEX[energyTier]),
                2,
                RoundingMode.DOWN
            )
            return "gtceu.top.electricity".toComponent(
                CommonUtils.format2BigDecimal(amperage),
                NewGTValues.VNF[energyTier]
            ).withStyle { style ->
                style.withColor(TextUtil.`GTL_CORE$VC`[energyTier.coerceAtMost(14)])
            }
        }
    }
}