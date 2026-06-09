package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.client.RenderMode
import com.gtladd.gtladditions.common.machine.multiblock.controller.ForgeOfTheAntichrist
import com.gtladd.gtladditions.utils.CommonUtils.getRotatedRenderPosition
import com.gtladd.gtladditions.utils.antichrist.ClientAnimationHelper.getClientRenderColor
import com.gtladd.gtladditions.utils.antichrist.ClientAnimationHelper.getClientRenderRadius
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.awt.Color

@OnlyIn(Dist.CLIENT)
data class AntichristRenderProfile(
    val tick: Float,
    val isWorking: Boolean,
    val facing: Direction,
    val renderMode: RenderMode,
    val starPos: Vec3,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    val starRadius: Float
) {
    val shouldRenderBeam: Boolean
        get() = renderMode == RenderMode.NORMAL || renderMode == RenderMode.RAINBOW

    val beamYawDegrees: Float
        get() = when (facing) {
            Direction.NORTH -> 0f
            Direction.EAST -> -90f
            Direction.SOUTH -> 180f
            Direction.WEST -> 90f
            else -> 0f
        }

    companion object {
        const val STAR_OFFSET_X = -122.0
        const val BASE_STAR_RADIUS = 13.0f
        const val STAR_GAMMA = 3.0f
        val BASE_DIRECTION: Direction = Direction.EAST

        fun create(
            machine: ForgeOfTheAntichrist,
            tick: Float,
            isWorking: Boolean
        ): AntichristRenderProfile {
            val renderMode = machine.starRitual.renderMode
            val (argb32, radiusMultiplier) = getRenderColorAndRadius(renderMode, machine, tick)
            val starPos = getRotatedRenderPosition(BASE_DIRECTION, machine.frontFacing, STAR_OFFSET_X, 0.0, 0.0)

            return AntichristRenderProfile(
                tick = tick,
                isWorking = isWorking,
                facing = machine.frontFacing,
                renderMode = renderMode,
                starPos = starPos,
                colorR = ((argb32 shr 16) and 0xFF) / 255.0f,
                colorG = ((argb32 shr 8) and 0xFF) / 255.0f,
                colorB = (argb32 and 0xFF) / 255.0f,
                starRadius = BASE_STAR_RADIUS * radiusMultiplier
            )
        }

        private fun getRenderColorAndRadius(
            renderMode: RenderMode,
            machine: ForgeOfTheAntichrist,
            tick: Float
        ): Pair<Int, Float> = when (renderMode) {
            RenderMode.NORMAL -> machine.rgbFromTime to machine.radiusMultiplier
            RenderMode.RAINBOW -> getRainbowColor(tick) to machine.radiusMultiplier
            RenderMode.COLLAPSING -> {
                val rainbowColor = getRainbowColor(tick)
                getClientRenderColor(machine, rainbowColor) to getClientRenderRadius(machine, machine.radiusMultiplier)
            }
            RenderMode.RECOVERING -> {
                getClientRenderColor(machine, machine.rgbFromTime) to
                    getClientRenderRadius(machine, machine.radiusMultiplier)
            }
        }

        private fun getRainbowColor(tick: Float): Int {
            val hue = (tick % 60) / 60.0f
            val color = Color.getHSBColor(hue, 1.0f, 1.0f)
            return (0xFF shl 24) or (color.red shl 16) or (color.green shl 8) or color.blue
        }
    }
}