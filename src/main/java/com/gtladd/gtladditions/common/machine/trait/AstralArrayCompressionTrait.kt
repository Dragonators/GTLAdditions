package com.gtladd.gtladditions.common.machine.trait

import com.gregtechceu.gtceu.api.machine.trait.MachineTrait
import com.gtladd.gtladditions.api.machine.IAstralArrayInteractionMachine
import com.gtladd.gtladditions.common.items.GTLAddItems
import com.gtladd.gtladditions.common.machine.multiblock.controller.ArcanicAstrograph
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.gtladd.gtladditions.utils.MachineUtil
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.gtlcore.gtlcore.api.recipe.ingredient.LongIngredient
import kotlin.math.floor
import kotlin.math.pow

class AstralArrayCompressionTrait(
    machine: ArcanicAstrograph
) : MachineTrait(machine) {

    @field:Persisted
    private var compressionAstralArrayCount = 0L

    override fun getMachine(): ArcanicAstrograph = super.getMachine() as ArcanicAstrograph

    fun handleCompressionWorking() {
        collectAstralArraysInCompressionArea()
    }

    fun finishCompression() {
        val batches = compressionAstralArrayCount / IAstralArrayInteractionMachine.COMPRESSED_ASTRAL_ARRAY_EQUIVALENT
        if (batches <= 0) return

        val outputs = calculateCompressionOutputs(batches)
        outputCompressedAstralArrays(outputs)

        compressionAstralArrayCount -= batches * IAstralArrayInteractionMachine.COMPRESSED_ASTRAL_ARRAY_EQUIVALENT
    }

    fun resetCompression() {
        compressionAstralArrayCount = 0L
    }

    private fun collectAstralArraysInCompressionArea() {
        val serverLevel = machine.level as? ServerLevel ?: return
        val center = getCompressionCenter()
        val radius = COMPRESSION_AREA_RADIUS
        val aabb = AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius))
        val entities = serverLevel.getEntitiesOfClass(ItemEntity::class.java, aabb) {
            it.item.`is`(GTLAddItems.ASTRAL_ARRAY.asItem())
        }

        for (entity in entities) {
            compressionAstralArrayCount += entity.item.count.toLong()
            entity.discard()
        }
    }

    private fun calculateCompressionOutputs(batches: Long): Long {
        val expectedOutputs = batches * compressedAstralArrayOutputChance
        val guaranteedOutputs = floor(expectedOutputs).toLong()
        val extraChance = expectedOutputs - guaranteedOutputs
        val random = machine.level?.random ?: return 0L
        return guaranteedOutputs + if (random.nextDouble() < extraChance) 1L else 0L
    }

    private fun outputCompressedAstralArrays(amount: Long) {
        if (amount <= 0) return
        val compressedAstralArray = GTLAddItems.COMPRESSED_ASTRAL_ARRAY.asItem()
        MachineUtil.outputItem(getMachine(), LongIngredient.create(Ingredient.of(compressedAstralArray), amount))
    }

    private fun getCompressionCenter(): Vec3 {
        val machine = getMachine()
        val offset = getRotatedRenderPosition(Direction.NORTH, machine.frontFacing, 0.0, 0.0, COMPRESSION_FRONT_OFFSET)
        return Vec3.atLowerCornerOf(machine.pos).add(offset)
    }

    val compressedAstralArrayOutputChance: Double
        get() {
            val astralArrayCount = getMachine().astralArrayCount
            if (astralArrayCount >= MAX_CHANCE_ASTRAL_ARRAY_COUNT) return 1.0

            val progress = (
                astralArrayCount.toDouble() /
                    MAX_CHANCE_ASTRAL_ARRAY_COUNT.toDouble()
                ).coerceIn(0.0, 1.0)
            return BASE_COMPRESSION_OUTPUT_CHANCE +
                (1.0 - BASE_COMPRESSION_OUTPUT_CHANCE) * progress.pow(COMPRESSION_OUTPUT_CHANCE_EXPONENT)
        }

    override fun getFieldHolder(): ManagedFieldHolder = MANAGED_FIELD_HOLDER

    companion object {
        // First embedded array count that makes the existing parallel formula reach Int.MAX_VALUE.
        private const val MAX_CHANCE_ASTRAL_ARRAY_COUNT = 42441
        private const val COMPRESSION_FRONT_OFFSET = 16.0
        private const val COMPRESSION_AREA_RADIUS = 11.5
        private const val BASE_COMPRESSION_OUTPUT_CHANCE = 0.3
        private const val COMPRESSION_OUTPUT_CHANCE_EXPONENT = 1.5

        val MANAGED_FIELD_HOLDER: ManagedFieldHolder = ManagedFieldHolder(AstralArrayCompressionTrait::class.java)
    }
}