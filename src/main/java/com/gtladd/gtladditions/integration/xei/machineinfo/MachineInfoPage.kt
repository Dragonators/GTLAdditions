package com.gtladd.gtladditions.integration.xei.machineinfo

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gregtechceu.gtceu.utils.GTUtil
import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.utils.ComponentExtensions.literal
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.TankWidget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.jei.IngredientIO
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.utils.CycleFluidTransfer
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler
import com.lowdragmc.lowdraglib.utils.Size
import guideme.Guides
import guideme.GuidesCommon
import guideme.indices.ItemIndex
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Items
import org.gtlcore.gtlcore.utils.NumberUtils
import org.gtlcore.gtlcore.utils.TextUtil

class MachineInfoPage(val machine: MachineDefinition, private val eut: Long, private val fluidsPerSecond: List<FluidStack> = emptyList()) {
    // Sorted before gtceu:multiblock_info, so a fresh JEI config opens this tab before the structure preview.
    val id: ResourceLocation = GTCEu.id("gtladditions/" + machine.id.path)

    fun createWidget(): WidgetGroup {
        val root = WidgetGroup(0, 0, WIDTH, 0)

        val rowWidth = if (fluidsPerSecond.isEmpty()) SLOT else fluidsPerSecond.size * SLOT + 2 * ARROW_GAP + ARROW + SLOT
        var x = (WIDTH - rowWidth) / 2
        for (fluid in fluidsPerSecond) {
            root.addWidget(tank(fluid, x, TOP))
            x += SLOT
        }
        if (fluidsPerSecond.isNotEmpty()) {
            root.addWidget(ImageWidget(x + ARROW_GAP, TOP - 1, ARROW, ARROW, ARROW_TEXTURE))
            x += 2 * ARROW_GAP + ARROW
        }
        root.addWidget(
            SlotWidget(CycleItemStackHandler(listOf(listOf(machine.asStack()))), 0, x, TOP, false, false).apply {
                setBackgroundTexture(GuiTextures.SLOT)
                setIngredientIO(IngredientIO.RENDER_ONLY)
            }
        )
        root.addWidget(
            ButtonWidget(WIDTH - 3 - BUTTON, 3, BUTTON, BUTTON, GUIDE_BUTTON) { openGuide() }
                .setHoverTexture(GUIDE_BUTTON_HOVER)
                .setHoverTooltips("$KEY.open_guide")
                .setClientSideWidget()
        )

        val tier = GTUtil.getTierByVoltage(eut).toInt()
        var y = TOP + SLOT + 8
        root.addWidget(label(3, y, "$KEY.subtitle".toComponent))
        for (fluid in fluidsPerSecond) {
            y += LINE
            root.addWidget(label(3, y, "$KEY.fluid_upkeep".toComponent(FormattingUtil.formatNumbers(fluid.amount / 1000), fluid.displayName)))
        }
        y += LINE
        root.addWidget(label(3, y, energyLine(tier)))
        val tierText = GTValues.VNF[tier]
        root.addWidget(LabelWidget(WIDTH - 3 - Minecraft.getInstance().font.width(tierText), y, tierText).setTextColor(-1).setDropShadow(false))

        root.setSize(Size(WIDTH, y + LINE))
        return root
    }

    // Same wording and amperage suffix as GTLCore's recipe pages.
    private fun energyLine(tier: Int): Component = "gtceu.recipe.eu".toComponent(NumberUtils.formatLong(eut)).append(
        " (".literal.withStyle(ChatFormatting.GREEN)
            .append((NumberUtils.formatDouble(eut.toDouble() / GTValues.V[tier]) + "A").literal.withStyle(TextUtil.`GTL_CORE$VC`[tier]))
            .append(")".literal.withStyle(ChatFormatting.GREEN))
    )

    private fun label(x: Int, y: Int, text: Component) = LabelWidget(x, y, text).setTextColor(-1).setDropShadow(true)

    private fun openGuide() {
        val guide = Guides.getById(GUIDE) ?: return
        GuidesCommon.openGuide(Minecraft.getInstance().player, GUIDE, guide.getIndex(ItemIndex::class.java)[machine.id])
    }

    private fun tank(fluid: FluidStack, x: Int, y: Int) =
        TankWidget(CycleFluidTransfer(listOf(listOf(fluid))), 0, x, y, SLOT, SLOT, false, false)
            .setBackground(GuiTextures.FLUID_SLOT)
            .setFillDirection(FillDirection.ALWAYS_FULL)
            .setShowAmount(false)
            .setIngredientIO(IngredientIO.INPUT)
            // Reads the shown fluid now; a recipe-viewer page never receives the server sync that would.
            .setClientSideWidget()

    companion object {
        private const val KEY = "gtladditions.machine_info"
        private val GUIDE = GTLAdditions.id("guide")

        private const val WIDTH = 176
        private const val TOP = 6
        private const val SLOT = 18
        private const val ARROW = 20
        private const val ARROW_GAP = 6
        private const val LINE = 10
        private const val BUTTON = 14

        private val ARROW_TEXTURE = GuiTextures.PROGRESS_BAR_ARROW.getSubTexture(0.0, 0.0, 1.0, 0.5)
        private val GUIDE_ICON = ItemStackTexture(Items.BOOK).scale(10 / 14f)
        private val GUIDE_BUTTON = GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, GUIDE_ICON)
        private val GUIDE_BUTTON_HOVER = GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, ColorRectTexture(0x30FFFFFF), GUIDE_ICON)
    }
}
