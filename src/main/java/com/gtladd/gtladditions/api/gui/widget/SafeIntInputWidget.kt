package com.gtladd.gtladditions.api.gui.widget

import com.google.common.primitives.Ints
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget
import java.util.function.Consumer
import java.util.function.Supplier

class SafeIntInputWidget(valueSupplier: Supplier<Int?>?, onChanged: Consumer<Int?>?) :
    IntInputWidget(valueSupplier, onChanged) {
    override fun add(a: Int?, b: Int): Int = Ints.saturatedCast(a!!.toLong() + b)
}
