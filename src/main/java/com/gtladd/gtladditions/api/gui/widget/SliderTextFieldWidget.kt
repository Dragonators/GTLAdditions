package com.gtladd.gtladditions.api.gui.widget

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.utils.Position
import com.lowdragmc.lowdraglib.utils.Size
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.util.Mth
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.text.toInt

class SliderTextFieldWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val minValue: Int,
    private val maxValue: Int,
    private val valueSupplier: Supplier<Int>,
    private val valueResponder: Consumer<Int>,
    private val onChange: Consumer<Int>
) : WidgetGroup(Position(x, y), Size(width, height)) {

    private val textField = TextFieldWidget(
        0,
        0,
        width,
        14,
        { valueSupplier.get().toString() },
        { this.setCurrentValue(it.toInt()) }
    ).setNumbersOnly(minValue, maxValue)
    private val slider = DraggableScrollableWidgetGroup(0, 15, width, 8)
    private var currentValue: Int = 0

    init {
        this.currentValue = valueSupplier.get()
        textField.setCurrentString(currentValue)
        slider.setScrollable(false).setDraggable(true).setUseScissor(false)
        val sliderPos = ((currentValue - minValue) * 1.0 / (maxValue - minValue) * (size.width - 4)).toInt()
        slider.addWidget(Widget(Position(sliderPos, 0), Size(4, 6)).setBackground(ColorPattern.GREEN.rectTexture()))

        addWidget(ImageWidget(2, 18, size.width - 4, 1, ColorPattern.WHITE.rectTexture()))
        addWidget(slider).addWidget(textField)
    }

    fun setCurrentValue(value: Int): SliderTextFieldWidget {
        this.currentValue = value
        this.valueResponder.accept(value)
        this.textField.setCurrentString(value)
        this.onChange.accept(value)
        if (Minecraft.getInstance().isSameThread) {
            if (!slider.widgets.isEmpty()) {
                val sliderKnob = slider.widgets[0]
                val newPos = ((currentValue - minValue) * 1.0 / (maxValue - minValue) * (sizeWidth - 4)).toInt()
                sliderKnob.setSelfPosition(Position(newPos, 0))
            }
        } else {
            writeUpdateInfo(255) { it.writeInt(currentValue) }
        }
        return this
    }

    override fun handleClientAction(id: Int, buffer: FriendlyByteBuf) {
        super.handleClientAction(id, buffer)
        if (id == 255) {
            val value = buffer.readInt()
            setCurrentValue(value)
            textField.setCurrentString(value)
        }
    }

    override fun detectAndSendChanges() {
        super.detectAndSendChanges()
        val currentSupplierValue = valueSupplier.get()
        if (currentSupplierValue != currentValue) {
            setCurrentValue(currentSupplierValue)
            textField.setCurrentString(currentSupplierValue)
            writeUpdateInfo(255) { it.writeInt(currentSupplierValue) }
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf) {
        super.readUpdateInfo(id, buffer)
        if (id == 255) {
            val value = buffer.readInt()
            setCurrentValue(value)
            textField.setCurrentString(value)
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val result = super.mouseClicked(mouseX, mouseY, button)
        if (slider.isMouseOverElement(mouseX, mouseY)) {
            updateValueFromMousePosition(mouseX)
            return true
        }
        return result
    }

    @OnlyIn(Dist.CLIENT)
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (slider.isMouseOverElement(mouseX, mouseY)) {
            updateValueFromMousePosition(mouseX)
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    private fun updateValueFromMousePosition(mouseX: Double) {
        val sliderPos = slider.position
        val relativeX = mouseX - sliderPos.x
        val progress = Mth.clamp(relativeX / sizeWidth, 0.0, 1.0)
        val newValue = minValue + (progress * (maxValue - minValue)).toInt()
        setCurrentValue(newValue)
        writeClientAction(255) { it.writeInt(currentValue) }
    }

    override fun writeInitialData(buffer: FriendlyByteBuf) {
        super.writeInitialData(buffer)
        buffer.writeInt(currentValue)
    }

    override fun readInitialData(buffer: FriendlyByteBuf) {
        super.readInitialData(buffer)
        val value = buffer.readInt()
        setCurrentValue(value)
        this.textField.setCurrentString(value)
    }
}