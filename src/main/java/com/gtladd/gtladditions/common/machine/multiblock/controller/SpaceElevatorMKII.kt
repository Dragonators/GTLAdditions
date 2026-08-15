package com.gtladd.gtladditions.common.machine.multiblock.controller

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.gtlcore.gtlcore.common.data.GTLBlocks
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine.ASSEMBLER_MODULE
import org.gtlcore.gtlcore.common.data.machines.AdvancedMultiBlockMachine.RESOURCE_COLLECTION
import org.gtlcore.gtlcore.common.machine.multiblock.electric.SpaceElevatorMachine
import org.gtlcore.gtlcore.utils.MachineUtil
import org.gtlcore.gtlcore.utils.datastructure.ModuleRenderInfo

class SpaceElevatorMKII(holder: IMachineBlockEntity) : SpaceElevatorMachine(holder) {
    override fun getModuleScanPositions(): Array<BlockPos> {
        val powerCore = getPowerCore(pos, level ?: return MachineUtil.EMPTY_POS_ARRAY)
            ?: return MachineUtil.EMPTY_POS_ARRAY
        return MODULE_OFFSETS.map { (x, y, z) -> powerCore.offset(x, y, z) }.toTypedArray()
    }

    override fun getModulesForRendering(): List<ModuleRenderInfo> = listOf(
        module(4, 14, Direction.NORTH, RESOURCE_COLLECTION),
        module(4, -8, Direction.SOUTH, ASSEMBLER_MODULE),
        module(-4, 14, Direction.NORTH, RESOURCE_COLLECTION),
        module(-4, -8, Direction.SOUTH, ASSEMBLER_MODULE),
        module(11, 7, Direction.WEST, RESOURCE_COLLECTION),
        module(-11, 7, Direction.EAST, ASSEMBLER_MODULE),
        module(11, -1, Direction.WEST, RESOURCE_COLLECTION),
        module(-11, -1, Direction.EAST, ASSEMBLER_MODULE),
        module(18, 11, Direction.SOUTH, RESOURCE_COLLECTION),
        module(18, -5, Direction.NORTH, ASSEMBLER_MODULE),
        module(-18, 11, Direction.SOUTH, RESOURCE_COLLECTION),
        module(-18, -5, Direction.NORTH, ASSEMBLER_MODULE),
        module(8, 21, Direction.EAST, RESOURCE_COLLECTION),
        module(8, -15, Direction.EAST, ASSEMBLER_MODULE),
        module(-8, 21, Direction.WEST, RESOURCE_COLLECTION),
        module(-8, -15, Direction.WEST, ASSEMBLER_MODULE),
        module(1, 24, Direction.WEST, RESOURCE_COLLECTION),
        module(1, -18, Direction.WEST, ASSEMBLER_MODULE),
        module(-1, 24, Direction.EAST, RESOURCE_COLLECTION),
        module(-1, -18, Direction.EAST, ASSEMBLER_MODULE),
        module(21, 4, Direction.WEST, RESOURCE_COLLECTION),
        module(21, 2, Direction.WEST, ASSEMBLER_MODULE),
        module(-21, 4, Direction.EAST, RESOURCE_COLLECTION),
        module(-21, 2, Direction.EAST, ASSEMBLER_MODULE),
        module(7, 25, Direction.EAST, RESOURCE_COLLECTION),
        module(7, -19, Direction.EAST, ASSEMBLER_MODULE),
        module(-7, 25, Direction.WEST, RESOURCE_COLLECTION),
        module(-7, -19, Direction.WEST, ASSEMBLER_MODULE),
        module(22, 10, Direction.SOUTH, RESOURCE_COLLECTION),
        module(22, -4, Direction.NORTH, ASSEMBLER_MODULE),
        module(-22, 10, Direction.SOUTH, RESOURCE_COLLECTION),
        module(-22, -4, Direction.NORTH, ASSEMBLER_MODULE),
        module(26, 9, Direction.SOUTH, RESOURCE_COLLECTION),
        module(26, -3, Direction.NORTH, ASSEMBLER_MODULE),
        module(-26, 9, Direction.SOUTH, RESOURCE_COLLECTION),
        module(-26, -3, Direction.NORTH, ASSEMBLER_MODULE),
        module(6, 29, Direction.EAST, RESOURCE_COLLECTION),
        module(6, -23, Direction.EAST, ASSEMBLER_MODULE),
        module(-6, 29, Direction.WEST, RESOURCE_COLLECTION),
        module(-6, -23, Direction.WEST, ASSEMBLER_MODULE),
        module(16, 22, Direction.EAST, RESOURCE_COLLECTION),
        module(16, -16, Direction.EAST, ASSEMBLER_MODULE),
        module(-16, 22, Direction.WEST, RESOURCE_COLLECTION),
        module(-16, -16, Direction.WEST, ASSEMBLER_MODULE),
        module(19, 19, Direction.SOUTH, RESOURCE_COLLECTION),
        module(19, -13, Direction.NORTH, ASSEMBLER_MODULE),
        module(-19, 19, Direction.SOUTH, RESOURCE_COLLECTION),
        module(-19, -13, Direction.NORTH, ASSEMBLER_MODULE),
        module(16, 26, Direction.EAST, RESOURCE_COLLECTION),
        module(16, -20, Direction.EAST, ASSEMBLER_MODULE),
        module(-16, 26, Direction.WEST, RESOURCE_COLLECTION),
        module(-16, -20, Direction.WEST, ASSEMBLER_MODULE),
        module(23, 19, Direction.SOUTH, RESOURCE_COLLECTION),
        module(23, -13, Direction.NORTH, ASSEMBLER_MODULE),
        module(-23, 19, Direction.SOUTH, RESOURCE_COLLECTION),
        module(-23, -13, Direction.NORTH, ASSEMBLER_MODULE)
    )

