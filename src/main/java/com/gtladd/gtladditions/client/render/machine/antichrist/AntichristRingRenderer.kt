package com.gtladd.gtladditions.client.render.machine.antichrist

import com.gtladd.gtladditions.client.render.withPose
import com.gtladd.gtladditions.utils.antichrist.RingStructureVertexBuffer
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.core.Direction
import net.minecraft.world.inventory.InventoryMenu
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Quaternionf

@OnlyIn(Dist.CLIENT)
object AntichristRingRenderer {
    fun render(profile: AntichristRenderProfile, poseStack: PoseStack) {
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()
        RenderSystem.depthMask(true)
        RenderSystem.blendFunc(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        )
        RenderSystem.setShader { GameRenderer.getRendertypeSolidShader() }
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS)
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer()

        RingStructureVertexBuffer.ringBuffers.forEachIndexed { index, buffer ->
            poseStack.withPose {
                translate(profile.starPos.x, profile.starPos.y, profile.starPos.z)

                when (profile.facing) {
                    Direction.NORTH -> mulPose(Axis.YP.rotationDegrees(270f))
                    Direction.SOUTH -> mulPose(Axis.YP.rotationDegrees(90f))
                    Direction.WEST -> mulPose(Axis.YP.rotationDegrees(0f))
                    Direction.EAST -> mulPose(Axis.YP.rotationDegrees(180f))
                    else -> {}
                }

                if (profile.isWorking) {
                    val direction = if (index == 1) -1.0f else 1.0f
                    val speedMultiplier = 0.4f + (index * 0.4f)
                    val angleOffset = index * 120f
                    val rotationAngle = (profile.tick * speedMultiplier * 2.0f * direction + angleOffset) % 360.0f

                    mulPose(Quaternionf().fromAxisAngleDeg(1.0f, 0.0f, 0.0f, rotationAngle))
                }

                buffer.bind()
                buffer.drawWithShader(
                    last().pose(),
                    RenderSystem.getProjectionMatrix(),
                    RenderSystem.getShader()!!
                )
                VertexBuffer.unbind()
            }
        }

        RenderSystem.disableDepthTest()
        RenderSystem.disableBlend()
    }
}