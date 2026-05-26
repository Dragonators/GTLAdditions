package com.gtladd.gtladditions.common.machine.multiblock.controller.rrf

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.TickableSubscription
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine
import com.gregtechceu.gtceu.api.sound.AutoReleasedSound
import com.gregtechceu.gtceu.api.sound.SoundEntry
import com.gregtechceu.gtceu.config.ConfigHolder
import com.gtladd.gtladditions.api.machine.gui.MultiblockDisplayText
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.modular.ModularUI
import com.lowdragmc.lowdraglib.gui.widget.*
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import org.gtlcore.gtlcore.api.machine.multiblock.IModularMachineModule
import org.gtlcore.gtlcore.api.machine.trait.ICheckPatternMachine
import java.util.function.Consumer

abstract class RRFModuleMachine(holder: IMachineBlockEntity) :
    MultiblockControllerMachine(holder),
    IModularMachineModule<RecursiveReverseArray, RRFModuleMachine>,
    IFancyUIMachine,
    IDisplayUIMachine,
    IMachineLife,
    ICheckPatternMachine {

    @Persisted
    @DescSynced
    var enabled: Boolean = true

    @Persisted
    private var hostPosition: BlockPos? = null

    private var host: RecursiveReverseArray? = null
    private var tickSubscription: TickableSubscription? = null
    private var workingSound: Any? = null

    // ========================================
    // Recursive reverse buff
    // ========================================

    open fun isReadyForRecursiveReverseBuff(): Boolean = isFormed && enabled

    // ========================================
    // Module connection
    // ========================================

    override fun getHostPosition(): BlockPos? = hostPosition

    override fun setHostPosition(pos: BlockPos?) {
        hostPosition = pos
    }

    override fun getHost(): RecursiveReverseArray? = host

    override fun setHost(host: RecursiveReverseArray?) {
        this.host = host
    }

    override fun getHostType(): Class<RecursiveReverseArray> = RecursiveReverseArray::class.java

    override fun getHostScanPositions(): Array<BlockPos> = arrayOf(
        pos.offset(33, 7, 0),
        pos.offset(-33, 7, 0),
        pos.offset(0, 7, 33),
        pos.offset(0, 7, -33)
    )

    override fun onConnected(host: RecursiveReverseArray) {
        updateTickSubscription()
    }

    override fun onDisconnected() {
        updateTickSubscription()
    }

    // ========================================
    // Lifecycle
    // ========================================

    override fun onStructureFormed() {
        super.onStructureFormed()
        if (!findAndConnectToHost()) removeFromHost(host)
        updateTickSubscription()
    }

    override fun onStructureInvalid() {
        super.onStructureInvalid()
        removeFromHost(host)
        updateTickSubscription()
    }

    override fun onPartUnload() {
        super.onPartUnload()
        removeFromHost(host)
    }

    override fun onMachineRemoved() {
        removeFromHost(host)
    }

    protected fun updateTickSubscription() {
        if (isFormed && isConnectedToHost) {
            tickSubscription = subscribeServerTick(tickSubscription, ::moduleServerTick)
        } else {
            tickSubscription?.unsubscribe()
            tickSubscription = null
        }
    }

    private fun moduleServerTick() {
        if (enabled) startupUpdate()
        updateTickSubscription()
    }

    override fun clientTick() {
        super.clientTick()
        updateWorkingSound()
    }

    // ========================================
    // Working sound
    // ========================================

    private fun updateWorkingSound() {
        val sound = getWorkingSound()
        if (sound != null && shouldKeepWorkingSound()) {
            val currentSound = workingSound as? AutoReleasedSound
            if (currentSound?.soundEntry == sound && !currentSound.isStopped) return

            currentSound?.release()
            workingSound = sound.playAutoReleasedSound(::shouldKeepWorkingSound, pos, true, 0, 1.0f, 1.0f)
        } else {
            (workingSound as? AutoReleasedSound)?.release()
            workingSound = null
        }
    }

    private fun shouldKeepWorkingSound(): Boolean {
        val currentLevel = level ?: return false
        return ConfigHolder.INSTANCE.machines.machineSounds &&
            shouldPlayWorkingSound() &&
            !isInValid &&
            currentLevel.isLoaded(pos) &&
            getMachine(currentLevel, pos) === this
    }

    open fun startupUpdate() = Unit

    protected open fun getWorkingSound(): SoundEntry? = null

    protected open fun shouldPlayWorkingSound(): Boolean = false

    // ========================================
    // GUI
    // ========================================

    override fun addDisplayText(textList: MutableList<Component>) {
        MultiblockDisplayText.builder(textList, isFormed())
            .setWorkingStatus(enabled, enabled)
            .addComponent(
                "gtladditions.machine.recursive_reverse_array.module.bound".toComponent(
                    Component.literal(if (isConnectedToHost) "✓" else "x")
                        .withStyle(if (isConnectedToHost) ChatFormatting.GREEN else ChatFormatting.RED)
                )
            )
    }

    override fun attachSideTabs(sideTabs: TabsWidget) {
        sideTabs.setMainTab(this)
        CombinedDirectionalFancyConfigurator.of(self(), self())?.let { sideTabs.attachSubTab(it) }
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

    override fun createUI(entityPlayer: Player): ModularUI = ModularUI(198, 208, this, entityPlayer)
        .widget(FancyMachineUIWidget(this, 198, 208))

    // ========================================
    // Metadata
    // ========================================

    open fun getModuleDisplayNameKey(): String = self().blockState.block.descriptionId

    override fun isRemote(): Boolean = super<MultiblockControllerMachine>.isRemote

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            RRFModuleMachine::class.java,
            MultiblockControllerMachine.MANAGED_FIELD_HOLDER
        )
    }
}