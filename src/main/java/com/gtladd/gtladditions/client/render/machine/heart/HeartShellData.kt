package com.gtladd.gtladditions.client.render.machine.heart

import com.gtladd.gtladditions.GTLAdditions
import net.minecraft.server.packs.resources.ResourceProvider
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

/**
 * Prefab shells: each shell repeats one unit-cube master on every plate pose.
 * - Directions and positions are for a south-facing controller, from the black hole's centre.
 * - Masters are in the plate frame (tangent, bitangent, normal toward the hole); a cube's depth counts back from the front face.
 */
@OnlyIn(Dist.CLIENT)
object HeartShellData {
    private val location = GTLAdditions.id("models/effect/heart/shells.bin.gz")

    /** Pattern symbols of master materials 0..10; materials from [FRAME] are the frame's six sectors. */
    private const val SYMBOLS = "WwFBATUGLSD"
    private const val FRAME = 11

    /** Rows y = -16..15 of a master's collector outline, and the long-run circuit tile's size. */
    const val ROWS = 32
    const val TILE = 32

    class Cube(val x: Int, val y: Int, val depth: Int, private val material: Int) {
        /** Rim plates turn the frame sectors facing the opening into lit edge; structural nodes turn collectors and light bars into node casing. */
        fun symbol(plate: Plate): Char {
            if (material >= FRAME) return if (plate.rimMask shr (material - FRAME) and 1 != 0) 'S' else 'F'
            val symbol = SYMBOLS[material]
            return if (plate.node && symbol in "WwL") 'D' else symbol
        }
    }

    class Plate(
        val tangent: Vector3f,
        val bitangent: Vector3f,
        val normal: Vector3f,
        val center: Vector3f,
        val angle: Float,
        val rimMask: Int,
        val node: Boolean
    ) {
        /** Master block coordinates (front cubes at depth 0, block centres on integers) to the shell frame. */
        fun pose(levels: Int): Matrix4f = Matrix4f(
            tangent.x, tangent.y, tangent.z, 0f,
            bitangent.x, bitangent.y, bitangent.z, 0f,
            normal.x, normal.y, normal.z, 0f,
            center.x, center.y, center.z, 1f
        ).translate(0f, 0f, levels / 2f - 0.5f)
    }

    class Shell(
        val id: Int,
        val axis: Vector3f,
        val sweep: Vector3f,
        val maxAngle: Float,
        val levels: Int,
        val cubes: List<Cube>,
        /** (x0, x1) of each collector row; x0 > x1 leaves the row empty. */
        val rows: ByteArray,
        val plates: List<Plate>
    )

    class Data(val shells: List<Shell>, val tile: ByteArray)

    fun read(resources: ResourceProvider): Data = DataInputStream(
        GZIPInputStream(resources.getResourceOrThrow(location).open()).buffered(1 shl 16)
    ).use { input ->
        val magic = ByteArray(7)
        input.readFully(magic)
        check(String(magic, Charsets.US_ASCII) == "GTLAHS2") { "Invalid Heart shell data: $location" }
        val shellCount = input.readUnsignedByte()
        check(shellCount == HeartShellRenderer.SHELLS) { "Invalid Heart shell count: $shellCount" }
        val shells = List(shellCount) { index ->
            val axis = input.readVector()
            val sweep = input.readVector()
            val maxAngle = input.readFloat()
            val levels = input.readUnsignedByte()
            val cubes = List(input.readUnsignedShort()) {
                Cube(input.readByte().toInt(), input.readByte().toInt(), input.readUnsignedByte(), input.readUnsignedByte())
            }

            val rows = ByteArray(ROWS * 2)
            input.readFully(rows)

            val count = input.readUnsignedShort()
            check(count in 1..HeartShellRenderer.MAX_PLATES) { "Invalid Heart plate count: $count" }
            val plates = List(count) {
                Plate(
                    input.readVector(),
                    input.readVector(),
                    input.readVector(),
                    input.readVector(),
                    input.readFloat(),
                    input.readUnsignedByte(),
                    input.readUnsignedByte() != 0
                )
            }

            Shell(index + 1, axis, sweep, maxAngle, levels, cubes, rows, plates)
        }

        check(input.readUnsignedByte() == TILE) { "Invalid Heart circuit tile" }
        val tile = ByteArray(TILE * TILE)
        input.readFully(tile)
        check(input.read() == -1) { "Trailing bytes in $location" }
        Data(shells, tile)
    }

    private fun DataInputStream.readVector() = Vector3f(readFloat(), readFloat(), readFloat())
}
