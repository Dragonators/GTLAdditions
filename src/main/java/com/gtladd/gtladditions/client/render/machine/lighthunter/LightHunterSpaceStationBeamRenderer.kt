package com.gtladd.gtladditions.client.render.machine.lighthunter

import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristBeamRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object LightHunterSpaceStationBeamRenderer {
    private const val BEAM_RADIUS = 0.85f
    private const val OUTER_ALPHA = 0.54901963f
    private const val INNER_RADIUS_SCALE = 0.45f
    private const val INNER_ALPHA = OUTER_ALPHA
    private const val OUTER_R = 0.50980395f
    private const val OUTER_G = 0.65882355f
    private const val OUTER_B = 0.7529412f

    fun enqueue(blockEntity: BlockEntity, tick: Float, from: Vec3, to: Vec3) {
        DeferredMachineRenderer.enqueue(Entry(blockEntity, tick, from, to))
    }

    private data class Entry(
        override val blockEntity: BlockEntity,
        val tick: Float,
        val from: Vec3,
        val to: Vec3
    ) : DeferredRenderEntry {
        override fun render(context: DeferredRenderContext) {
            context.renderRelativeToCamera(blockEntity) {
                AntichristBeamRenderer.renderLinear(
                    blockEntity = blockEntity,
                    poseStack = this,
                    tick = tick,
                    from = from,
                    to = to,
                    startRadius = BEAM_RADIUS,
                    endRadius = BEAM_RADIUS,
                    colorR = OUTER_R,
                    colorG = OUTER_G,
                    colorB = OUTER_B,
                    outerAlpha = OUTER_ALPHA,
                    innerRadiusScale = INNER_RADIUS_SCALE,
                    innerAlpha = INNER_ALPHA
                )
            }
        }
    }
}