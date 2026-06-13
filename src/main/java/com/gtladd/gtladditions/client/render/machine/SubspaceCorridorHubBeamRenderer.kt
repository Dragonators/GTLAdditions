package com.gtladd.gtladditions.client.render.machine

import com.gtladd.gtladditions.GTLAdditions
import com.gtladd.gtladditions.client.RenderMode
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristBeamRenderer
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristRenderProfile
import com.gtladd.gtladditions.client.render.machine.antichrist.AntichristStarRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredMachineRenderer
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderContext
import com.gtladd.gtladditions.client.render.machine.deferred.DeferredRenderEntry
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object SubspaceCorridorHubBeamRenderer {
    private val STAR_TEXTURE = GTLAdditions.id("textures/block/multiblock/forge_of_antichrist/star_layer_1.png")
    private const val STAR_RADIUS = 17.02f
    private const val HEIGHT = 460.0f
    private const val BOTTOM_RADIUS = 12.615f
    private const val TOP_RADIUS = 1.1f
    private const val BEAM_ALPHA = 1.0f
    private const val BASE_STELLAR_COLOR = 0xF37F06
    private val BASE_STELLAR_COLOR_R = ((BASE_STELLAR_COLOR shr 16) and 0xFF) / 255.0f
    private val BASE_STELLAR_COLOR_G = ((BASE_STELLAR_COLOR shr 8) and 0xFF) / 255.0f
    private val BASE_STELLAR_COLOR_B = (BASE_STELLAR_COLOR and 0xFF) / 255.0f

    fun enqueue(blockEntity: BlockEntity, tick: Float, starPos: Vec3, beamBasePos: Vec3) {
        DeferredMachineRenderer.enqueue(
            Entry(
                blockEntity,
                AntichristRenderProfile(
                    tick = tick,
                    isWorking = true,
                    facing = Direction.NORTH,
                    renderMode = RenderMode.NORMAL,
                    starPos = starPos,
                    colorR = BASE_STELLAR_COLOR_R,
                    colorG = BASE_STELLAR_COLOR_G,
                    colorB = BASE_STELLAR_COLOR_B,
                    starRadius = STAR_RADIUS,
                    beamAlpha = BEAM_ALPHA
                ),
                AntichristBeamRenderer.VerticalTaperBeamProfile(
                    tick = tick,
                    basePos = beamBasePos,
                    height = HEIGHT,
                    bottomRadius = BOTTOM_RADIUS,
                    topRadius = TOP_RADIUS,
                    colorR = BASE_STELLAR_COLOR_R,
                    colorG = BASE_STELLAR_COLOR_G,
                    colorB = BASE_STELLAR_COLOR_B,
                    beamAlpha = BEAM_ALPHA
                )
            )
        )
    }

    private data class Entry(
        override val blockEntity: BlockEntity,
        val starProfile: AntichristRenderProfile,
        val beamProfile: AntichristBeamRenderer.VerticalTaperBeamProfile
    ) : DeferredRenderEntry {
        override fun render(context: DeferredRenderContext) {
            context.renderRelativeToCamera(blockEntity) {
                AntichristStarRenderer.renderOpaqueOriginalColor(starProfile, this, STAR_TEXTURE)
                AntichristBeamRenderer.renderVerticalTaper(beamProfile, this, blockEntity)
            }
        }
    }
}