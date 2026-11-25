package com.gtladd.gtladditions.client.gui

import com.glodblock.github.glodium.client.render.ColorData
import com.glodblock.github.glodium.client.render.highlight.HighlightHandler
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.AABB
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
internal object StructureTierRenderer {

    private const val HIGHLIGHT_MILLIS = 15_000L

    private var marked: Set<BlockPos> = emptySet()

    fun clear() {
        marked = emptySet()
    }

    fun highlight(buf: FriendlyByteBuf) {
        val dimension = Minecraft.getInstance().level?.dimension() ?: return
        val until = System.currentTimeMillis() + HIGHLIGHT_MILLIS

        val cells = List(buf.readVarInt()) { BlockPos.of(buf.readLong()) }
        val chunks = List(buf.readVarInt()) {
            AABB(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
            )
        }

        val corners = chunks.map { BlockPos.containing(it.minX, it.minY, it.minZ) }
        // A fixed block must not stay marked: only the latest press's marks are drawn.
        marked = (cells + corners).toSet()

        val problem = ColorData(1f, 0f, 0f)
        val unloaded = ColorData(1f, 0.8f, 0f)

        for (pos in cells) {
            HighlightHandler.highlight(pos, null, dimension, until, AABB(pos), problem) {
                pos in marked && System.currentTimeMillis() / 500 % 2 == 0L
            }
        }

        for ((pos, box) in corners.zip(chunks)) {
            HighlightHandler.highlight(pos, null, dimension, until, box, unloaded) { pos in marked }
        }
    }
}
