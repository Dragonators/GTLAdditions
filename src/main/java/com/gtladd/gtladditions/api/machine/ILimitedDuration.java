package com.gtladd.gtladditions.api.machine;

public interface ILimitedDuration {

    default int getLimitedDuration() {
        return 20;
    }

    default void setLimitedDuration(int duration) {}
}
