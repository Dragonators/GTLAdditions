package com.gtladd.gtladditions.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object SubspaceNexusLayout {
    val metadata: JsonObject by lazy {
        val stream = checkNotNull(
            javaClass.classLoader.getResourceAsStream(
                "assets/gtladditions/subspace_nexus/structure.json"
            )
        )
        InputStreamReader(stream, StandardCharsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }
    }

    private data class Slot(val index: Int, val offset: BlockPos, val facing: Direction)

    private val slotsByFacing by lazy {
        val base = metadata.getAsJsonArray("moduleOffsets").mapIndexed { index, entry ->
            val value = entry.asJsonObject
            val offset = value.getAsJsonArray("offset")
            Slot(
                index,
                BlockPos(offset[0].asInt, offset[1].asInt, offset[2].asInt),
                Direction.valueOf(value.get("module_facing").asString)
            )
        }

        Direction.Plane.HORIZONTAL.associateWith { facing ->
            base.map { slot ->
                val moduleFacing = when (facing) {
                    Direction.EAST -> slot.facing
                    Direction.SOUTH -> slot.facing.clockWise
                    Direction.WEST -> slot.facing.opposite
                    Direction.NORTH -> slot.facing.counterClockWise
                    else -> error("Nexus facing must be horizontal")
                }
                slot.copy(offset = rotateEast(slot.offset, facing), facing = moduleFacing)
            }
        }
    }

    private val slotLookup by lazy { slotsByFacing.mapValues { (_, slots) -> slots.associateBy { it.offset } } }

    fun rotateEast(pos: BlockPos, facing: Direction): BlockPos = when (facing) {
        Direction.EAST -> pos
        Direction.SOUTH -> BlockPos(-pos.z, pos.y, pos.x)
        Direction.WEST -> BlockPos(-pos.x, pos.y, -pos.z)
        Direction.NORTH -> BlockPos(pos.z, pos.y, -pos.x)
        else -> error("Nexus facing must be horizontal")
    }

    fun modulePositions(host: BlockPos, facing: Direction): Array<BlockPos> =
        slotsByFacing.getValue(facing).map { host.offset(it.offset) }.toTypedArray()

    fun moduleFacing(facing: Direction, index: Int): Direction = slotsByFacing.getValue(facing)[index].facing

    fun moduleIndex(host: BlockPos, facing: Direction, module: BlockPos): Int =
        slotLookup[facing]?.get(module.subtract(host))?.index ?: -1

    /** Design-space point (Z up) to a position relative to the controller block's corner. */
    fun point(x: Double, y: Double, z: Double, facing: Direction): Vec3 {
        val eastX = -y - 130.5
        val up = z - 31.5
        val eastZ = 21.5 - x

        val rotated = when (facing) {
            Direction.EAST -> Vec3(eastX, up, eastZ)
            Direction.SOUTH -> Vec3(-eastZ, up, eastX)
            Direction.WEST -> Vec3(-eastX, up, -eastZ)
            Direction.NORTH -> Vec3(eastZ, up, -eastX)
            else -> error("Nexus facing must be horizontal")
        }

        return rotated.add(0.5, 0.5, 0.5)
    }
}
