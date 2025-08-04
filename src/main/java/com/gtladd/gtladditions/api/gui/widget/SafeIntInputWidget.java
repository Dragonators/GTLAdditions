package com.gtladd.gtladditions.api.gui.widget;

import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;

import com.google.common.primitives.Ints;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SafeIntInputWidget extends IntInputWidget {

    public SafeIntInputWidget(Supplier<Integer> valueSupplier, Consumer<Integer> onChanged) {
        super(valueSupplier, onChanged);
    }

    @Override
    protected Integer add(Integer a, Integer b) {
        return Ints.saturatedCast((long) a + b);
    }
}
