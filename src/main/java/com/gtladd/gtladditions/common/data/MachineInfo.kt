package com.gtladd.gtladditions.common.data

import net.minecraft.core.Direction

data class MachineInfo(
    val posLong: Long,
    val facing: Direction,
    val type: StructureRenderType,
    val upwardsFacing: Direction,
    val flipped: Boolean
)