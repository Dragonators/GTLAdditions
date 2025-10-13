package com.gtladd.gtladditions.api.machine;

import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart;
import org.jetbrains.annotations.NotNull;

public interface IThreadModifierMachine {

    default int getAdditionalThread() {
        return 0;
    }

    default void setThreadPartMachine(@NotNull IThreadModifierPart threadModifierPart) {}
}
