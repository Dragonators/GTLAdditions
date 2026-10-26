package com.gtladd.gtladditions.api.machine

import com.gtladd.gtladditions.common.machine.multiblock.controller.TimeSpaceDistorter
import net.minecraft.world.level.Level

interface ITimeSpaceDistorterSource : IWirelessBindableSource<TimeSpaceDistorter> {
    fun getLevel(): Level?

    fun disconnectTSD(target: TimeSpaceDistorter?, clearBinding: Boolean)
}