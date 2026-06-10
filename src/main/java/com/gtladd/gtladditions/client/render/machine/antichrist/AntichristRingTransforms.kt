package com.gtladd.gtladditions.client.render.machine.antichrist

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.core.Direction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Quaternionf

@OnlyIn(Dist.CLIENT)
object AntichristRingTransforms {
    fun apply(profile: AntichristRenderProfile, ringIndex: Int, poseStack: PoseStack) {
        poseStack.translate(profile.starPos.x, profile.starPos.y, profile.starPos.z)

        when (profile.facing) {
            Direction.NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270f))
            Direction.SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90f))
            Direction.WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(0f))
            Direction.EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            else -> {}
        }

        if (profile.isWorking) {
            val direction = if (ringIndex == 1) -1.0f else 1.0f
            val speedMultiplier = 0.4f + ringIndex * 0.4f
            val angleOffset = ringIndex * 120f
            val rotationAngle = (profile.tick * speedMultiplier * 2.0f * direction + angleOffset) % 360.0f

            poseStack.mulPose(Quaternionf().fromAxisAngleDeg(1.0f, 0.0f, 0.0f, rotationAngle))
        }
    }
}