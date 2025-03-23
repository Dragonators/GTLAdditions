package com.gtladd.gtladditions.common.machine;

import com.gtladd.gtladditions.api.registry.GTLAddRegistration;
import com.gtladd.gtladditions.common.data.GTLAddCreativeModeTabs;
import com.gtladd.gtladditions.common.machine.muiltblock.MultiBlockMachine;

public class GTLAddMachine {

    public static void init() {
        MultiBlockMachine.init();
    }

    static {
        GTLAddRegistration.REGISTRATE.creativeModeTab(() -> GTLAddCreativeModeTabs.GTLADD_MACHINE);
    }
}
