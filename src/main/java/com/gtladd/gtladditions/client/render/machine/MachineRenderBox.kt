package com.gtladd.gtladditions.client.render.machine

import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB

/**
 * A machine renderer that draws outside GTCEu's frustum-culling box, the controller ±viewDistance / 2. The block entity
 * returns this box instead (MetaMachineBlockEntityMixin).
 */
interface MachineRenderBox {
    fun renderBox(blockEntity: BlockEntity): AABB
}
