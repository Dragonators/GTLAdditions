package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gtladd.gtladditions.api.machine.DataModuleBindingError
import com.gtladd.gtladditions.api.machine.DataModuleBindingResult
import com.gtladd.gtladditions.api.machine.IWirelessBindableSource
import com.gtladd.gtladditions.api.machine.IWirelessBindableTarget
import com.gtladd.gtladditions.api.machine.gui.MultiblockDisplayText
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.gtladd.gtladditions.utils.MachineUtil
import com.lowdragmc.lowdraglib.gui.util.ClickData
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget.withButton
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import org.gtlcore.gtlcore.api.machine.trait.ICheckPatternMachine
import org.gtlcore.gtlcore.api.machine.trait.IRecipeStatus
import org.gtlcore.gtlcore.api.recipe.RecipeResult
import org.gtlcore.gtlcore.common.data.GTLMaterials.Hypogen
import org.gtlcore.gtlcore.common.data.GTLMaterials.Infinity
import org.gtlcore.gtlcore.common.data.GTLMaterials.SpaceTime
import org.gtlcore.gtlcore.utils.Registries

class TimeSpaceDistorter(holder: IMachineBlockEntity, vararg args: Any?) :
    WorkableElectricMultiblockMachine(holder, *args),
    IMachineLife,
    IWirelessBindableTarget {

    @Persisted
    @DescSynced
    private var causalityDistortionLevel = 1

    @Persisted
    @DescSynced
    private var boundMatrixPos: BlockPos? = null

    @Persisted
    @DescSynced
    private var lastConsumedDistortionLevel = 0

    @Persisted
    @DescSynced
    private var lastConsumedParallels = 0L

    @Persisted
    @DescSynced
    private var lastConsumptionSucceeded = false

    private var boundMatrix: ApocalypticTorsionQuantumMatrix? = null

    // ========================================
    // Wireless binding
    // ========================================

    override val bindingTargetType: ResourceLocation = BINDING_TYPE

    override fun acceptsBindingSource(type: ResourceLocation): Boolean = type == ApocalypticTorsionQuantumMatrix.BINDING_TYPE

    override fun bindResolvedSource(source: IWirelessBindableSource<*>): DataModuleBindingResult {
        val matrix = source as? ApocalypticTorsionQuantumMatrix ?: return DataModuleBindingResult.Failure(
            DataModuleBindingError.TARGET_MISMATCH,
            "gtladditions.message.suprachronal_data_module.target_mismatch".toComponent
        )

        if (matrix.level?.dimension() != level?.dimension()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.DIMENSION_MISMATCH,
                "gtladditions.message.suprachronal_data_module.dimension_mismatch".toComponent
            )
        }
        if (!matrix.isFormed() || !isFormed()) {
            return DataModuleBindingResult.Failure(
                DataModuleBindingError.TARGET_MISMATCH,
                "gtladditions.message.suprachronal_data_module.target_mismatch".toComponent
            )
        }

        if (boundMatrix != matrix) {
            boundMatrix?.onUnbound(this)
        }
        boundMatrix = matrix
        boundMatrixPos = matrix.pos

        matrix.onBound(this)
        return DataModuleBindingResult.BoundSuccess(source, matrix.pos, getBoundMessage(source, matrix.pos))
    }

    override fun unbindSource() = disconnectATQM(clearBinding = true)

    fun disconnectATQM(clearBinding: Boolean) {
        val oldMatrix = boundMatrix
        boundMatrix = null
        if (clearBinding) boundMatrixPos = null
        oldMatrix?.disconnectTSD(this, clearBinding)
    }

    private fun reconnectATQM(): Boolean {
        if (!isFormed) return false
        val matrixPos = boundMatrixPos ?: return false
        val machine = ((level as? ServerLevel)?.getBlockEntity(matrixPos) as? MetaMachineBlockEntity)
            ?.metaMachine as? ApocalypticTorsionQuantumMatrix
            ?: return false
        if (!machine.isFormed()) return false
        return bindResolvedSource(machine).isSuccess
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun onStructureFormed() {
        super.onStructureFormed()
        reconnectATQM()
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        disconnectATQM(clearBinding = false)
    }

    override fun onMachineRemoved() {
        disconnectATQM(clearBinding = true)
    }

    override fun onUnload() {
        super.onUnload()
        boundMatrix?.disconnectTSD(this, clearBinding = false)
        boundMatrix = null
    }

    // ========================================
    // Causality distortion
    // ========================================

    fun tryConsumeAndGetOutputMultiplier(totalParallels: Long): Double {
        if (!isReady() || totalParallels <= 0L) return 1.0
        return if (consumeResources(totalParallels)) outputMultiplier else 1.0
    }

    private fun isReady(): Boolean = isFormed && recipeLogic.isWorking

    private fun consumeResources(parallels: Long): Boolean {
        val input = MachineUtil.input(this)

        forEachRequiredResource(
            causalityDistortionLevel,
            parallels,
            addFluid = { input.fluid(it) },
            addItem = { stack, amount -> input.item(stack, amount) }
        )

        val consumed = input.execute()
        RecipeResult.of(this, null)
        lastConsumedDistortionLevel = causalityDistortionLevel
        lastConsumedParallels = parallels
        lastConsumptionSucceeded = consumed
        return consumed
    }

    private fun forEachRequiredResource(
        distortionLevel: Int,
        parallels: Long,
        addFluid: (FluidStack) -> Unit,
        addItem: (ItemStack, Long) -> Unit
    ) {
        fun addResourceRequired(level: Int, divisor: Long, addInput: (Long) -> Unit) {
            if (distortionLevel < level) return
            val amount = parallels / divisor
            if (amount > 0L) addInput(amount)
        }

        addResourceRequired(1, 17) { addFluid(Infinity.getFluid(it)) }
        addResourceRequired(2, 28) { addFluid(Hypogen.getFluid(it)) }
        addResourceRequired(3, 44) { addFluid(SpaceTime.getFluid(it)) }
        addResourceRequired(4, 730) { addItem(QUANTUM_ANOMALY, it) }
        addResourceRequired(4, 873) { addItem(HYPERCUBE, it) }
    }

    private val outputMultiplier: Double
        get() = when (causalityDistortionLevel) {
            1 -> 1.3
            2 -> 1.7
            3 -> 2.6
            4 -> 3.2
            else -> 1.0
        }

    // ========================================
    // Client rendering
    // ========================================

    fun shouldRenderMagicCircleEffect(): Boolean = isFormed && recipeLogic.isWorking && boundMatrixPos != null

    val renderCausalityDistortionLevel: Int
        get() = causalityDistortionLevel.coerceIn(1, 4)

    // ========================================
    // GUI
    // ========================================

    override fun addDisplayText(textList: MutableList<Component>) {
        MultiblockDisplayText.builder(textList, isFormed)
            .setWorkingStatus(recipeLogic.isWorkingEnabled, recipeLogic.isActive)
            .addEnergyUsageLine(energyContainer)
            .addEnergyTierLine(tier)
            .addMachineModeLine(recipeType)
            .addWorkingStatusLine()
            .addProgressLine(recipeLogic.progressPercent)
            .addRecipeStatus(recipeLogic as IRecipeStatus)
            .addComponent(
                "gtladditions.machine.time_space_distorter.level".toComponent(
                    Component.literal(causalityDistortionLevel.toString()).withStyle(ChatFormatting.AQUA),
                    Component.literal(outputMultiplier.toString()).withStyle(ChatFormatting.GOLD)
                ),
                Component.empty()
                    .append(withButton(Component.literal("[-] "), "levelSub"))
                    .append(withButton(Component.literal("[+]"), "levelAdd"))
            )
            .addComponent(
                "gtladditions.machine.time_space_distorter.bound".toComponent(
                    Component.literal(if (boundMatrixPos != null) "✓" else "x")
                        .withStyle(if (boundMatrixPos != null) ChatFormatting.GREEN else ChatFormatting.RED)
                )
            )
        addLastConsumedDisplayText(textList)
    }

    private fun addLastConsumedDisplayText(textList: MutableList<Component>) {
        if (!isFormed || lastConsumedParallels <= 0L) return
        textList.add(
            Component.translatable(
                if (lastConsumptionSucceeded) {
                    "gtladditions.machine.time_space_distorter.last_consumed"
                } else {
                    "gtladditions.machine.time_space_distorter.last_consumed_failed"
                }
            ).withStyle(if (lastConsumptionSucceeded) ChatFormatting.GRAY else ChatFormatting.RED)
        )

        forEachRequiredResource(
            lastConsumedDistortionLevel,
            lastConsumedParallels,
            addFluid = { fluid ->
                textList.add(
                    Component.translatable(
                        "gtladditions.machine.time_space_distorter.last_consumed_fluid_entry",
                        fluid.displayName.copy().withStyle(ChatFormatting.AQUA),
                        FormattingUtil.formatNumbers(fluid.amount)
                    ).withStyle(ChatFormatting.GRAY)
                )
            },
            addItem = { stack, amount ->
                textList.add(
                    Component.translatable(
                        "gtladditions.machine.time_space_distorter.last_consumed_item_entry",
                        stack.displayName.copy().withStyle(ChatFormatting.GOLD),
                        FormattingUtil.formatNumbers(amount)
                    ).withStyle(ChatFormatting.GRAY)
                )
            }
        )
    }

    override fun handleDisplayClick(componentData: String, clickData: ClickData) {
        if (clickData.isRemote) return
        causalityDistortionLevel = when (componentData) {
            "levelSub" -> Mth.clamp(causalityDistortionLevel - 1, 1, 4)
            "levelAdd" -> Mth.clamp(causalityDistortionLevel + 1, 1, 4)
            else -> causalityDistortionLevel
        }
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        configuratorPanel.attachConfigurators(
            IFancyConfiguratorButton.Toggle(
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.0, 1.0, 0.5),
                GuiTextures.BUTTON_POWER.getSubTexture(0.0, 0.5, 1.0, 0.5),
                { isWorkingEnabled },
                { _, pressed -> isWorkingEnabled = pressed }
            ).setTooltipsSupplier {
                listOf((if (it) "behaviour.soft_hammer.enabled" else "behaviour.soft_hammer.disabled").toComponent)
            }
        )
        ICheckPatternMachine.attachConfigurators(configuratorPanel, self())
    }

    // ========================================
    // Metadata
    // ========================================

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val BINDING_TYPE: ResourceLocation = ResourceLocation("gtladditions", "time_space_distorter")

        private val QUANTUM_ANOMALY = Registries.getItemStack("kubejs:quantum_anomaly")
        private val HYPERCUBE = Registries.getItemStack("kubejs:hypercube")

        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            TimeSpaceDistorter::class.java,
            WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER
        )
    }
}