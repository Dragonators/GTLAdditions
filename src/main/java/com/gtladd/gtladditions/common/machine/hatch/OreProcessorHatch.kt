package com.gtladd.gtladditions.common.machine.hatch

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine
import com.gtladd.gtladditions.api.gui.widget.SliderTextFieldWidget
import com.gtladd.gtladditions.utils.ComponentExtensions.toComponent
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import com.lowdragmc.lowdraglib.utils.ColorUtils
import net.minecraft.network.FriendlyByteBuf
import org.gtlcore.gtlcore.api.gui.ExtendLabelWidget
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

class OreProcessorHatch(holder: IMachineBlockEntity) :
    MultiblockPartMachine(holder),
    IMachineLife,
    IDropSaveMachine {
    @Persisted
    @DropSaved
    @DescSynced
    val color: Int = ColorUtils.randomColor()

    @Persisted
    @DropSaved
    @DescSynced
    private var red = 0

    @Persisted
    @DropSaved
    @DescSynced
    private var green = 0

    @Persisted
    @DropSaved
    @DescSynced
    private var blue = 0

    val matchAll get() = this.red == color.red && this.green == color.green && this.blue == color.blue
    val firstTier: Int get() = findTier(this.red, color.red)
    val secondTier: Int get() = findTier(this.green, color.green)
    val thirdTier: Int get() = findTier(this.blue, color.blue)
    val parallelMultiplier: Int
        get() = when (firstTier) {
            3 -> 10
            2 -> 8
            1 -> 6
            else -> 4
        }
    val advancedThreadBonus: Int
        get() = when (firstTier) {
            3 -> 144
            2 -> 128
            1 -> 96
            else -> 72
        }
    val itemChanceBoost: Int get() = secondTier + 1
    val durationMultiplier: Double
        get() = when (thirdTier) {
            3 -> 0.4
            2 -> 0.5
            1 -> 0.65
            else -> 0.8
        }
    val fluidMultiplier: Double
        get() = when (thirdTier) {
            3 -> 0.4
            2 -> 0.5
            1 -> 0.65
            else -> 0.8
        }

    private fun findTier(x: Int, t: Int): Int = when {
        matchRange(x, t, 5) -> 3
        matchRange(x, t, 35) -> 2
        matchRange(x, t, 80) -> 1
        else -> 0
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, 186, 130)
        val container = WidgetGroup(4, 4, 178, 120)
        container.setBackground(GuiTextures.BACKGROUND_INVERSE)
        container.addWidget(ExtendLabelWidget(8, 4, "gtceu.machine.ore_hatch.gui.1".toComponent))
        container.addWidget(HatchChannelGroup(0, 16, color.red, { this.red }, { this.red = it }))
        container.addWidget(ExtendLabelWidget(8, 42, "gtceu.machine.ore_hatch.gui.2".toComponent))
        container.addWidget(HatchChannelGroup(0, 54, color.green, { this.green }, { this.green = it }))
        container.addWidget(ExtendLabelWidget(8, 80, "gtceu.machine.ore_hatch.gui.3".toComponent))
        container.addWidget(HatchChannelGroup(0, 92, color.blue, { this.blue }, { this.blue = it }))
        group.addWidget(container)
        return group
    }

    override fun getFieldHolder() = MANAGED_FIELD_HOLDER

    companion object {
        val MANAGED_FIELD_HOLDER = ManagedFieldHolder(OreProcessorHatch::class.java, MultiblockPartMachine.MANAGED_FIELD_HOLDER)
        private val RED_LAMP = ResourceTexture("gtladditions:textures/gui/red_lamp.png")
        private val GREEN_LAMP = ResourceTexture("gtladditions:textures/gui/green_lamp.png")
        private fun matchRange(x: Int, t: Int, range: Int): Boolean = x.isRange(t.safeLimitMinus(range, 0), t.safeLimitPlus(range, 0xff))

        private val Int.red: Int get() = (this shr 16) and 0xff
        private val Int.green: Int get() = (this shr 8) and 0xff
        private val Int.blue: Int get() = this and 0xff
        private fun Int.isRange(min: Int, max: Int): Boolean = this in min..max
        private fun Int.safeLimitPlus(plus: Int, limit: Int): Int = min((this + plus), limit)
        private fun Int.safeLimitMinus(minus: Int, limit: Int): Int = max((this - minus), limit)

        private class HatchChannelGroup(x: Int, y: Int, t: Int, v: Supplier<Int>, c: Consumer<Int>) : WidgetGroup(x, y, 160, 26) {
            private val sliderTextFieldWidget: SliderTextFieldWidget
            private val lamp1: ImageWidget
            private val lamp2: ImageWidget
            private val lamp3: ImageWidget
            private val target: Int = t

            init {
                this.sliderTextFieldWidget = SliderTextFieldWidget(
                    6,
                    2,
                    80,
                    24,
                    0,
                    255,
                    v,
                    c
                ) { this.update(it) }
                this.lamp1 = ImageWidget(100, 1, 20, 20, if (matchFirst(v.get())) GREEN_LAMP else RED_LAMP)
                this.lamp2 = ImageWidget(122, 1, 20, 20, if (matchSecond(v.get())) GREEN_LAMP else RED_LAMP)
                this.lamp3 = ImageWidget(144, 1, 20, 20, if (matchThird(v.get())) GREEN_LAMP else RED_LAMP)

                this.addWidget(this.sliderTextFieldWidget).addWidget(lamp1).addWidget(lamp2).addWidget(lamp3)
            }

            private fun matchFirst(x: Int): Boolean = matchRange(x, target, 80)
            private fun matchSecond(x: Int): Boolean = matchRange(x, target, 35)
            private fun matchThird(x: Int): Boolean = matchRange(x, target, 5)

            private fun update(x: Int) = writeUpdateInfo(16) { it.writeInt(x) }

            override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
                super.readUpdateInfo(id, buffer)
                if (id == 16) {
                    val value = buffer.readInt()
                    lamp1.setImage { if (matchFirst(value)) GREEN_LAMP else RED_LAMP }
                    lamp2.setImage { if (matchSecond(value)) GREEN_LAMP else RED_LAMP }
                    lamp3.setImage { if (matchThird(value)) GREEN_LAMP else RED_LAMP }
                }
            }
        }
    }
}