package com.gtladd.gtladditions.client.render.machine.condensation

import com.gtladd.gtladditions.GTLAdditions
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.model.data.ModelData

@OnlyIn(Dist.CLIENT)
object CondensationCoreMesh {
    val CORE_MODEL = GTLAdditions.id("effect/condensation/antimatter")
    val SHELL_MODEL = GTLAdditions.id("effect/condensation/smooth_sphere")

    /** Preserve coordinates for the color hash. */
    fun writeModel(builder: BufferBuilder, model: BakedModel) {
        val random = RandomSource.create(42)
        val stride = DefaultVertexFormat.BLOCK.integerSize

        for (side in arrayOf(null, *Direction.entries.toTypedArray())) {
            random.setSeed(42)

            for (quad in model.getQuads(null, side, random, ModelData.EMPTY, null)) {
                val vertices = quad.vertices
                fun vertex(index: Int) {
                    val offset = index * stride
                    builder.vertex(
                        Float.fromBits(vertices[offset]).toDouble(),
                        Float.fromBits(vertices[offset + 1]).toDouble(),
                        Float.fromBits(vertices[offset + 2]).toDouble()
                    ).endVertex()
                }

                vertex(0)
                vertex(1)
                vertex(2)
                // Forge pads OBJ triangles by repeating their last vertex in the quad.
                if ((0..2).any { vertices[2 * stride + it] != vertices[3 * stride + it] }) {
                    vertex(0)
                    vertex(2)
                    vertex(3)
                }
            }
        }
    }
}