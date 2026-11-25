package com.gtladd.gtladditions.api.machine

import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gtladd.gtladditions.common.machine.multiblock.controller.TimeSpaceDistorter
import net.minecraft.core.BlockPos

interface ITimeSpaceDistorterSource : IWirelessBindableSource<TimeSpaceDistorter> {

    var timeSpaceDistorterPos: BlockPos?
    var timeSpaceDistorter: TimeSpaceDistorter?

    override var boundTargetPos: BlockPos?
        get() = timeSpaceDistorterPos
        set(value) {
            timeSpaceDistorterPos = value
        }

    override var boundTarget: TimeSpaceDistorter?
        get() = timeSpaceDistorter
        set(value) {
            timeSpaceDistorter = value
        }

    override fun asValidTarget(machine: MetaMachine?): TimeSpaceDistorter? = machine as? TimeSpaceDistorter

    fun resolveTSD(): TimeSpaceDistorter? = resolveTarget()
}
