package com.gtladd.gtladditions.client.render.machine.nexus

import com.gtladd.gtladditions.api.client.scene.PartBounds
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Boxes are in the scene frame: Z up, the star at (0, 0, 88). */
internal object NexusBounds {
    // Orbit lines, traffic lanes and planet links keep at least 4 px of half-width; the other floors are narrower.
    private const val PIXEL_FLOOR = 4.0

    // Star, its billboards (the corona's corners reach 74.34 outside wide frames), planets, belt, dispatch ring and markers.
    private val centre = doubleArrayOf(-75.0, -75.0, 13.0, 75.0, 75.0, 163.0)
    private const val BEAM_RADIUS = 12.62

    // Pad lights, hull, plumes, loading beam, the cable up to 880 and the 38 + 1.6 hand-off ring around each pad axis.
    private val pads = Array(6) { i ->
        val angle = Math.toRadians(60.0 * i)
        val x = 134 * cos(angle)
        val y = 134 * sin(angle)
        doubleArrayOf(x - 39.6, y - 39.6, 27.0, x + 39.6, y + 39.6, 880.0)
    }

    // Screen, floor flow, port, pillar and rays of each station, with their widths.
    private val stations = arrayOf(
        doubleArrayOf(42.9, -94.3, 9.9, 130.3, -21.9, 92.5),
        doubleArrayOf(-33.2, 50.9, 9.9, 33.2, 135.1, 92.5),
        doubleArrayOf(-130.3, -94.3, 9.9, -42.9, -21.9, 92.5)
    )

    // While wide: the flash quad (corners to 266.6), the shell quad and the shock ring.
    private const val FLASH = 267.0

    fun parts(draw: NexusScene.Draw, projection: Matrix4fc, wide: Boolean): Pair<PartBounds, PartBounds> {
        val pose = PoseStack()
        NexusScene.applyController(pose, draw.machine)
        pose.mulPose(Axis.XP.rotationDegrees(-90f))
        val placement = Placement(draw, pose.last().pose(), 2.0 / (projection.m11() * Minecraft.getInstance().window.height))

        val shared = listOf(placement.piece(centre)) + pads.map(placement::piece)
        val center = draw.scene.center
        val opaque = if (center.moon.visible) shared + placement.piece(center.moonMin, center.moonMax) else shared

        val star = NexusFrame.STAR
        val transparent = ArrayList(shared)
        transparent += placement.piece(doubleArrayOf(-BEAM_RADIUS, -BEAM_RADIUS, 10.0, BEAM_RADIUS, BEAM_RADIUS, draw.frame.beamTop))
        stations.mapTo(transparent, placement::piece)
        if (wide) {
            transparent += placement.piece(
                doubleArrayOf(
                    star.x() - FLASH,
                    star.y() - FLASH,
                    star.z() - FLASH,
                    star.x() + FLASH,
                    star.y() + FLASH,
                    star.z() + FLASH
                )
            )
        }

        return PartBounds.union(*opaque.toTypedArray()) to PartBounds.union(*transparent.toTypedArray())
    }

    private class Placement(private val draw: NexusScene.Draw, private val matrix: Matrix4f, private val pixel: Double) {
        private val minimum = Vector3f()
        private val maximum = Vector3f()

        fun piece(min: Vector3d, max: Vector3d) = piece(doubleArrayOf(min.x, min.y, min.z, max.x, max.y, max.z))

        fun piece(box: DoubleArray): PartBounds {
            val camera = draw.camera
            var far = 0.0

            for (corner in 0..7) {
                val x = box[if (corner and 1 == 0) 0 else 3] - camera[0]
                val y = box[if (corner and 2 == 0) 1 else 4] - camera[1]
                val z = box[if (corner and 4 == 0) 2 else 5] - camera[2]
                far = max(far, sqrt(x * x + y * y + z * z))
            }

            val grow = PIXEL_FLOOR * pixel * far
            matrix.transformAab(
                (box[0] - grow).toFloat(),
                (box[1] - grow).toFloat(),
                (box[2] - grow).toFloat(),
                (box[3] + grow).toFloat(),
                (box[4] + grow).toFloat(),
                (box[5] + grow).toFloat(),
                minimum,
                maximum
            )
            val pos = draw.blockEntity.blockPos
            return PartBounds.box(
                pos.x + minimum.x.toDouble(),
                pos.y + minimum.y.toDouble(),
                pos.z + minimum.z.toDouble(),
                pos.x + maximum.x.toDouble(),
                pos.y + maximum.y.toDouble(),
                pos.z + maximum.z.toDouble()
            )
        }
    }
}
