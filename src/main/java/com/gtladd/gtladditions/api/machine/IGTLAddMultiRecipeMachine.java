package com.gtladd.gtladditions.api.machine;

public interface IGTLAddMultiRecipeMachine extends IWirelessThreadModifierParallelMachine {

    default int getLimitedDuration() {
        return 20;
    }

    default void setLimitedDuration(int duration) {}
}
