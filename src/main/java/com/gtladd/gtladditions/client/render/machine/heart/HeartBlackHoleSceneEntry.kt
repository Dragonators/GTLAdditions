package com.gtladd.gtladditions.client.render.machine.heart

import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
internal data class HeartBlackHoleSceneEntry(
    override val blockEntity: BlockEntity,
    override val profile: HeartBlackHoleRenderProfile
) : HeartBlackHoleRenderEntry