    private fun getPowerCore(controllerPos: BlockPos, level: Level): BlockPos? =
        POWER_CORE_OFFSETS.asSequence()
            .map { (x, y, z) -> controllerPos.offset(x, y, z) }
            .firstOrNull { level.getBlockState(it).block === GTLBlocks.POWER_CORE.get() }

    companion object {
        private val POWER_CORE_OFFSETS = arrayOf(
            intArrayOf(3, -2, 0),
            intArrayOf(-3, -2, 0),
            intArrayOf(0, -2, 3),
            intArrayOf(0, -2, -3)
        )

        val MODULE_OFFSETS = arrayOf(
            intArrayOf(4, 2, 11), intArrayOf(4, 2, -11), intArrayOf(-4, 2, 11), intArrayOf(-4, 2, -11),
            intArrayOf(11, 2, 4), intArrayOf(-11, 2, 4), intArrayOf(11, 2, -4), intArrayOf(-11, 2, -4),
            intArrayOf(18, 2, 8), intArrayOf(18, 2, -8), intArrayOf(-18, 2, 8), intArrayOf(-18, 2, -8),
            intArrayOf(8, 2, 18), intArrayOf(8, 2, -18), intArrayOf(-8, 2, 18), intArrayOf(-8, 2, -18),
            intArrayOf(1, 2, 21), intArrayOf(1, 2, -21), intArrayOf(-1, 2, 21), intArrayOf(-1, 2, -21),
            intArrayOf(21, 2, 1), intArrayOf(21, 2, -1), intArrayOf(-21, 2, 1), intArrayOf(-21, 2, -1),
            intArrayOf(7, 2, 22), intArrayOf(7, 2, -22), intArrayOf(-7, 2, 22), intArrayOf(-7, 2, -22),
            intArrayOf(22, 2, 7), intArrayOf(22, 2, -7), intArrayOf(-22, 2, 7), intArrayOf(-22, 2, -7),
            intArrayOf(26, 2, 6), intArrayOf(26, 2, -6), intArrayOf(-26, 2, 6), intArrayOf(-26, 2, -6),
            intArrayOf(6, 2, 26), intArrayOf(6, 2, -26), intArrayOf(-6, 2, 26), intArrayOf(-6, 2, -26),
            intArrayOf(16, 2, 19), intArrayOf(16, 2, -19), intArrayOf(-16, 2, 19), intArrayOf(-16, 2, -19),
            intArrayOf(19, 2, 16), intArrayOf(19, 2, -16), intArrayOf(-19, 2, 16), intArrayOf(-19, 2, -16),
            intArrayOf(16, 2, 23), intArrayOf(16, 2, -23), intArrayOf(-16, 2, 23), intArrayOf(-16, 2, -23),
            intArrayOf(23, 2, 16), intArrayOf(23, 2, -16), intArrayOf(-23, 2, 16), intArrayOf(-23, 2, -16)
        )

        private fun module(
            x: Int,
            z: Int,
            moduleFront: Direction,
            definition: com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition
        ) = ModuleRenderInfo(
            BlockPos(x, 0, z),
            Direction.NORTH,
            Direction.UP,
            moduleFront,
            Direction.UP,
            definition
        )
    }
}