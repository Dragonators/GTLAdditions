package com.gtladd.gtladditions.client.render.machine.matrix

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Quaternionf
import org.lwjgl.opengl.GL11C
import kotlin.math.cos
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
internal object MatrixFrameTransforms {
    fun applyStructure(pose: PoseStack, yaw: Float, flipped: Boolean) {
        pose.translate(0.5, 0.0, 0.5)
        pose.mulPose(Axis.YP.rotationDegrees(yaw))
        if (flipped) pose.scale(-1f, 1f, 1f)
        pose.translate(-0.5, 0.0, MatrixEffectGeometry.CENTER_OFFSET_Z - 0.5)
    }

    /** In world space; the mirror leaves it in place. */
    fun cubeCenter(pos: BlockPos, yaw: Float): Vec3 {
        val angle = Math.toRadians(yaw.toDouble())
        return Vec3(
            pos.x + 0.5 + MatrixEffectGeometry.CENTER_OFFSET_Z * sin(angle),
            pos.y + MatrixEffectGeometry.CUBE_CENTER_Y.toDouble(),
            pos.z + 0.5 + MatrixEffectGeometry.CENTER_OFFSET_Z * cos(angle)
        )
    }

    fun withWinding(flipped: Boolean, render: () -> Unit) {
        if (!flipped) {
            render()
            return
        }

        val winding = GL11C.glGetInteger(GL11C.GL_FRONT_FACE)
        GL11C.glFrontFace(if (winding == GL11C.GL_CCW) GL11C.GL_CW else GL11C.GL_CCW)
        try {
            render()
        } finally {
            GL11C.glFrontFace(winding)
        }
    }

    @JvmOverloads
    fun apply(pose: PoseStack, index: Int, ticks: Double, yaw: Float, flipped: Boolean = false) {
        val layer = MatrixEffectGeometry.layers[index]
        applyStructure(pose, yaw, flipped)
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