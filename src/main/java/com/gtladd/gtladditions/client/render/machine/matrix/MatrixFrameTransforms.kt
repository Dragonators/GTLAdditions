package com.gtladd.gtladditions.client.render.machine.matrix

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Quaternionf
import kotlin.math.cos
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
internal object MatrixFrameTransforms {
    fun applyStructure(pose: PoseStack, yaw: Float) {
        pose.translate(0.5, 0.0, 0.5)
        pose.mulPose(Axis.YP.rotationDegrees(yaw))
        pose.translate(-0.5, 0.0, MatrixEffectGeometry.CENTER_OFFSET_Z - 0.5)
    }

    /** Centre of the nested frames in world space. */
    fun cubeCenter(pos: BlockPos, yaw: Float): Vec3 {
        val angle = Math.toRadians(yaw.toDouble())
        return Vec3(
            pos.x + 0.5 + MatrixEffectGeometry.CENTER_OFFSET_Z * sin(angle),
            pos.y + MatrixEffectGeometry.CUBE_CENTER_Y.toDouble(),
            pos.z + 0.5 + MatrixEffectGeometry.CENTER_OFFSET_Z * cos(angle)
        )
    }

    fun apply(pose: PoseStack, index: Int, ticks: Double, yaw: Float) {
        val layer = MatrixEffectGeometry.layers[index]
        applyStructure(pose, yaw)
        pose.translate(0.5, MatrixEffectGeometry.CUBE_CENTER_Y.toDouble(), 0.5)
        // Blender model (x,y,z) -> Minecraft (x,z,-y), followed by local XYZ Euler rotation.
        pose.mulPose(Axis.XP.rotationDegrees(-90f))
        pose.mulPose(Axis.ZP.rotationDegrees(layer.angles[2]))
        pose.mulPose(Axis.YP.rotationDegrees(layer.angles[1]))
        pose.mulPose(Axis.XP.rotationDegrees(layer.angles[0]))
        pose.mulPose(
            Quaternionf().rotationAxis(
                Math.toRadians(MatrixEffectGeometry.angle(ticks, layer.speed).toDouble()).toFloat(),
                layer.axis[0],
                layer.axis[1],
                layer.axis[2]
            )
        )
    }
}
