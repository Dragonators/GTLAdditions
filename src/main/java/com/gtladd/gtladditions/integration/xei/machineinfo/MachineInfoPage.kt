package com.gtladd.gtladditions.integration.xei.machineinfo

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.utils.FormattingUtil
import com.gregtechceu.gtceu.utils.GTUtil
import com.gtladd.gtladditions.GTLAdditions
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection
import com.lowdragmc.lowdraglib.gui.texture.TextTexture
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget
import com.lowdragmc.lowdraglib.gui.widget.TankWidget
import com.lowdragmc.lowdraglib.gui.widget.TextBoxWidget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.jei.IngredientIO
import com.lowdragmc.lowdraglib.side.fluid.FluidStack
import com.lowdragmc.lowdraglib.utils.CycleFluidTransfer
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler
import com.lowdragmc.lowdraglib.utils.LocalizationUtils
import com.lowdragmc.lowdraglib.utils.Size
import guideme.Guides
import guideme.GuidesCommon
import guideme.indices.ItemIndex
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation

class MachineInfoPage(val machine: MachineDefinition, private val eut: Long, private val fluidsPerSecond: List<FluidStack> = emptyList()) {
    // Sorted before gtceu:multiblock_info, so a fresh JEI config opens this tab before the structure preview.
    val id: ResourceLocation = GTCEu.id("gtladditions/" + machine.id.path)

    fun createWidget(): WidgetGroup {
        val root = WidgetGroup(0, 0, WIDTH, 0)
        root.addWidget(
            SlotWidget(CycleItemStackHandler(listOf(listOf(machine.asStack()))), 0, PADDING, PADDING, false, false).apply {
                setBackgroundTexture(SLOT_BACKGROUND)
                setIngredientIO(IngredientIO.RENDER_ONLY)
            }
        )
        val titleX = PADDING + SLOT + 5
        val title = text(titleX, PADDING, WIDTH - titleX - PADDING, listOf(machine.descriptionId), ACCENT)
        root.addWidget(title)
        val subtitle = text(titleX, PADDING + title.sizeHeight, WIDTH - titleX - PADDING, listOf("$KEY.subtitle"), MUTED)
        root.addWidget(subtitle)
        var y = maxOf(PADDING + SLOT, subtitle.selfPositionY + subtitle.sizeHeight) + 4
        root.addWidget(ImageWidget(PADDING, y, WIDTH - 2 * PADDING, 1, ColorRectTexture(DIVIDER)))
        y += 6

        val slotsX = WIDTH - PADDING - fluidsPerSecond.size * (SLOT + 1) + 1
        fluidsPerSecond.forEachIndexed { index, fluid -> root.addWidget(tank(fluid, slotsX + index * (SLOT + 1), y)) }
        val body = text(PADDING, y, slotsX - PADDING - 4, upkeepLines(), BODY).setSpace(2)
        root.addWidget(body)
        y = maxOf(body.selfPositionY + body.sizeHeight, if (fluidsPerSecond.isEmpty()) 0 else y + SLOT) + 6

        root.addWidget(
            ButtonWidget(
                WIDTH - PADDING - BUTTON_WIDTH,
                y,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                button(0xFF232931.toInt(), BUTTON_BORDER)
            ) { openGuide() }
                .setHoverTexture(button(0xFF2A3A44.toInt(), ACCENT))
                .setClientSideWidget()
        )
        root.setSize(Size(WIDTH, y + BUTTON_HEIGHT + PADDING))
        root.setBackground(GuiTextureGroup(ColorRectTexture(PANEL).setRadius(4f), ColorBorderTexture(1, BORDER).setRadius(4f)))
        return root
    }

    private fun upkeepLines(): List<String> = listOf(
        LocalizationUtils.format(
            "$KEY.energy_upkeep",
            FormattingUtil.formatNumbers(eut),
            GTValues.VNF[GTUtil.getTierByVoltage(eut).toInt()]
        )
    ) + fluidsPerSecond.map {
        LocalizationUtils.format("$KEY.fluid_upkeep", FormattingUtil.formatNumbers(it.amount / 1000), it.displayName.string)
    }

    private fun openGuide() {
        val guide = Guides.getById(GUIDE) ?: return
        GuidesCommon.openGuide(Minecraft.getInstance().player, GUIDE, guide.getIndex(ItemIndex::class.java)[machine.id])
    }

    private fun text(x: Int, y: Int, width: Int, lines: List<String>, color: Int) = TextBoxWidget(x, y, width, lines).setFontColor(color)

    private fun tank(fluid: FluidStack, x: Int, y: Int) =
        TankWidget(CycleFluidTransfer(listOf(listOf(fluid))), 0, x, y, SLOT, SLOT, false, false)
            .setBackground(SLOT_BACKGROUND)
            .setFillDirection(FillDirection.ALWAYS_FULL)
            .setShowAmount(false)
            .setIngredientIO(IngredientIO.INPUT)
            // Reads the shown fluid now; a recipe-viewer page never receives the server sync that would.
            .setClientSideWidget()

    private fun button(fill: Int, border: Int): IGuiTexture = GuiTextureGroup(
        ColorRectTexture(fill).setRadius(3f),
        ColorBorderTexture(1, border).setRadius(3f),
        TextTexture("$KEY.open_guide", ACCENT).setDropShadow(false)
    )

    companion object {
        private const val KEY = "gtladditions.machine_info"
        private val GUIDE = GTLAdditions.id("guide")

        private const val WIDTH = 240
        private const val PADDING = 6
        private const val SLOT = 18
        private const val BUTTON_WIDTH = 64
        private const val BUTTON_HEIGHT = 14

        private const val PANEL = 0xFF181C22.toInt()
        private const val BORDER = 0xFF33505A.toInt()
        private const val BUTTON_BORDER = 0xFF3C5A64.toInt()
        private const val DIVIDER = 0x4455FFFF
        private const val ACCENT = 0xFF55FFFF.toInt()
        private const val BODY = 0xFFD6DCE2.toInt()
        private const val MUTED = 0xFF8C96A0.toInt()

        private val SLOT_BACKGROUND: IGuiTexture = GuiTextureGroup(
            ColorRectTexture(0xFF232931.toInt()).setRadius(2f),
            ColorBorderTexture(1, 0xFF3C4852.toInt()).setRadius(2f)
        )
    }
}
