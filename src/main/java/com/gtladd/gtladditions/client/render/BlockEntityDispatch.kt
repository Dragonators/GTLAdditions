package com.gtladd.gtladditions.client.render

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object BlockEntityDispatch {
    private var mainView: BlockEntity? = null

    @JvmStatic
    fun begin(blockEntity: BlockEntity, source: MultiBufferSource) {
        // Recorded before Iris wraps the level's own buffer source for block entity ids.
        mainView = blockEntity.takeIf { source === Minecraft.getInstance().renderBuffers().bufferSource() }
    }

    @JvmStatic
    fun end() {
        mainView = null
    }

    fun isMainView(blockEntity: BlockEntity): Boolean = mainView === blockEntity
}