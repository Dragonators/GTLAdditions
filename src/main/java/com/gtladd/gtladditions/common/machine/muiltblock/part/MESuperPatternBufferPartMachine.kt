package com.gtladd.gtladditions.common.machine.muiltblock.part

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.ButtonConfigurator
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AETextInputButtonWidget
import com.gregtechceu.gtceu.utils.ResearchManager
import com.gtladd.gtladditions.utils.CommonUtils
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup
import com.lowdragmc.lowdraglib.gui.util.ClickData
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import net.minecraft.core.BlockPos
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.gtlcore.gtlcore.api.gui.MEPatternCatalystUIManager
import org.gtlcore.gtlcore.common.machine.multiblock.part.PaginationUIManager
import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEPatternBufferPartMachine
import kotlin.math.max

class MESuperPatternBufferPartMachine @JvmOverloads constructor(
    holder: IMachineBlockEntity?,
    patternsPerRow: Int = 9,
    rowsPerPage: Int = 6,
    maxPages: Int = 3
) : MEPatternBufferPartMachine(holder, patternsPerRow * rowsPerPage * maxPages, IO.BOTH) {
    private val paginationUIManager: PaginationUIManager

    init {
        val uiWidth = max(patternsPerRow * 18 + 16, 106)
        val uiHeight = rowsPerPage * 18 + 28
        paginationUIManager = PaginationUIManager(
            patternsPerRow, rowsPerPage, maxPages,
            uiWidth, uiHeight,
            { index: Int -> this.onPatternChange(index) },
            { i: Int? -> cacheRecipe[i!!] },
            patternInventory
        )
    }

    // ========================================
    // DATASTICK INTERACTION
    // ========================================
    override fun onUse(
        state: BlockState?, world: Level, pos: BlockPos, player: Player,
        hand: InteractionHand, hit: BlockHitResult?
    ): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (stack.isEmpty) return InteractionResult.PASS

        if (stack.`is`(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!world.isClientSide) {
                // Check if it's research data - if so, pass to avoid conflicts
                val researchData = ResearchManager.readResearchId(stack)
                if (researchData != null) {
                    return InteractionResult.PASS
                }

                if (stack.getOrCreateTag().contains("pos", Tag.TAG_INT_ARRAY.toInt())) {
                    stack.getOrCreateTag().remove("pos")
                }

                // Store this pattern buffer's position in the data stick
                stack.getOrCreateTag().putIntArray("superPos", intArrayOf(pos.x, pos.y, pos.z))
                player.sendSystemMessage(Component.translatable("gtceu.machine.me.import_copy_settings"))
            }
            return InteractionResult.sidedSuccess(world.isClientSide)
        }

        return InteractionResult.PASS
    }

    // ========================================
    // GUI SYSTEM
    // ========================================
    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, paginationUIManager.uiWidth, paginationUIManager.uiHeight)

        // ME Network status indicator
        group.addWidget(
            LabelWidget(
                8, 2
            ) { if (this.isOnline) "gtceu.gui.me_network.online" else "gtceu.gui.me_network.offline" }
        )

        // Custom name input widget
        group.addWidget(
            AETextInputButtonWidget(paginationUIManager.uiWidth - 78, 2, 70, 10)
                .setText(customName)
                .setOnConfirm { customName: String? -> this.setCustomName(customName) }
                .setButtonTooltips(Component.translatable("gui.gtceu.rename.desc"))
        )

        val catalystUIManager = MEPatternCatalystUIManager(
            group.sizeWidth + 4,
            catalystItems,
            catalystFluids,
            cacheRecipeCount
        ) { slot: Int -> this.removeSlotFromGTRecipeCache(slot) }
        group.waitToAdded(catalystUIManager)

        // Create pagination UI using the manager
        group.addWidget(paginationUIManager.createPaginationUI { index: Int ->
            catalystUIManager.toggleFor(
                index
            )
        })

        return group
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        configuratorPanel.attachConfigurators(
            ButtonConfigurator(
                GuiTextureGroup(GuiTextures.GREGTECH_LOGO)
            ) { clickData: ClickData -> this.embedCircuit(clickData) }
                .setTooltips(
                    listOf<Component>(
                        Component.translatable("gui.gtladditions.lock_pattern_circuit.desc.0"),
                        Component.translatable("gui.gtladditions.lock_pattern_circuit.desc.1"),
                        Component.translatable("gui.gtladditions.lock_pattern_circuit.desc.2"),
                        Component.translatable("gui.gtladditions.lock_pattern_circuit.desc.3"),
                        Component.translatable("gui.gtladditions.lock_pattern_circuit.desc.4"),
                        Component.translatable("gui.gtladditions.lock_pattern_circuit.desc.5")
                    )
                )
        )
    }

    private fun embedCircuit(clickData: ClickData) {
        if (clickData.isRemote) return
        if (!clickData.isShiftClick) return

        val circuit = IntCircuitBehaviour.getCircuitConfiguration(sharedCircuitInventory.storage.getStackInSlot(0))
        for (i in 0 until this.terminalPatternInventory.size()) {
            val stack = terminalPatternInventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                val after = CommonUtils.createPatternWithCircuit(stack, circuit, false, level)
                if (!after.isEmpty) {
                    terminalPatternInventory.setItemDirect(i, after)
                }
            }
        }
    }
}
