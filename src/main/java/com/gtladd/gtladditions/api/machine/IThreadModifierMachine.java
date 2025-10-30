package com.gtladd.gtladditions.api.machine;

import com.gtladd.gtladditions.api.machine.feature.IThreadModifierPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IThreadModifierMachine {

    default int getAdditionalThread() {
        final var thread = getThreadPartMachine();
        return thread != null ? thread.getThreadCount() : 0;
    }

    default @Nullable IThreadModifierPart getThreadPartMachine() {
        return null;
    }

    default void setThreadPartMachine(@NotNull IThreadModifierPart threadModifierPart) {}
}
