package com.gtladd.gtladditions.common.machine.muiltblock.part

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gtladd.gtladditions.api.machine.IThreadModifierMachine
import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart
import com.gtladd.gtladditions.api.machine.logic.MutableRecipesLogic
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.utils.ThreadMultiplierStrategy.getAdditionalMultiplier
import com.lowdragmc.lowdraglib.gui.widget.*
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack

open class ThreadPartMachine(holder: IMachineBlockEntity) : MultiblockPartMachine(holder), IThreadModifierPart,
    IMachineLife {
    @field:Persisted
    private val astralArrayInventory: ItemStackTransfer = ItemStackTransfer(1)

    private var threadMultiplier = 0
    private var threadCount = 0

    init {
        astralArrayInventory.setFilter { stack: ItemStack? -> astralArrayFilter(stack!!) }
    }

    private fun reCalculateThreadCount() {
        threadCount = Ints.saturatedCast(astralArrayInventory.getStackInSlot(0).count * 64L * threadMultiplier)
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, 150, 70)
        group.addWidget(
            DraggableScrollableWidgetGroup(4, 4, 142, 62).setBackground(GuiTextures.DISPLAY)
                .addWidget(ComponentPanelWidget(4, 5) { list: MutableList<Component?>? ->
                    list!!.add(
                        Component.translatable(
                            "gtladditions.thread_modifier_hatch.thread_multiplier",
                            Component.literal(threadMultiplier.toString()).withStyle(ChatFormatting.GOLD)
                        )
                            .setStyle(
                                Style.EMPTY.withHoverEvent(
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("gtladditions.thread_modifier_hatch.hover")
                                    )
                                )
                            )
                    )
                    list.add(
                        Component.translatable(
                            "gtladditions.thread_modifier_hatch.thread_count", Component.literal(threadCount.toString())
                                .withStyle(ChatFormatting.GOLD)
                        )
                    )
                })
        ).setBackground(GuiTextures.BACKGROUND_INVERSE)
        group.addWidget(
            SlotWidget(astralArrayInventory, 0, 120, 40, true, true)
                .setChangeListener { this.reCalculateThreadCount() }
                .setBackground(GuiTextures.SLOT)
                .setHoverTooltips(Component.translatable("gtladditions.thread_modifier_hatch.base_thread"))
        )
        return group
    }

    override fun onMachineRemoved() {
        clearInventory(astralArrayInventory)
    }

    override fun addedToController(controller: IMultiController) {
        super.addedToController(controller)
        if (controller is IThreadModifierMachine) {
            controller.setThreadPartMachine(this)
            if (controller is IRecipeLogicMachine) {
                (controller.recipeLogic as? MutableRecipesLogic<*>)?.setUseMultipleRecipes(true)
                this.threadMultiplier = getAdditionalMultiplier(controller.self().definition)
            }
        } else this.threadMultiplier = 0
        reCalculateThreadCount()
    }

    override fun removedFromController(controller: IMultiController) {
        super.removedFromController(controller)
        threadMultiplier = 0
        threadCount = 0
    }

    override fun canShared(): Boolean = false

    override fun getThreadCount(): Int = threadCount

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        protected val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(
            ThreadPartMachine::class.java, MultiblockPartMachine.MANAGED_FIELD_HOLDER
        )

        private fun astralArrayFilter(stack: ItemStack): Boolean {
            return stack.`is`(GTLAddItems.ASTRAL_ARRAY.asItem())
        }
    }
}
