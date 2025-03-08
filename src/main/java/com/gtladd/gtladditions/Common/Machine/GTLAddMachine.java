package com.gtladd.gtladditions.Common.Machine;

import com.gtladd.gtladditions.Common.Data.GTLAddCreativeModeTabs;
import com.gtladd.gtladditions.Common.MuiltBlock.MultiBlockMachine;
import com.gtladd.gtladditions.api.Registry.GTLAddRegistration;

public class GTLAddMachine {

    public static void init() {
        MultiBlockMachine.init();
    }

    static {
        GTLAddRegistration.REGISTRATE.creativeModeTab(() -> GTLAddCreativeModeTabs.GTLADD_MACHINE);
    }
}
