package com.gtladd.gtladditions.client.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
inline fun <T> PoseStack.withPose(block: PoseStack.() -> T): T {
    pushPose()
    return try {
        block()
    } finally {
        popPose()
    }
}