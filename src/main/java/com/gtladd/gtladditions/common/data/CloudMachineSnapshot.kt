package com.gtladd.gtladditions.common.data

import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack

class CloudMachineSnapshot(
    val dimensionId: String,
    pos: BlockPos,
    frontPos: BlockPos?,
    item: ItemStack,
    val currentCwu: Long,
    val maxCwu: Long,
    val requestedCwu: Int
) {

    val pos: BlockPos = pos.immutable()
    val frontPos: BlockPos? = frontPos?.immutable()
    val item: ItemStack = item.copy()
}